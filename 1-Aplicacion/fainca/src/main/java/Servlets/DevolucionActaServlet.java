package Servlets;

import Dao.ActaHerramientaDAO;
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
 * Registra una tanda de devolucion sobre un acta abierta.
 * POST {acta_id, observaciones?,
 *       lineas: [{linea_id, ok, danado, perdido}, ...]}
 *
 * Lo que no se reporte queda "aun en proyecto". Si tras la tanda no queda nada
 * pendiente, el acta se cierra sola y la respuesta lo indica.
 */
@WebServlet("/DevolucionActaServlet")
public class DevolucionActaServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        int actaId = entero(json, "acta_id", 0);

        if (actaId <= 0) {
            responderError(response, 400, "Falta el número del acta");
            return;
        }
        if (json == null || !json.has("lineas") || !json.get("lineas").isJsonArray()) {
            responderError(response, 400, "No se reportó ninguna devolución");
            return;
        }

        List<ActaHerramientaDAO.Devolucion> devoluciones = new ArrayList<>();
        JsonArray arreglo = json.getAsJsonArray("lineas");
        for (int i = 0; i < arreglo.size(); i++) {
            JsonObject l = arreglo.get(i).getAsJsonObject();
            devoluciones.add(new ActaHerramientaDAO.Devolucion(
                    entero(l, "linea_id", 0), entero(l, "ok", 0),
                    entero(l, "danado", 0), entero(l, "perdido", 0)));
        }

        try {
            ActaHerramientaDAO dao = new ActaHerramientaDAO();
            boolean cerrada = dao.devolver(actaId, devoluciones,
                    texto(json, "observaciones"), usuarioActual(request).getId());
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("cerrada", cerrada);
            respuesta.add("acta", GSON.toJsonTree(dao.detalle(actaId)));
            responderJson(response, respuesta);
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
        }
    }
}
