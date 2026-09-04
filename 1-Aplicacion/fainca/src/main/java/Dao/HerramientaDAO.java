package Dao;

import Objetos.Herramienta;
import Objetos.MovimientoHerramienta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Catalogo y stock de la bodega de herramientas.
 *
 * Los items NO llevan codigo: el nombre los identifica (es UNIQUE en la base, con
 * collation que ignora mayusculas y acentos). La marca, el estado o lo que le falte
 * a una herramienta van en su observacion.
 *
 * Misma filosofia que el inventario de productos: los contadores nunca se editan
 * directo, cada cambio pasa por una transaccion que ademas deja su rastro en
 * movimientos_herramientas. Invariante: fuera = total - disponible - danadas.
 */
public class HerramientaDAO {

    /**
     * Catalogo con filtros opcionales (null/false para omitirlos).
     * "q" busca en el nombre y en las observaciones.
     * "soloReposicion" deja solo lo que hay que reponer: items con unidades danadas
     * o consumibles en (o bajo) su stock minimo.
     */
    public List<Herramienta> listar(String q, String tipo, boolean soloReposicion) {
        StringBuilder sql = new StringBuilder("""
                SELECT h.id, h.nombre, h.tipo, h.cantidad_total, h.cantidad_disponible,
                       h.cantidad_danada, h.stock_minimo, h.observaciones, h.activo,
                       COALESCE(p.perdidas, 0) AS perdidas
                FROM herramientas h
                LEFT JOIN (SELECT herramienta_id, SUM(cantidad) AS perdidas
                           FROM movimientos_herramientas WHERE tipo = 'perdida'
                           GROUP BY herramienta_id) p ON p.herramienta_id = h.id
                WHERE h.activo = 1
                """);
        List<Object> valores = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            sql.append(" AND (h.nombre LIKE ? OR h.observaciones LIKE ?)");
            String contiene = "%" + q.trim() + "%";
            valores.add(contiene);
            valores.add(contiene);
        }
        if (tipo != null && List.of("herramienta", "consumible").contains(tipo)) {
            sql.append(" AND h.tipo = ?");
            valores.add(tipo);
        }
        if (soloReposicion) {
            sql.append(" AND (h.cantidad_danada > 0 OR (h.tipo = 'consumible'")
               .append(" AND h.stock_minimo IS NOT NULL AND h.cantidad_disponible <= h.stock_minimo))");
        }
        sql.append(" ORDER BY h.nombre");

