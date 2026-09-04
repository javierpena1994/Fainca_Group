package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultas de resumen para el panel de control (dashboard). */
public class ReporteDAO {

    /** Numeros generales: productos, unidades, marcas y actividad de hoy. */
    public Map<String, Object> resumen() {
        Map<String, Object> r = new LinkedHashMap<>();
        try (Connection con = Db.getConnection()) {
            r.put("productos_activos", escalar(con,
                    "SELECT COUNT(*) FROM productos WHERE activo = 1"));
            r.put("unidades_totales", escalar(con,
                    "SELECT COALESCE(SUM(stock_actual), 0) FROM productos WHERE activo = 1"));
            r.put("marcas", escalar(con, "SELECT COUNT(*) FROM marcas"));
            // Solo movimientos que mueven stock: las anotaciones ('edicion' y 'correccion')
            // no cuentan aqui, porque esta tarjeta muestra "+X / -Y uds" y aportarian
            // 0 unidades, inflando el numero sin explicar nada.
            r.put("movimientos_hoy", escalar(con,
                    "SELECT COUNT(*) FROM movimientos WHERE DATE(fecha) = CURDATE() "
                    + "AND tipo NOT IN ('edicion', 'correccion')"));
            r.put("unidades_ingresadas_hoy", escalar(con,
                    "SELECT COALESCE(SUM(cantidad), 0) FROM movimientos WHERE tipo = 'ingreso' AND DATE(fecha) = CURDATE()"));
            r.put("unidades_salidas_hoy", escalar(con,
                    "SELECT COALESCE(SUM(cantidad), 0) FROM movimientos WHERE tipo = 'egreso' AND DATE(fecha) = CURDATE()"));
            return r;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando resumen", e);
        }
    }

    /**
     * Unidades ingresadas y salidas por dia, ultimos 7 dias (incluye dias sin
     * movimiento con 0, para que el grafico siempre tenga las 7 columnas).
     */
    public List<Map<String, Object>> actividadSemanal() {
        String sql = """
                SELECT DATE(fecha) AS dia, tipo, SUM(cantidad) AS unidades
                FROM movimientos
                WHERE fecha >= CURDATE() - INTERVAL 6 DAY AND tipo IN ('ingreso', 'egreso')
                GROUP BY dia, tipo
                """;
        // Inicializar los 7 dias en orden con ceros
        Map<LocalDate, long[]> porDia = new LinkedHashMap<>(); // [ingresos, salidas]
        LocalDate hoy = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            porDia.put(hoy.minusDays(i), new long[]{0, 0});
        }

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate dia = rs.getDate("dia").toLocalDate();
                long[] valores = porDia.get(dia);
                if (valores == null) continue;
                if ("ingreso".equals(rs.getString("tipo"))) {
                    valores[0] = rs.getLong("unidades");
                } else {
                    valores[1] = rs.getLong("unidades");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando actividad semanal", e);
        }

        List<Map<String, Object>> salida = new ArrayList<>();
        porDia.forEach((dia, valores) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fecha", dia.toString());
            m.put("ingresos", valores[0]);
            m.put("salidas", valores[1]);
            salida.add(m);
        });
        return salida;
    }

    private long escalar(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
