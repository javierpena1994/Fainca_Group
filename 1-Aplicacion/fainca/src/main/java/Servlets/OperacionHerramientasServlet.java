package Servlets;

import Dao.HerramientaDAO;
import Objetos.MovimientoHerramienta;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Las lineas de UNA operacion del historial de herramientas (lo que se despliega
 * al abrir una fila): GET ?lote=xxx  o  ?id=123 para una linea suelta.
 *
 * Se pide al servidor en vez de reutilizar lo que ya cargo la pantalla, porque el
 * historial esta limitado a 1000 lineas y una operacion larga podria quedar cortada.
 */
@WebServlet("/OperacionHerramientasServlet")
public class OperacionHerramientasServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String lote = request.getParameter("lote");
        String id = request.getParameter("id");

        List<MovimientoHerramienta> lineas;
        if (lote != null && !lote.isBlank()) {
            lineas = new HerramientaDAO().porLote(lote.trim());
        } else if (id != null && id.matches("\\d{1,9}")) {
            lineas = new HerramientaDAO().porId(Integer.parseInt(id));
        } else {
            responderError(response, 400, "Indique lote o id");
            return;
        }

        if (lineas.isEmpty()) {
            responderError(response, 404, "La operación no existe");
            return;
        }
        responderJson(response, lineas);
    }
}