        List<Herramienta> lista = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setObject(i + 1, valores.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando herramientas", e);
        }
    }

    /**
     * Alta de un item nuevo. Si trae cantidad inicial, queda registrada como un
     * 'ingreso' en el libro de movimientos, en la misma transaccion.
     */
    public Herramienta crear(String nombre, String tipo, int cantidadInicial,
                             Integer stockMinimo, String observaciones, int usuarioId) {
        String sqlInsert = """
                INSERT INTO herramientas (nombre, tipo, cantidad_total,
                                          cantidad_disponible, stock_minimo, observaciones)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        String limpio = limpiarNombre(nombre);
        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (buscarPorNombre(con, limpio, false) != null) {
                    throw new IllegalArgumentException(
                            "Ya hay una herramienta registrada como \"" + limpio + "\"");
                }
                int id;
                try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, limpio);
                    ps.setString(2, tipo);
                    ps.setInt(3, cantidadInicial);
                    ps.setInt(4, cantidadInicial);
                    if (stockMinimo == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, stockMinimo);
                    ps.setString(6, observaciones);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        id = rs.getInt(1);
                    }
                }
                if (cantidadInicial > 0) {
                    anotar(con, id, null, "ingreso", UUID.randomUUID().toString(), cantidadInicial,
                            cantidadInicial, "Alta en el catálogo con " + cantidadInicial + " unidad(es)",
                            usuarioId);
                }
                con.commit();

                Herramienta h = new Herramienta();
                h.setId(id);
                h.setNombre(limpio);
                h.setTipo(tipo);
                h.setTotal(cantidadInicial);
                h.setDisponible(cantidadInicial);
                h.setStockMinimo(stockMinimo);
                h.setObservaciones(observaciones);
                return h;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando la herramienta", e);
        }
    }

    /**
     * Resuelve unidades danadas. Sumar y restar cantidades NO va por aqui: eso vive
     * en la pantalla "Ajuste de herramientas" (ver {@link #ajustarLote}), que ademas
     * permite tratar varias herramientas en una sola operacion.
     *
     *   reparacion una danada vuelve a servir:  danadas-c, disponible+c
     *   baja       una danada se descarta:      danadas-c, total-c
     *
     * @return la herramienta con sus contadores ya actualizados.
     */
    public Herramienta accionStock(String nombre, String accion, int cantidad,
                                   String observaciones, int usuarioId) {
        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                Herramienta h = buscarPorNombre(con, limpiarNombre(nombre), true);
                if (h == null) {
                    throw new IllegalArgumentException("Herramienta no encontrada: " + nombre);
                }

                int movimiento; // lo que se anota en el libro
                switch (accion) {
                    case "reparacion" -> {
                        exigirPositiva(cantidad);
                        if (cantidad > h.getDanadas()) {
                            throw new IllegalArgumentException(
                                    "Solo hay " + h.getDanadas() + " unidad(es) dañada(s) de " + h.getNombre());
                        }
                        h.setDanadas(h.getDanadas() - cantidad);
                        h.setDisponible(h.getDisponible() + cantidad);
                        movimiento = cantidad;
                    }
                    case "baja" -> {
                        exigirPositiva(cantidad);
                        if (cantidad > h.getDanadas()) {
                            throw new IllegalArgumentException(
                                    "Solo hay " + h.getDanadas() + " unidad(es) dañada(s) de " + h.getNombre());
                        }
                        h.setDanadas(h.getDanadas() - cantidad);
                        h.setTotal(h.getTotal() - cantidad);
                        movimiento = cantidad;
                    }
                    default -> throw new IllegalArgumentException("Acción no válida: " + accion);
                }

                actualizarContadores(con, h);
                anotar(con, h.getId(), null, accion, UUID.randomUUID().toString(), movimiento,
                        h.getDisponible(), observaciones, usuarioId);
                con.commit();
                h.setFuera(h.getTotal() - h.getDisponible() - h.getDanadas());
                return h;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando el stock de la herramienta", e);
        }
    }

    /**
     * Una linea de la pantalla "Ajuste de herramientas".
     *
     * @param delta       cuanto sumar (positivo) o restar (negativo); 0 = no tocar el stock
     * @param observacion nueva observacion de la herramienta; null = dejarla como esta
     */
    public record LineaAjuste(String nombre, int delta, String observacion) {}

    /**
     * Ajusta varias herramientas en una sola operacion: suma o resta cantidades y/o
     * cambia sus observaciones (marca, estado, si le falta algo).
     *
     * Es TODO O NADA: si una linea no cabe (dejaria el stock en negativo), no se
     * aplica ninguna. Asi el usuario corrige y reintenta sin quedar a medias.
     *
     * Todas las lineas comparten lote, de modo que el historial muestra el ajuste
     * como UNA sola fila con su lista de herramientas. Cambiar la cantidad se anota
     * como 'ajuste' y cambiar la observacion como 'edicion' (no mueve stock), para
     * que en el historial se distinga que fue lo que se toco.
     *
     * @return las herramientas con sus contadores ya actualizados.
     */
    public List<Herramienta> ajustarLote(List<LineaAjuste> lineas, String motivo, int usuarioId) {
        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                String lote = UUID.randomUUID().toString();
                List<Herramienta> resultado = new ArrayList<>();

                for (LineaAjuste linea : lineas) {
                    Herramienta h = buscarPorNombre(con, limpiarNombre(linea.nombre()), true);
                    if (h == null) {
                        throw new IllegalArgumentException(
                                "\"" + linea.nombre() + "\" no está en el catálogo de herramientas");
                    }

                    boolean cambiaObservacion = linea.observacion() != null
                            && !linea.observacion().equals(h.getObservaciones() == null ? "" : h.getObservaciones());

                    if (linea.delta() == 0 && !cambiaObservacion) {
                        throw new IllegalArgumentException(
                                h.getNombre() + ": no se indicó ninguna cantidad ni cambio de observación");
                    }

                    if (linea.delta() != 0) {
                        if (h.getDisponible() + linea.delta() < 0) {
                            throw new IllegalArgumentException(h.getNombre() + ": solo hay "
                                    + h.getDisponible() + " disponible(s) en bodega y se intenta restar "
                                    + Math.abs(linea.delta()));
                        }
                        h.setDisponible(h.getDisponible() + linea.delta());
                        h.setTotal(h.getTotal() + linea.delta());
                        actualizarContadores(con, h);
                        anotar(con, h.getId(), null, "ajuste", lote, linea.delta(),
                                h.getDisponible(), motivo, usuarioId);
                    }

                    if (cambiaObservacion) {
                        String antes = h.getObservaciones() == null || h.getObservaciones().isBlank()
                                ? "(vacía)" : h.getObservaciones();
                        try (PreparedStatement ps = con.prepareStatement(
                                "UPDATE herramientas SET observaciones = ? WHERE id = ?")) {
                            ps.setString(1, linea.observacion().isBlank() ? null : linea.observacion());
                            ps.setInt(2, h.getId());
                            ps.executeUpdate();
                        }
                        h.setObservaciones(linea.observacion());
                        // Cantidad 0: la edicion no mueve stock, pero deja su rastro.
                        anotar(con, h.getId(), null, "edicion", lote, 0, h.getDisponible(),
                                "Observación: \"" + antes + "\" → \""
                                        + (linea.observacion().isBlank() ? "(vacía)" : linea.observacion())
                                        + "\"" + (motivo != null && !motivo.isBlank() ? " — " + motivo : ""),
                                usuarioId);
                    }

                    h.setFuera(h.getTotal() - h.getDisponible() - h.getDanadas());
                    resultado.add(h);
                }

                con.commit();
                return resultado;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando el ajuste de herramientas", e);
        }
    }

    /**
     * Libro de movimientos, del mas reciente al mas antiguo (hasta 1000 lineas).
     * Cada linea trae su lote y cuantas lineas tiene ese lote, para que la pantalla
     * las agrupe en UNA fila por operacion.
     */
    public List<MovimientoHerramienta> historial(String q, String tipo, String desde, String hasta) {
        StringBuilder sql = new StringBuilder("""
                SELECT mv.id, h.nombre, mv.tipo, mv.lote, mv.cantidad, mv.disponible_resultante,
                       mv.acta_id, mv.observaciones, u.nombre AS usuario,
                       DATE_FORMAT(mv.fecha, '%Y-%m-%d %H:%i:%s') AS fecha,
                       COUNT(*) OVER (PARTITION BY COALESCE(mv.lote, CONCAT('id:', mv.id))) AS items_lote
                FROM movimientos_herramientas mv
                JOIN herramientas h ON h.id = mv.herramienta_id
                JOIN usuarios u ON u.id = mv.usuario_id
                WHERE 1 = 1
                """);
        List<Object> valores = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            sql.append(" AND (h.nombre LIKE ? OR mv.observaciones LIKE ?)");
            String contiene = "%" + q.trim() + "%";
            valores.add(contiene);
            valores.add(contiene);
        }
        if (tipo != null && TIPOS.contains(tipo)) {
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
        // Las lineas de un mismo lote quedan juntas y en su orden de registro, para
        // que la pantalla las pueda agrupar recorriendo la lista una sola vez.
        sql.append(" ORDER BY mv.fecha DESC, COALESCE(mv.lote, CONCAT('id:', mv.id)), mv.id LIMIT 1000");

        List<MovimientoHerramienta> lista = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setObject(i + 1, valores.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearMovimiento(rs, true));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el historial de herramientas", e);
        }
    }

    /**
     * Todas las lineas de una operacion (lote), en el orden en que se registraron.
     * Es lo que se despliega al abrir una fila del historial: no depende de lo que
     * haya alcanzado a cargar la pantalla.
     */
    public List<MovimientoHerramienta> porLote(String lote) {
        return consultarLineas("WHERE mv.lote = ?", lote);
    }

    /** Una linea suelta, para los movimientos que no tienen lote. */
    public List<MovimientoHerramienta> porId(int id) {
        return consultarLineas("WHERE mv.id = ?", id);
    }

    private List<MovimientoHerramienta> consultarLineas(String filtro, Object valor) {
        String sql = """
                SELECT mv.id, h.nombre, mv.tipo, mv.lote, mv.cantidad, mv.disponible_resultante,
                       mv.acta_id, mv.observaciones, u.nombre AS usuario,
                       DATE_FORMAT(mv.fecha, '%Y-%m-%d %H:%i:%s') AS fecha
                FROM movimientos_herramientas mv
                JOIN herramientas h ON h.id = mv.herramienta_id
                JOIN usuarios u ON u.id = mv.usuario_id
                """ + filtro + " ORDER BY mv.id";
        List<MovimientoHerramienta> lineas = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lineas.add(mapearMovimiento(rs, false));
            }
            return lineas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando la operación", e);
        }
    }

    // ------------------------------------------------------------------
    // Compartido con ActaHerramientaDAO (mismo paquete)
    // ------------------------------------------------------------------

    static final List<String> TIPOS = List.of("ingreso", "entrega", "devolucion", "dano",
            "perdida", "reparacion", "baja", "ajuste", "edicion", "correccion");

    /**
     * Busca una herramienta por su nombre (la collation de la tabla ignora
     * mayusculas y acentos). Con bloqueo cuando se va a modificar su stock.
     */
    static Herramienta buscarPorNombre(Connection con, String nombre, boolean bloquear) throws SQLException {
        String sql = "SELECT * FROM herramientas WHERE nombre = ? AND activo = 1"
                + (bloquear ? " FOR UPDATE" : "");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Normaliza el nombre de una herramienta: quita espacios sobrantes (para que
     * "TALADRO " y "TALADRO" no se dupliquen) y lo pasa a MAYUSCULAS, que es como
     * se registran en bodega. Es el unico punto donde se decide como se guarda un
     * nombre, asi que da igual si llega en minusculas desde la pantalla o desde
     * una llamada directa al servlet: siempre queda igual.
     */
    static String limpiarNombre(String nombre) {
        return nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /**
     * Cambia el nombre de una herramienta (por ejemplo para corregir como se
     * escribio al registrarla) y deja el cambio anotado en el historial.
     *
     * Renombrar es seguro para el historial: las actas y los movimientos apuntan
     * al id de la herramienta, no a su nombre, asi que ningun registro anterior se
     * pierde ni se despega. Lo que si cambia es COMO se ve ese pasado, porque las
     * consultas muestran el nombre actual; por eso el cambio queda registrado.
     */
    public Herramienta renombrar(String nombreActual, String nombreNuevo, int usuarioId) {
        String nuevo = limpiarNombre(nombreNuevo);
        if (nuevo.isBlank()) {
            throw new IllegalArgumentException("El nombre nuevo no puede quedar vacío");
        }
        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                Herramienta h = buscarPorNombre(con, limpiarNombre(nombreActual), true);
                if (h == null) {
                    throw new IllegalArgumentException("Herramienta no encontrada: " + nombreActual);
                }
                String anterior = h.getNombre();
                if (anterior.equals(nuevo)) {
                    throw new IllegalArgumentException("La herramienta ya se llama \"" + nuevo + "\"");
                }
                Herramienta ocupado = buscarPorNombre(con, nuevo, false);
                if (ocupado != null && ocupado.getId() != h.getId()) {
                    throw new IllegalArgumentException(
                            "Ya hay otra herramienta registrada como \"" + nuevo + "\"");
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE herramientas SET nombre = ? WHERE id = ?")) {
                    ps.setString(1, nuevo);
                    ps.setInt(2, h.getId());
                    ps.executeUpdate();
                }
                h.setNombre(nuevo);

                // Cantidad 0: renombrar no mueve stock, pero tiene que dejar rastro.
                anotar(con, h.getId(), null, "edicion", UUID.randomUUID().toString(), 0,
                        h.getDisponible(), "Nombre: \"" + anterior + "\" → \"" + nuevo + "\"", usuarioId);

                con.commit();
                return h;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error renombrando la herramienta", e);
        }
    }

    /** Guarda los contadores del objeto tal como estan, validando que ninguno quede negativo. */
    static void actualizarContadores(Connection con, Herramienta h) throws SQLException {
        if (h.getTotal() < 0 || h.getDisponible() < 0 || h.getDanadas() < 0
                || h.getTotal() - h.getDisponible() - h.getDanadas() < 0) {
            throw new IllegalArgumentException(
                    "La operación dejaría el stock de " + h.getNombre() + " en negativo");
        }
        String sql = "UPDATE herramientas SET cantidad_total = ?, cantidad_disponible = ?, cantidad_danada = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, h.getTotal());
            ps.setInt(2, h.getDisponible());
            ps.setInt(3, h.getDanadas());
            ps.setInt(4, h.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Anota una linea en el libro de movimientos, dentro de la transaccion del que
     * llama. Todas las lineas de una misma operacion deben recibir el MISMO lote:
     * de ahi sale la agrupacion del historial.
     */
    static void anotar(Connection con, int herramientaId, Integer actaId, String tipo, String lote,
                       int cantidad, int disponibleResultante, String observaciones,
                       int usuarioId) throws SQLException {
        String sql = """
                INSERT INTO movimientos_herramientas
                    (herramienta_id, acta_id, tipo, lote, cantidad, disponible_resultante,
                     observaciones, usuario_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, herramientaId);
            if (actaId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, actaId);
            ps.setString(3, tipo);
            ps.setString(4, lote);
            ps.setInt(5, cantidad);
            ps.setInt(6, disponibleResultante);
            ps.setString(7, recortar(observaciones));
            ps.setInt(8, usuarioId);
            ps.executeUpdate();
        }
    }

    static Herramienta mapear(ResultSet rs) throws SQLException {
        Herramienta h = new Herramienta();
        h.setId(rs.getInt("id"));
        h.setNombre(rs.getString("nombre"));
        h.setTipo(rs.getString("tipo"));
        h.setTotal(rs.getInt("cantidad_total"));
        h.setDisponible(rs.getInt("cantidad_disponible"));
        h.setDanadas(rs.getInt("cantidad_danada"));
        int minimo = rs.getInt("stock_minimo");
        h.setStockMinimo(rs.wasNull() ? null : minimo);
        h.setObservaciones(rs.getString("observaciones"));
        h.setActivo(rs.getBoolean("activo"));
        h.setFuera(h.getTotal() - h.getDisponible() - h.getDanadas());
        try {
            h.setPerdidas(rs.getInt("perdidas"));
        } catch (SQLException ignorada) {
            // la consulta no traia la columna (mapeos desde SELECT * de una sola tabla)
        }
        return h;
    }

    private static MovimientoHerramienta mapearMovimiento(ResultSet rs, boolean conItemsLote)
            throws SQLException {
        MovimientoHerramienta mv = new MovimientoHerramienta();
        mv.setId(rs.getInt("id"));
        mv.setNombre(rs.getString("nombre"));
        mv.setTipo(rs.getString("tipo"));
        mv.setLote(rs.getString("lote"));
        mv.setCantidad(rs.getInt("cantidad"));
        mv.setDisponibleResultante(rs.getInt("disponible_resultante"));
        int actaId = rs.getInt("acta_id");
        if (!rs.wasNull()) {
            mv.setActaId(actaId);
            mv.setActaNumero(ActaHerramientaDAO.numero(actaId));
        }
        mv.setObservaciones(rs.getString("observaciones"));
        mv.setUsuario(rs.getString("usuario"));
        mv.setFecha(rs.getString("fecha"));
        mv.setItemsLote(conItemsLote ? rs.getInt("items_lote") : 1);
        return mv;
    }

    private static void exigirPositiva(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
    }

    private static String recortar(String texto) {
        if (texto == null) return null;
        return texto.length() <= 1000 ? texto : texto.substring(0, 997) + "...";
    }
}
