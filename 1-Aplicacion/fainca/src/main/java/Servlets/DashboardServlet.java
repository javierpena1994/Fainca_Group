package Servlets;

import Dao.MovimientoDAO;
import Dao.ReporteDAO;
import Objetos.Movimiento;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datos del panel de control: resumen, actividad semanal y ultimos movimientos. */
@WebServlet("/DashboardServlet")
public class DashboardServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ReporteDAO reportes = new ReporteDAO();
        List<Movimiento> historial = new MovimientoDAO().historial(null, null, null, null);

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("resumen", reportes.resumen());
        salida.put("actividad", reportes.actividadSemanal());
        salida.put("ultimos", historial.subList(0, Math.min(8, historial.size())));
        responderJson(response, salida);
    }
}
