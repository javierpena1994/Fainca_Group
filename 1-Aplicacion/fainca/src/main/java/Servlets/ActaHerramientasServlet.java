package Servlets;

import Dao.ActaHerramientaDAO;
import Objetos.ActaHerramienta;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Actas de entrega de herramientas.
 *
 * POST crea el acta completa (todo o nada, porque es un documento que se firma):
 *   {solicitante, proyecto, destino?, observaciones?,
 *    lineas: [{nombre, cantidad, observacion?}, ...]}
 *
 * GET ?id=N        devuelve el acta con sus lineas
 * GET ?estado=&q=&desde=&hasta=   lista las actas con sus totales
 */
@WebServlet("/ActaHerramientasServlet")
public class ActaHerramientasServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String solicitante = texto(json, "solicitante");
        String proyecto = texto(json, "proyecto");

        if (solicitante == null || solicitante.isBlank() || proyecto == null || proyecto.isBlank()) {
            responderError(response, 400, "El solicitante y el proyecto son obligatorios");
            return;
        }
        if (json == null || !json.has("lineas") || !json.get("lineas").isJsonArray()
                || json.getAsJsonArray("lineas").isEmpty()) {
            responderError(response, 400, "El acta debe tener al menos una herramienta");
            return;
        }

        List<ActaHerramientaDAO.LineaNueva> lineas = new ArrayList<>();
        JsonArray arreglo = json.getAsJsonArray("lineas");
        for (int i = 0; i < arreglo.size(); i++) {
            JsonObject l = arreglo.get(i).getAsJsonObject();
            String nombre = texto(l, "nombre");
            if (nombre == null || nombre.isBlank()) {
                responderError(response, 400, "La fila " + (i + 1) + " no tiene herramienta");
                return;
            }
            lineas.add(new ActaHerramientaDAO.LineaNueva(
                    nombre, entero(l, "cantidad", 0), texto(l, "observacion")));
        }

        try {
            ActaHerramientaDAO dao = new ActaHerramientaDAO();
            int id = dao.crear(solicitante, proyecto, texto(json, "destino"),
                    texto(json, "observaciones"), lineas, usuarioActual(request).getId());
            responderJson(response, dao.detalle(id));
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        if (id != null && id.matches("\\d{1,9}")) {
            ActaHerramienta acta = new ActaHerramientaDAO().detalle(Integer.parseInt(id));
            if (acta == null) {
                responderError(response, 404, "El acta no existe");
            } else {
                responderJson(response, acta);
            }
            return;
        }

        responderJson(response, new ActaHerramientaDAO().listar(
                request.getParameter("estado"),
                request.getParameter("q"),
                request.getParameter("desde"),
                request.getParameter("hasta")));
    }
}
