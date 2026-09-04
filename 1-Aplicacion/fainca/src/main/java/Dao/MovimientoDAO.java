package Dao;

import Objetos.Movimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAO {

    /**
     * Registra un ingreso/egreso y actualiza el stock en la misma transaccion.
     * El stock nunca se edita directo: siempre pasa por aqui para dejar rastro.
     *
     * @param fecha opcional ("yyyy-MM-dd" o "yyyy-MM-dd HH:mm"); null = ahora.
     * @return el stock resultante.
     * @throws IllegalArgumentException si el producto no existe o el stock es insuficiente.
     */
    public int registrar(String productoCodigo, String tipo, int cantidad,
                         String observaciones, String fecha, int usuarioId) {
        return registrar(productoCodigo, tipo, cantidad, observaciones, fecha, usuarioId, null);
    }

    /**
     * Igual que el anterior, pero indicando el lote al que pertenece la linea.
     * Todas las lineas de un mismo envio comparten lote y se muestran en el historial
     * como un solo documento con su lista de productos.
     */
    public int registrar(String productoCodigo, String tipo, int cantidad,
                         String observaciones, String fecha, int usuarioId, String lote) {
        // Se busca ignorando guiones/espacios y se recupera el codigo REAL, para que el
        // movimiento quede guardado contra el codigo tal cual esta registrado el producto.
        String sqlSelect = "SELECT codigo, stock_actual FROM productos WHERE "
                + Codigo.sqlNormalizado("codigo") + " = ? FOR UPDATE";
        String sqlUpdate = "UPDATE productos SET stock_actual = ? WHERE codigo = ?";
        String sqlInsert = """
                INSERT INTO movimientos (producto_codigo, tipo, lote, cantidad, stock_resultante,
                                         usuario_id, observaciones, fecha)
                VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))
                """;

        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                int stockActual;
                try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                    ps.setString(1, Codigo.normalizar(productoCodigo));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Producto no encontrado: " + productoCodigo);
                        }
                        productoCodigo = rs.getString("codigo");
                        stockActual = rs.getInt("stock_actual");
                    }
                }

                int stockResultante = "egreso".equals(tipo) ? stockActual - cantidad : stockActual + cantidad;
                if (stockResultante < 0) {
                    throw new IllegalArgumentException("Stock insuficiente (actual: " + stockActual + ")");
                }

                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, stockResultante);
                    ps.setString(2, productoCodigo);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                    ps.setString(1, productoCodigo);
                    ps.setString(2, tipo);
                    ps.setString(3, lote);
                    ps.setInt(4, cantidad);
                    ps.setInt(5, stockResultante);
                    ps.setInt(6, usuarioId);
                    ps.setString(7, observaciones);
                    if (fecha == null || fecha.isBlank()) {
                        ps.setNull(8, Types.VARCHAR);
                    } else {
                        ps.setString(8, fecha);
                    }
                    ps.executeUpdate();
                }

                con.commit();
                return stockResultante;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando movimiento", e);
        }
    }

    /**
     * Ajuste por reconteo fisico: fija el stock en la cantidad real contada y
     * registra la diferencia (positiva o negativa) como movimiento tipo 'ajuste'.
     * De paso permite corregir la ubicacion (percha); si "ubicacion" es null significa
     * "no tocarla" (para poder cambiar solo la cantidad), si viene informada (incluso
     * vacia) se compara con la actual y se actualiza si es distinta -- asi se puede
     * usar esta misma pantalla para corregir solo la ubicacion sin alterar el stock.
     *
     * @return arreglo {stockAnterior, diferencia, stockNuevo}.
     * @throws IllegalArgumentException si el producto no existe o no hay nada que ajustar.
     */
    public int[] ajustar(String productoCodigo, int cantidadReal, String ubicacion,
                         String observaciones, String fecha, int usuarioId) {
        return ajustar(productoCodigo, cantidadReal, ubicacion, observaciones, fecha, usuarioId, null);
    }

    /** Igual que el anterior, indicando el lote (para reconteos de varios productos a la vez). */
    public int[] ajustar(String productoCodigo, int cantidadReal, String ubicacion,
                         String observaciones, String fecha, int usuarioId, String lote) {
        // Igual que en registrar(): se acepta el codigo con guion o con espacio y se
        // recupera el real para guardar el movimiento contra el producto correcto.
        String sqlSelect = "SELECT codigo, stock_actual, ubicacion FROM productos WHERE "
                + Codigo.sqlNormalizado("codigo") + " = ? FOR UPDATE";
        String sqlInsert = """
                INSERT INTO movimientos (producto_codigo, tipo, lote, cantidad, stock_resultante,
                                         usuario_id, observaciones, fecha)
                VALUES (?, 'ajuste', ?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))
                """;

        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                int stockActual;
                String ubicacionActual;
                try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                    ps.setString(1, Codigo.normalizar(productoCodigo));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Producto no encontrado: " + productoCodigo);
                        }
                        productoCodigo = rs.getString("codigo");
                        stockActual = rs.getInt("stock_actual");
                        ubicacionActual = rs.getString("ubicacion");
                    }
                }

                String actualNormalizada = ubicacionActual == null ? "" : ubicacionActual;
                boolean cambiaUbicacion = ubicacion != null && !ubicacion.equals(actualNormalizada);

                int diferencia = cantidadReal - stockActual;
                if (diferencia == 0 && !cambiaUbicacion) {
                    throw new IllegalArgumentException(
                            "El stock del sistema ya es " + stockActual
                                    + (ubicacion != null ? " y la ubicación ya es \"" + actualNormalizada + "\"" : "")
                                    + "; no hay nada que ajustar");
                }

                StringBuilder sqlUpdate = new StringBuilder("UPDATE productos SET stock_actual = ?");
                if (cambiaUbicacion) sqlUpdate.append(", ubicacion = ?");
                sqlUpdate.append(" WHERE codigo = ?");
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate.toString())) {
                    int i = 1;
                    ps.setInt(i++, cantidadReal);
                    if (cambiaUbicacion) ps.setString(i++, ubicacion);
                    ps.setString(i, productoCodigo);
                    ps.executeUpdate();
                }

                String observacionesFinal = observaciones;
                if (cambiaUbicacion) {
                    String nota = "Ubicación actualizada: \"" + actualNormalizada + "\" → \"" + ubicacion + "\".";
                    observacionesFinal = observaciones != null && !observaciones.isBlank()
                            ? nota + " " + observaciones : nota;
                }

                try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                    ps.setString(1, productoCodigo);
                    ps.setString(2, lote);
                    ps.setInt(3, diferencia); // firmada: negativa si faltaba producto; 0 si solo cambio la ubicacion
                    ps.setInt(4, cantidadReal);
                    ps.setInt(5, usuarioId);
                    ps.setString(6, observacionesFinal);
                    if (fecha == null || fecha.isBlank()) {
                        ps.setNull(7, Types.VARCHAR);
                    } else {
                        ps.setString(7, fecha);
                    }
                    ps.executeUpdate();
                }

                con.commit();
                return new int[]{stockActual, diferencia, cantidadReal};
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando ajuste", e);
        }
    }

    /**
     * Deja constancia en el historial de un cambio en los datos del producto
     * (marca, ubicacion, descripcion...), que no mueve stock: cantidad = 0 y el
     * stock resultante es el mismo que ya tenia.
     *
     * Recibe la conexion desde afuera a proposito: asi el registro viaja en la
     * misma transaccion que el UPDATE del producto, y es imposible que quede un
     * cambio guardado sin su rastro (o al reves).
     */
    void registrarEdicion(Connection con, String productoCodigo, int stockActual,
                          String detalle, int usuarioId) throws SQLException {
        anotar(con, productoCodigo, "edicion", stockActual, detalle, usuarioId);
    }

    /**
     * Inserta una anotacion que NO mueve stock ('edicion' o 'correccion'): cantidad 0 y
     * el stock actual como resultante, para que el historial siga cuadrando.
     */
    private void anotar(Connection con, String productoCodigo, String tipo, int stockActual,
                        String detalle, int usuarioId) throws SQLException {
        String sql = """
                INSERT INTO movimientos (producto_codigo, tipo, cantidad, stock_resultante,
                                         usuario_id, observaciones, fecha)
                VALUES (?, ?, 0, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, productoCodigo);
            ps.setString(2, tipo);
            ps.setInt(3, stockActual);
            ps.setInt(4, usuarioId);
            ps.setString(5, recortar(detalle));
            ps.executeUpdate();
        }
    }

    /** Igual que el anterior, pero abriendo su propia conexion (para quien no este en una transaccion). */
    public void registrarEdicion(String productoCodigo, int stockActual, String detalle, int usuarioId) {
        try (Connection con = Db.getConnection()) {
            registrarEdicion(con, productoCodigo, stockActual, detalle, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando la edicion en el historial", e);
        }
    }

    /**
     * Corrige/aclara un movimiento ya guardado SIN alterarlo ni borrarlo: agrega una nueva
     * entrada tipo 'correccion' que documenta el cambio (que decia antes, que dice ahora).
     * Asi el historial nunca pierde el rastro de lo que realmente paso.
     *
     * Lleva tipo propio (no 'edicion') a proposito: en el historial sale marcada con un
     * triangulo de advertencia y se puede filtrar aparte, para que un auditor detecte
     * enseguida quien toco que registro y nadie pueda cambiar algo sin que se note.
     */
    public void corregirObservacion(int movimientoId, String correccion, int usuarioId) {
        String sqlBuscar = "SELECT producto_codigo, tipo, observaciones, "
                + "DATE_FORMAT(fecha, '%d/%m/%Y %H:%i') AS fecha_corta FROM movimientos WHERE id = ?";
        try (Connection con = Db.getConnection()) {
            String productoCodigo, tipo, obsOriginal, fechaCorta;
            try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
                ps.setInt(1, movimientoId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("El movimiento #" + movimientoId + " no existe");
                    }
                    productoCodigo = rs.getString("producto_codigo");
                    tipo = rs.getString("tipo");
                    obsOriginal = rs.getString("observaciones");
                    fechaCorta = rs.getString("fecha_corta");
                }
            }

            int stockActual = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT stock_actual FROM productos WHERE codigo = ?")) {
                ps.setString(1, productoCodigo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) stockActual = rs.getInt("stock_actual");
                }
            }

            String detalle = "Corrección del movimiento #" + movimientoId + " (" + tipo.toUpperCase()
                    + " del " + fechaCorta + ")"
                    + (obsOriginal != null && !obsOriginal.isBlank() ? " — antes decía: \"" + obsOriginal + "\"" : "")
                    + " → " + correccion;
            anotar(con, productoCodigo, "correccion", stockActual, detalle, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando la corrección", e);
        }
    }

    /** La columna admite 1000 caracteres; recortar evita que un texto largo aborte el guardado. */
    private static String recortar(String texto) {
        if (texto == null) return null;
        return texto.length() <= 1000 ? texto : texto.substring(0, 997) + "...";
    }

    /** Cuantos movimientos devuelve cada pagina del historial. */
    public static final int PAGINA = 1000;

    /**
     * Todas las lineas de un documento (lote), en el orden en que se registraron.
     * Es la fuente autoritativa para desplegar el detalle y para exportar el PDF:
     * no depende de lo que haya alcanzado a cargar la pantalla.
     */
    public List<Movimiento> porLote(String lote) {
        String sql = """
                SELECT mv.id, mv.producto_codigo, p.descripcion, m.nombre AS marca,
                       mv.tipo, mv.lote, mv.cantidad, mv.stock_resultante, mv.observaciones,
                       DATE_FORMAT(mv.fecha, '%Y-%m-%d %H:%i:%s') AS fecha,
                       u.nombre AS usuario, p.imagen, p.unidad_medida, p.ubicacion
                FROM movimientos mv
                JOIN productos p ON p.codigo = mv.producto_codigo
                JOIN marcas m ON m.id = p.marca_id
                JOIN usuarios u ON u.id = mv.usuario_id
                WHERE mv.lote = ?
                ORDER BY mv.id
                """;
        List<Movimiento> lineas = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, lote);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lineas.add(mapearConProducto(rs));
            }
            return lineas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el documento", e);
        }
    }

    /** Una sola linea suelta (movimientos anteriores al agrupado, que no tienen lote). */
    public List<Movimiento> porId(int id) {
        String sql = """
                SELECT mv.id, mv.producto_codigo, p.descripcion, m.nombre AS marca,
                       mv.tipo, mv.lote, mv.cantidad, mv.stock_resultante, mv.observaciones,
                       DATE_FORMAT(mv.fecha, '%Y-%m-%d %H:%i:%s') AS fecha,
                       u.nombre AS usuario, p.imagen, p.unidad_medida, p.ubicacion
                FROM movimientos mv
                JOIN productos p ON p.codigo = mv.producto_codigo
                JOIN marcas m ON m.id = p.marca_id
                JOIN usuarios u ON u.id = mv.usuario_id
                WHERE mv.id = ?
                """;
        List<Movimiento> lineas = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lineas.add(mapearConProducto(rs));
            }
            return lineas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el movimiento", e);
        }
    }

    private Movimiento mapearConProducto(ResultSet rs) throws SQLException {
        Movimiento mv = new Movimiento();
        mv.setId(rs.getInt("id"));
        mv.setProductoCodigo(rs.getString("producto_codigo"));
        mv.setDescripcion(rs.getString("descripcion"));
        mv.setMarca(rs.getString("marca"));
        mv.setTipo(rs.getString("tipo"));
        mv.setLote(rs.getString("lote"));
        mv.setCantidad(rs.getInt("cantidad"));
        mv.setStockResultante(rs.getInt("stock_resultante"));
        mv.setObservaciones(rs.getString("observaciones"));
        mv.setFecha(rs.getString("fecha"));
        mv.setUsuario(rs.getString("usuario"));
        mv.setImagen(rs.getString("imagen"));
        mv.setUnidadMedida(rs.getString("unidad_medida"));
        mv.setUbicacion(rs.getString("ubicacion"));
        return mv;
    }

    /** Historial con filtros opcionales (pasar null para omitir un filtro). Primera pagina. */
    public List<Movimiento> historial(String codigo, String tipo, String desde, String hasta) {
        return historial(codigo, tipo, desde, hasta, 0);
    }

    /**
     * Historial paginado: devuelve {@value #PAGINA} movimientos a partir de "salto".
     * La pantalla pide la siguiente tanda con el boton "Cargar 1000 más", en vez de
     * traer de golpe todo el historial (que ya pasa de mil registros).
     */
    public List<Movimiento> historial(String codigo, String tipo, String desde, String hasta, int salto) {
        StringBuilder sql = new StringBuilder("""
                SELECT mv.id, mv.producto_codigo, p.descripcion, m.nombre AS marca,
                       mv.tipo, mv.lote, mv.cantidad, mv.stock_resultante, mv.observaciones,
                       DATE_FORMAT(mv.fecha, '%Y-%m-%d %H:%i:%s') AS fecha,
                       u.nombre AS usuario,
                       -- Cuantas lineas trae el documento al que pertenece esta fila.
                       -- COALESCE con el id hace que cada movimiento SIN lote (los
                       -- anteriores a esta funcion) cuente como su propio grupo de 1,
                       -- en vez de que todos los nulos caigan en la misma particion.
                       COUNT(*) OVER (PARTITION BY COALESCE(mv.lote, CONCAT('id:', mv.id))) AS items_lote
                FROM movimientos mv
                JOIN productos p ON p.codigo = mv.producto_codigo
                JOIN marcas m ON m.id = p.marca_id
                JOIN usuarios u ON u.id = mv.usuario_id
                WHERE 1 = 1
                """);
        List<Object> valores = new ArrayList<>();

        if (codigo != null && !codigo.isBlank()) {
            // Un solo cuadro de busqueda: ademas del codigo del producto, tambien
            // reconoce coincidencias en la observacion del movimiento y en la marca
            // del producto (asi se encuentra, por ejemplo, todo lo de una marca o
            // todo lo relacionado a una guia mencionada en la observacion).
            sql.append(" AND (")
               .append(Codigo.sqlNormalizado("mv.producto_codigo")).append(" LIKE ?")
               .append(" OR mv.observaciones LIKE ?")
               .append(" OR m.nombre LIKE ?")
               .append(")");
            String contiene = "%" + codigo.trim() + "%";
            valores.add(Codigo.normalizar(codigo) + "%");
            valores.add(contiene);
            valores.add(contiene);
        }
        if (tipo != null && List.of("ingreso", "egreso", "ajuste", "edicion", "correccion").contains(tipo)) {
            sql.append(" AND mv.tipo = ?");
            valores.add(tipo);
        }
        if (desde != null && desde.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sql.append(" AND mv.fecha >= ?");
            valores.add(desde + " 00:00:00");
        }
        if (hasta != null && hasta.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sql.append(" AND mv.fecha <= ?");
            valores.add(hasta + " 23:59:59");
        }
        sql.append(" ORDER BY mv.fecha DESC, mv.id DESC LIMIT ? OFFSET ?");
        valores.add(PAGINA);
        valores.add(Math.max(0, salto));

        List<Movimiento> movimientos = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setObject(i + 1, valores.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movimiento mv = new Movimiento();
                    mv.setId(rs.getInt("id"));
                    mv.setProductoCodigo(rs.getString("producto_codigo"));
                    mv.setDescripcion(rs.getString("descripcion"));
                    mv.setMarca(rs.getString("marca"));
                    mv.setTipo(rs.getString("tipo"));
                    mv.setLote(rs.getString("lote"));
                    mv.setCantidad(rs.getInt("cantidad"));
                    mv.setStockResultante(rs.getInt("stock_resultante"));
                    mv.setObservaciones(rs.getString("observaciones"));
                    mv.setFecha(rs.getString("fecha"));
                    mv.setUsuario(rs.getString("usuario"));
                    mv.setItemsLote(rs.getInt("items_lote"));
                    movimientos.add(mv);
                }
            }
            return movimientos;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando historial", e);
        }
    }
}
