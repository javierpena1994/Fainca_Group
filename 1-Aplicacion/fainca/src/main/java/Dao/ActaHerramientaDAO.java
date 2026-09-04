package Dao;

import Objetos.ActaHerramienta;
import Objetos.ActaLineaHerramienta;
import Objetos.Herramienta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Actas de entrega de la bodega de herramientas.
 *
 * Un acta agrupa todo lo que un tecnico se llevo en una operacion. A diferencia de
 * los movimientos de productos (que aceptan exito parcial), el acta se registra
 * COMPLETA o no se registra: es un documento que se firma, no puede quedar a medias.
 *
 * Cada operacion (la entrega y cada tanda de devolucion) comparte un "lote" en el
 * libro de movimientos, para que el historial muestre una sola fila por lista.
 */
public class ActaHerramientaDAO {

    /** Una linea tal como llega de la pantalla de entrega (la herramienta va por nombre). */
    public record LineaNueva(String nombre, int cantidad, String observacion) {}

    /** Lo devuelto de una linea en una tanda de devolucion. */
    public record Devolucion(int lineaId, int ok, int danado, int perdido) {}

    public static String numero(int id) {
        return "HER-" + String.format("%06d", id);
    }

    /**
     * Registra el acta completa en una sola transaccion: cabecera, lineas, descuento
     * de stock y rastro en el libro de movimientos. Si CUALQUIER linea falla
     * (herramienta inexistente, disponible insuficiente), no se registra nada.
     *
     * @return el id del acta creada.
     */
    public int crear(String solicitante, String proyecto, String destino, String observaciones,
                     List<LineaNueva> lineas, int usuarioId) {
        String sqlActa = """
                INSERT INTO actas_herramientas (solicitante, proyecto, destino, observaciones, usuario_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        String sqlLinea = """
                INSERT INTO actas_herramientas_lineas
                    (acta_id, herramienta_id, cantidad, observacion, consumido)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                int actaId;
                try (PreparedStatement ps = con.prepareStatement(sqlActa, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, solicitante);
                    ps.setString(2, proyecto);
                    ps.setString(3, destino);
                    ps.setString(4, observaciones);
                    ps.setInt(5, usuarioId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        actaId = rs.getInt(1);
                    }
                }

                // Toda la entrega es UNA operacion: un solo lote para todas sus lineas.
                String lote = UUID.randomUUID().toString();

                for (LineaNueva linea : lineas) {
                    if (linea.cantidad() <= 0) {
                        throw new IllegalArgumentException(
                                linea.nombre() + ": la cantidad debe ser mayor que cero");
                    }

                    Herramienta h = HerramientaDAO.buscarPorNombre(
                            con, HerramientaDAO.limpiarNombre(linea.nombre()), true);
                    if (h == null) {
                        throw new IllegalArgumentException("\"" + linea.nombre()
                                + "\" no está registrada en el catálogo de herramientas");
                    }
                    if (linea.cantidad() > h.getDisponible()) {
                        throw new IllegalArgumentException(h.getNombre() + ": solo hay "
                                + h.getDisponible() + " disponible(s) y se pidieron " + linea.cantidad());
                    }

                    boolean consumible = "consumible".equals(h.getTipo());
                    h.setDisponible(h.getDisponible() - linea.cantidad());
                    if (consumible) {
                        // Los consumibles no vuelven: salen del total desde ya, y la linea
                        // nace saldada (consumido = cantidad) para no dejar el acta abierta.
                        h.setTotal(h.getTotal() - linea.cantidad());
                    }
                    HerramientaDAO.actualizarContadores(con, h);

                    try (PreparedStatement ps = con.prepareStatement(sqlLinea)) {
                        ps.setInt(1, actaId);
                        ps.setInt(2, h.getId());
                        ps.setInt(3, linea.cantidad());
                        ps.setString(4, linea.observacion());
                        ps.setInt(5, consumible ? linea.cantidad() : 0);
                        ps.executeUpdate();
                    }

                    HerramientaDAO.anotar(con, h.getId(), actaId, "entrega", lote, linea.cantidad(),
                            h.getDisponible(),
                            "Acta " + numero(actaId) + " — " + proyecto
                                    + (destino != null && !destino.isBlank() ? " (" + destino + ")" : "")
                                    + " — Solicitante: " + solicitante
                                    + (linea.observacion() != null && !linea.observacion().isBlank()
                                        ? " — " + linea.observacion() : ""),
                            usuarioId);
                }

                // Un acta solo de consumibles nace ya saldada: se cierra en el acto.
                cerrarSiEstaSaldada(con, actaId);
                con.commit();
                return actaId;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando el acta de entrega", e);
        }
    }

    /**
     * Registra una tanda de devolucion sobre un acta abierta. De cada linea puede
     * volver una parte en buen estado, otra danada y otra declararse perdida; lo que
     * no se reporte sigue "en proyecto". Si tras la tanda no queda nada pendiente,
     * el acta se cierra sola.
     *
     * @return true si el acta quedo cerrada.
     */
    public boolean devolver(int actaId, List<Devolucion> devoluciones, String observaciones, int usuarioId) {
        String sqlActa = "SELECT estado FROM actas_herramientas WHERE id = ? FOR UPDATE";
        String sqlLinea = """
                SELECT l.*, h.nombre
                FROM actas_herramientas_lineas l
                JOIN herramientas h ON h.id = l.herramienta_id
                WHERE l.id = ? AND l.acta_id = ? FOR UPDATE
                """;
        String sqlActualizarLinea = """
                UPDATE actas_herramientas_lineas
                SET devuelto_ok = devuelto_ok + ?, devuelto_danado = devuelto_danado + ?, perdido = perdido + ?
                WHERE id = ?
                """;

        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlActa)) {
                    ps.setInt(1, actaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("El acta #" + actaId + " no existe");
                        }
                        if (!"abierta".equals(rs.getString("estado"))) {
                            throw new IllegalArgumentException(
                                    "El acta " + numero(actaId) + " ya está cerrada");
                        }
                    }
                }

                // Toda la tanda es UNA operacion, aunque genere lineas de tipos
                // distintos (devolucion, dano, perdida): comparten lote y en el
                // historial salen como una sola devolucion con su lista.
                String lote = UUID.randomUUID().toString();
                boolean algunCambio = false;

                for (Devolucion dev : devoluciones) {
                    if (dev.ok() < 0 || dev.danado() < 0 || dev.perdido() < 0) {
                        throw new IllegalArgumentException("Las cantidades no pueden ser negativas");
                    }
                    int totalTanda = dev.ok() + dev.danado() + dev.perdido();
                    if (totalTanda == 0) continue;

                    String nombre;
                    int herramientaId, pendiente;
                    try (PreparedStatement ps = con.prepareStatement(sqlLinea)) {
                        ps.setInt(1, dev.lineaId());
                        ps.setInt(2, actaId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new IllegalArgumentException(
                                        "La línea #" + dev.lineaId() + " no pertenece a este acta");
                            }
                            nombre = rs.getString("nombre");
                            herramientaId = rs.getInt("herramienta_id");
                            pendiente = rs.getInt("cantidad") - rs.getInt("devuelto_ok")
                                    - rs.getInt("devuelto_danado") - rs.getInt("perdido")
                                    - rs.getInt("consumido");
                        }
                    }
                    if (totalTanda > pendiente) {
                        throw new IllegalArgumentException(nombre + ": se reportan " + totalTanda
                                + " unidad(es) pero solo quedan " + pendiente + " pendiente(s)");
                    }

                    Herramienta h;
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT * FROM herramientas WHERE id = ? FOR UPDATE")) {
                        ps.setInt(1, herramientaId);
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            h = HerramientaDAO.mapear(rs);
                        }
                    }
                    h.setDisponible(h.getDisponible() + dev.ok());
                    h.setDanadas(h.getDanadas() + dev.danado());
                    h.setTotal(h.getTotal() - dev.perdido());
                    HerramientaDAO.actualizarContadores(con, h);

                    try (PreparedStatement ps = con.prepareStatement(sqlActualizarLinea)) {
                        ps.setInt(1, dev.ok());
                        ps.setInt(2, dev.danado());
                        ps.setInt(3, dev.perdido());
                        ps.setInt(4, dev.lineaId());
                        ps.executeUpdate();
                    }

                    String contexto = "Acta " + numero(actaId)
                            + (observaciones != null && !observaciones.isBlank() ? " — " + observaciones : "");
                    if (dev.ok() > 0) {
                        HerramientaDAO.anotar(con, herramientaId, actaId, "devolucion", lote,
                                dev.ok(), h.getDisponible(), "Devolución en buen estado — " + contexto, usuarioId);
                    }
                    if (dev.danado() > 0) {
                        HerramientaDAO.anotar(con, herramientaId, actaId, "dano", lote,
                                dev.danado(), h.getDisponible(), "Devuelto DAÑADO — " + contexto, usuarioId);
                    }
                    if (dev.perdido() > 0) {
                        HerramientaDAO.anotar(con, herramientaId, actaId, "perdida", lote,
                                dev.perdido(), h.getDisponible(), "Declarado PERDIDO — " + contexto, usuarioId);
                    }
                    algunCambio = true;
                }

                if (!algunCambio) {
                    throw new IllegalArgumentException("No se reportó ninguna cantidad a devolver");
                }

                boolean cerrada = cerrarSiEstaSaldada(con, actaId);
                con.commit();
                return cerrada;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando la devolución", e);
        }
    }

    /**
     * Lista de actas con sus totales, de la mas reciente a la mas antigua (hasta 500).
     * "q" busca por numero de acta (HER-000012 o solo el numero), solicitante,
     * proyecto, destino u observaciones.
     */
    public List<ActaHerramienta> listar(String estado, String q, String desde, String hasta) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.solicitante, a.proyecto, a.destino, a.observaciones, a.estado,
                       a.usuario_id, u.nombre AS usuario,
                       DATE_FORMAT(a.fecha, '%Y-%m-%d %H:%i:%s') AS fecha,
                       DATE_FORMAT(a.fecha_cierre, '%Y-%m-%d %H:%i:%s') AS fecha_cierre,
                       COUNT(l.id) AS items,
                       COALESCE(SUM(l.cantidad), 0) AS unidades,
                       COALESCE(SUM(l.cantidad - l.devuelto_ok - l.devuelto_danado - l.perdido - l.consumido), 0) AS pendientes
                FROM actas_herramientas a
                JOIN usuarios u ON u.id = a.usuario_id
                LEFT JOIN actas_herramientas_lineas l ON l.acta_id = a.id
                WHERE 1 = 1
                """);
        List<Object> valores = new ArrayList<>();

        if (estado != null && List.of("abierta", "cerrada").contains(estado)) {
            sql.append(" AND a.estado = ?");
            valores.add(estado);
        }
        if (q != null && !q.isBlank()) {
            String limpio = q.trim();
            String soloDigitos = limpio.toUpperCase().replaceFirst("^HER-?", "").trim();
            if (soloDigitos.matches("\\d{1,9}")) {
                sql.append(" AND a.id = ?");
                valores.add(Integer.parseInt(soloDigitos));
            } else {
                sql.append(" AND (a.solicitante LIKE ? OR a.proyecto LIKE ? OR a.destino LIKE ?")
                   .append(" OR a.observaciones LIKE ?)");
                String contiene = "%" + limpio + "%";
                for (int i = 0; i < 4; i++) valores.add(contiene);
            }
        }
        if (desde != null && desde.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sql.append(" AND a.fecha >= ?");
            valores.add(desde + " 00:00:00");
        }
        if (hasta != null && hasta.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sql.append(" AND a.fecha <= ?");
            valores.add(hasta + " 23:59:59");
        }
        sql.append(" GROUP BY a.id ORDER BY a.fecha DESC, a.id DESC LIMIT 500");

        List<ActaHerramienta> actas = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setObject(i + 1, valores.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActaHerramienta a = mapearCabecera(rs);
                    a.setItems(rs.getInt("items"));
                    a.setUnidades(rs.getInt("unidades"));
                    a.setPendientes(rs.getInt("pendientes"));
                    actas.add(a);
                }
            }
            return actas;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando las actas", e);
        }
    }

    /** El acta completa con sus lineas; null si no existe. */
    public ActaHerramienta detalle(int id) {
        String sqlActa = """
                SELECT a.id, a.solicitante, a.proyecto, a.destino, a.observaciones, a.estado,
                       a.usuario_id, u.nombre AS usuario,
                       DATE_FORMAT(a.fecha, '%Y-%m-%d %H:%i:%s') AS fecha,
                       DATE_FORMAT(a.fecha_cierre, '%Y-%m-%d %H:%i:%s') AS fecha_cierre
                FROM actas_herramientas a
                JOIN usuarios u ON u.id = a.usuario_id
                WHERE a.id = ?
                """;
        String sqlLineas = """
                SELECT l.id, l.herramienta_id, h.nombre, h.tipo, l.cantidad,
                       l.observacion, l.devuelto_ok, l.devuelto_danado, l.perdido, l.consumido
                FROM actas_herramientas_lineas l
                JOIN herramientas h ON h.id = l.herramienta_id
                WHERE l.acta_id = ?
                ORDER BY l.id
                """;
        try (Connection con = Db.getConnection()) {
            ActaHerramienta acta = null;
            try (PreparedStatement ps = con.prepareStatement(sqlActa)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) acta = mapearCabecera(rs);
                }
            }
            if (acta == null) return null;

            List<ActaLineaHerramienta> lineas = new ArrayList<>();
            int unidades = 0, pendientes = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlLineas)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ActaLineaHerramienta l = new ActaLineaHerramienta();
                        l.setId(rs.getInt("id"));
                        l.setHerramientaId(rs.getInt("herramienta_id"));
                        l.setNombre(rs.getString("nombre"));
                        l.setTipo(rs.getString("tipo"));
                        l.setCantidad(rs.getInt("cantidad"));
                        l.setObservacion(rs.getString("observacion"));
                        l.setDevueltoOk(rs.getInt("devuelto_ok"));
                        l.setDevueltoDanado(rs.getInt("devuelto_danado"));
                        l.setPerdido(rs.getInt("perdido"));
                        l.setConsumido(rs.getInt("consumido"));
                        l.setPendiente(l.getCantidad() - l.getDevueltoOk() - l.getDevueltoDanado()
                                - l.getPerdido() - l.getConsumido());
                        unidades += l.getCantidad();
                        pendientes += l.getPendiente();
                        lineas.add(l);
                    }
                }
            }
            acta.setLineas(lineas);
            acta.setItems(lineas.size());
            acta.setUnidades(unidades);
            acta.setPendientes(pendientes);
            return acta;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el acta", e);
        }
    }

    // ------------------------------------------------------------------

    /** Si el acta ya no tiene nada pendiente, la marca cerrada. Devuelve true si cerro. */
    private boolean cerrarSiEstaSaldada(Connection con, int actaId) throws SQLException {
        String sqlPendiente = """
                SELECT COALESCE(SUM(cantidad - devuelto_ok - devuelto_danado - perdido - consumido), 0)
                FROM actas_herramientas_lineas WHERE acta_id = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sqlPendiente)) {
            ps.setInt(1, actaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) return false;
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE actas_herramientas SET estado = 'cerrada', fecha_cierre = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, actaId);
            ps.executeUpdate();
        }
        return true;
    }

    private ActaHerramienta mapearCabecera(ResultSet rs) throws SQLException {
        ActaHerramienta a = new ActaHerramienta();
        a.setId(rs.getInt("id"));
        a.setNumero(numero(a.getId()));
        a.setSolicitante(rs.getString("solicitante"));
        a.setProyecto(rs.getString("proyecto"));
        a.setDestino(rs.getString("destino"));
        a.setObservaciones(rs.getString("observaciones"));
        a.setEstado(rs.getString("estado"));
        a.setUsuarioId(rs.getInt("usuario_id"));
        a.setUsuario(rs.getString("usuario"));
        a.setFecha(rs.getString("fecha"));
        a.setFechaCierre(rs.getString("fecha_cierre"));
        return a;
    }
}
