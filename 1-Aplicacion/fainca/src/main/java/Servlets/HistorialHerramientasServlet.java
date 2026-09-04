package Servlets;

import Dao.HerramientaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Libro de movimientos de la bodega de herramientas (hasta 1000, el mas reciente primero).
 * GET ?q=&tipo=&desde=&hasta=
 *   q busca en codigo, nombre y observaciones (igual que el historial de productos).
 */
@WebServlet("/HistorialHerramientasServlet")
public class HistorialHerramientasServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        responderJson(response, new HerramientaDAO().historial(
                request.getParameter("q"),
                request.getParameter("tipo"),
                request.getParameter("desde"),
                request.getParameter("hasta")));
    }
}
