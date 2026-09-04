package Servlets;

import Dao.HerramientaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Catalogo de la bodega de herramientas.
 * GET ?q=&tipo=&reposicion=1
 *   q          busca en codigo, nombre y observaciones
 *   tipo       "herramienta" | "consumible"
 *   reposicion "1" = solo lo que hay que reponer (danadas o bajo stock minimo)
 */
@WebServlet("/HerramientasServlet")
public class HerramientasServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        responderJson(response, new HerramientaDAO().listar(
                request.getParameter("q"),
                request.getParameter("tipo"),
                "1".equals(request.getParameter("reposicion"))));
    }
}
