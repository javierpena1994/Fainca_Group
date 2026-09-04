package Dao;

import Objetos.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    private static final String CAMPOS = """
            p.codigo, p.descripcion, p.ubicacion, p.unidad_medida,
            p.stock_actual, p.nota_maletas, p.activo, p.marca_id, p.imagen, m.nombre AS marca
            """;

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setCodigo(rs.getString("codigo"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setUbicacion(rs.getString("ubicacion"));
        p.setUnidadMedida(rs.getString("unidad_medida"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setNotaMaletas(rs.getString("nota_maletas"));
        p.setActivo(rs.getBoolean("activo"));
        p.setMarcaId(rs.getInt("marca_id"));
        p.setImagen(rs.getString("imagen"));
        p.setMarca(rs.getString("marca"));
        return p;
    }

    public List<Producto> listar(boolean incluirInactivos) {
        String sql = "SELECT " + CAMPOS + " FROM productos p JOIN marcas m ON m.id = p.marca_id "
                + (incluirInactivos ? "" : "WHERE p.activo = 1 ")
                + "ORDER BY p.codigo";
        List<Producto> productos = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) productos.add(mapear(rs));
            return productos;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando productos", e);
        }
    }

    /**
     * Productos activos para el reporte de inventario, ordenados por marca y codigo.
     * Si marcaId es null, incluye todas las marcas.
     */
    public List<Producto> paraReporte(Integer marcaId) {
        String sql = "SELECT " + CAMPOS + " FROM productos p JOIN marcas m ON m.id = p.marca_id "
                + "WHERE p.activo = 1 " + (marcaId != null ? "AND p.marca_id = ? " : "")
                + "ORDER BY m.nombre, p.codigo";
        List<Producto> productos = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (marcaId != null) ps.setInt(1, marcaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) productos.add(mapear(rs));
            }
            return productos;
        } catch (SQLException e) {
            throw new RuntimeException("Error preparando el reporte", e);
        }
    }

    /**
     * Busqueda en vivo: codigo que empiece por q, o descripcion/marca que lo contenga.
     * El codigo se compara ignorando guiones y espacios, asi "BAN P01" encuentra "BAN-P01".
     * Por defecto omite los productos dados de baja.
     */
    public List<Producto> buscar(String q) {
        return buscar(q, false);
    }

    public List<Producto> buscar(String q, boolean incluirInactivos) {
        String sql = "SELECT " + CAMPOS + " FROM productos p JOIN marcas m ON m.id = p.marca_id "
                + "WHERE " + (incluirInactivos ? "1 = 1" : "p.activo = 1")
                + " AND (" + Codigo.sqlNormalizado("p.codigo") + " LIKE ? "
                + "OR p.descripcion LIKE ? OR m.nombre LIKE ?) "
                + "ORDER BY p.codigo LIMIT 30";
        List<Producto> productos = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Codigo.normalizar(q) + "%");
            ps.setString(2, "%" + q + "%");
            ps.setString(3, "%" + q + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) productos.add(mapear(rs));
            }
            return productos;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando productos", e);
        }
    }

    /** Devuelve el producto aunque el codigo se haya tecleado con guion o con espacio. */
    public Producto obtener(String codigo) {
        String sql = "SELECT " + CAMPOS + " FROM productos p JOIN marcas m ON m.id = p.marca_id "
                + "WHERE " + Codigo.sqlNormalizado("p.codigo") + " = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Codigo.normalizar(codigo));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando producto", e);
        }
    }

    /** Registra el producto. El stock inicial NO se pone aqui: entra como movimiento de ingreso. */
    public void registrar(Producto p) {
        String sql = """
                INSERT INTO productos (codigo, nombre, marca_id, descripcion, unidad_medida,
                                       stock_actual, ubicacion)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                """;
        try (Connection con = Db.getConnection()) {
            String[] choque = codigoEquivalenteExistente(con, p.getCodigo(), null);
            if (choque != null) {
                String existente = choque[0];
                boolean estaActivo = "1".equals(choque[1]);
                String mismoTexto = existente.equals(p.getCodigo())
                        ? "" : " (para el sistema es el mismo código: los guiones y espacios no se distinguen)";
                // Un producto dado de baja no se ve en el listado: sin este aviso, el
                // "ya existe" seria un misterio (el producto esta, pero oculto).
                throw new IllegalArgumentException(estaActivo
                        ? "Ya existe el producto \"" + existente + "\"" + mismoTexto
                        : "El producto \"" + existente + "\" ya existe pero está eliminado" + mismoTexto
                          + ". Para recuperarlo, marca \"Ver también los productos eliminados\" en Buscar productos y edítalo para reactivarlo.");
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, p.getCodigo());
                ps.setString(2, p.getCodigo()); // nombre interno = codigo
                ps.setInt(3, p.getMarcaId());
                ps.setString(4, p.getDescripcion());
                ps.setString(5, p.getUnidadMedida());
                ps.setString(6, p.getUbicacion());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando producto (¿codigo duplicado?)", e);
        }
    }

    /**
     * Busca un producto ya registrado cuyo codigo sea equivalente (mismo texto ignorando
     * guiones y espacios). Como el sistema los trata como iguales, permitir los dos crearia
     * dos productos indistinguibles para quien busca.
     *
     * @param exceptoCodigo codigo a excluir de la comparacion (para no chocar consigo mismo), o null.
     * @return {codigoReal, "1" si esta activo / "0" si esta dado de baja}, o null si no hay ninguno.
     */
    private String[] codigoEquivalenteExistente(Connection con, String codigo, String exceptoCodigo)
            throws SQLException {
        String sql = "SELECT codigo, activo FROM productos WHERE " + Codigo.sqlNormalizado("codigo") + " = ?"
                + (exceptoCodigo == null ? "" : " AND codigo <> ?") + " LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Codigo.normalizar(codigo));
            if (exceptoCodigo != null) ps.setString(2, exceptoCodigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new String[]{rs.getString(1), rs.getBoolean(2) ? "1" : "0"} : null;
            }
        }
    }

    /**
     * Actualiza los datos del producto y deja constancia en el historial (tipo 'edicion').
     * El stock_actual NO se toca aqui: eso solo cambia por ingreso/egreso/ajuste.
     *
     * Todo ocurre en una sola transaccion, para que nunca quede un cambio sin su registro
     * ni un registro de un cambio que no llego a guardarse.
     *
     * Se registra el detalle de que campo cambio (valor anterior -> nuevo) junto con la
     * observacion opcional que escriba el usuario. Si no cambio ningun campo y tampoco
     * hay observacion, no se escribe nada en el historial (para no llenarlo de ruido).
     *
     * @return el texto que quedo en el historial, o null si no habia nada que registrar.
     */
    public String actualizar(Producto p, String observaciones, int usuarioId) {
        String sqlSelect = """
                SELECT p.marca_id, p.descripcion, p.unidad_medida,
                       p.ubicacion, p.activo, p.stock_actual, p.nota_maletas, m.nombre AS marca
                FROM productos p JOIN marcas m ON m.id = p.marca_id
                WHERE p.codigo = ? FOR UPDATE
                """;
        String sqlUpdate = """
                UPDATE productos SET marca_id = ?, descripcion = ?, unidad_medida = ?,
                                     ubicacion = ?, activo = ?, nota_maletas = ?
                WHERE codigo = ?
                """;

        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                String marcaAntes, descAntes, unidadAntes, ubicAntes, notaMaletasAntes;
                int marcaIdAntes, stockActual;
                boolean activoAntes;
                try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                    ps.setString(1, p.getCodigo());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Producto no encontrado: " + p.getCodigo());
                        marcaIdAntes = rs.getInt("marca_id");
                        marcaAntes = rs.getString("marca");
                        descAntes = rs.getString("descripcion");
                        unidadAntes = rs.getString("unidad_medida");
                        ubicAntes = rs.getString("ubicacion");
                        activoAntes = rs.getBoolean("activo");
                        stockActual = rs.getInt("stock_actual");
                        notaMaletasAntes = rs.getString("nota_maletas");
                    }
                }

                List<String> cambios = new ArrayList<>();
                if (marcaIdAntes != p.getMarcaId()) {
                    cambios.add("Marca: " + marcaAntes + " -> " + nombreMarca(con, p.getMarcaId()));
                }
                if (!iguales(ubicAntes, p.getUbicacion())) {
                    cambios.add("Ubicación: " + textoODefecto(ubicAntes) + " -> " + textoODefecto(p.getUbicacion()));
                }
                if (!iguales(unidadAntes, p.getUnidadMedida())) {
                    cambios.add("Unidad: " + textoODefecto(unidadAntes) + " -> " + textoODefecto(p.getUnidadMedida()));
                }
                if (!iguales(notaMaletasAntes, p.getNotaMaletas())) {
                    // El contenido de la maleta es texto de bitacora: se registra el estado
                    // nuevo completo (recortado si fuera muy largo, en registrarEdicion).
                    String nuevo = p.getNotaMaletas() == null || p.getNotaMaletas().isBlank()
                            ? "(vacío)" : p.getNotaMaletas().trim();
                    cambios.add("Contenido de la maleta actualizado a: \"" + nuevo + "\"");
                }
                if (!iguales(descAntes, p.getDescripcion())) {
                    // La descripcion puede tener parrafos enteros: se deja constancia del
                    // cambio sin volcar el texto completo dentro del historial.
                    cambios.add("Descripción modificada");
                }
                if (activoAntes != p.isActivo()) {
                    cambios.add("Estado: " + (activoAntes ? "Activo" : "Inactivo")
                            + " -> " + (p.isActivo() ? "Activo" : "Inactivo"));
                }

                boolean hayObservacion = observaciones != null && !observaciones.isBlank();
                if (cambios.isEmpty() && !hayObservacion) {
                    con.rollback();
                    return null;
                }

                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, p.getMarcaId());
                    ps.setString(2, p.getDescripcion());
                    ps.setString(3, p.getUnidadMedida());
                    ps.setString(4, p.getUbicacion());
                    ps.setBoolean(5, p.isActivo());
                    ps.setString(6, p.getNotaMaletas() == null || p.getNotaMaletas().isBlank() ? null : p.getNotaMaletas().trim());
                    ps.setString(7, p.getCodigo());
                    ps.executeUpdate();
                }

                String detalle = cambios.isEmpty() ? "Sin cambios en los datos" : String.join("; ", cambios);
                if (hayObservacion) detalle += ". " + observaciones.trim();

                new MovimientoDAO().registrarEdicion(con, p.getCodigo(), stockActual, detalle, usuarioId);

                con.commit();
                return detalle;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando producto", e);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando producto", e);
        }
    }

    private String nombreMarca(Connection con, int marcaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT nombre FROM marcas WHERE id = ?")) {
            ps.setInt(1, marcaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : ("#" + marcaId);
            }
        }
    }

    /** Compara tratando null y "" como lo mismo (la BD guarda null; el formulario manda ""). */
    private boolean iguales(String a, String b) {
        return (a == null ? "" : a.trim()).equals(b == null ? "" : b.trim());
    }

    private String textoODefecto(String s) {
        return (s == null || s.isBlank()) ? "(vacío)" : s.trim();
    }

    /** Asocia (o quita, si archivo es null) la foto de referencia comprimida ya guardada en disco. */
    public void actualizarImagen(String codigo, String archivo) {
        String sql = "UPDATE productos SET imagen = ? WHERE codigo = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, archivo);
            ps.setString(2, codigo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error asociando la imagen al producto", e);
        }
    }

    /**
     * Corrige el codigo (PK/FK) de un producto que quedo mal cargado -- por ejemplo,
     * datos importados desde Excel con la celda de codigo vacia o con un caracter suelto.
     * Como movimientos.producto_codigo no tiene ON UPDATE CASCADE, el cambio se hace
     * en 3 pasos dentro de una transaccion: crear la fila con el codigo nuevo, mover
     * el historial de movimientos hacia el, y recien ahi borrar la fila vieja.
     *
     * @throws IllegalArgumentException si el producto actual no existe o el codigo
     *         nuevo ya esta en uso por otro producto.
     */
    public void renombrarCodigo(String codigoActual, String codigoNuevo) {
        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM productos WHERE codigo = ?")) {
                    ps.setString(1, codigoActual);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("El producto " + codigoActual + " no existe");
                        }
                    }
                }
                String[] choque = codigoEquivalenteExistente(con, codigoNuevo, codigoActual);
                if (choque != null) {
                    throw new IllegalArgumentException("Ya existe un producto con el código " + choque[0]
                            + (choque[0].equals(codigoNuevo) ? ""
                               : " (para el sistema es el mismo: los guiones y espacios no se distinguen)")
                            + ("0".equals(choque[1]) ? ", aunque esté eliminado" : ""));
                }

                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT INTO productos (codigo, nombre, marca_id, descripcion, imagen, unidad_medida,
                                                stock_actual, nota_maletas, ubicacion, activo, creado_en)
                        SELECT ?, ?, marca_id, descripcion, imagen, unidad_medida,
                               stock_actual, nota_maletas, ubicacion, activo, creado_en
                        FROM productos WHERE codigo = ?
                        """)) {
                    ps.setString(1, codigoNuevo);
                    ps.setString(2, codigoNuevo); // nombre interno = codigo, igual que al registrar
                    ps.setString(3, codigoActual);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE movimientos SET producto_codigo = ? WHERE producto_codigo = ?")) {
                    ps.setString(1, codigoNuevo);
                    ps.setString(2, codigoActual);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement("DELETE FROM productos WHERE codigo = ?")) {
                    ps.setString(1, codigoActual);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (IllegalArgumentException e) {
                con.rollback();
                throw e;
            } catch (Exception e) {
                con.rollback();
                throw new RuntimeException("Error renombrando el código", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error renombrando el código", e);
        }
    }

    /** Baja logica: el producto deja de aparecer pero conserva su historial. */
    public void darDeBaja(String codigo) {
        String sql = "UPDATE productos SET activo = 0 WHERE codigo = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error dando de baja el producto", e);
        }
    }
}
