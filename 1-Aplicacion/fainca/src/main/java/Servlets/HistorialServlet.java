package Servlets;

import Dao.MovimientoDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Historial de movimientos con filtros: ?codigo=&tipo=&desde=&hasta=&salto=
 *
 * "salto" es cuantos registros omitir desde el mas reciente: la pantalla manda 0
 * al abrir y va sumando de mil en mil con el boton "Cargar 1000 más".
 */
@WebServlet("/HistorialServlet")
public class HistorialServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int salto = 0;
        String param = request.getParameter("salto");
        if (param != null && param.matches("\\d{1,7}")) {
            salto = Integer.parseInt(param);
        }

        responderJson(response, new MovimientoDAO().historial(
                request.getParameter("codigo"),
                request.getParameter("tipo"),
                request.getParameter("desde"),
                request.getParameter("hasta"),
                salto));
    }
}
