package Servlets;

import Dao.HerramientaDAO;
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
 * Ajuste de la bodega de herramientas: suma o resta cantidades y/o cambia la
 * observacion de cada herramienta.
 *
 * POST {motivo, lineas: [{nombre, delta, observacion?}, ...]}
 *   delta       positivo suma, negativo resta, 0 = solo cambiar la observacion
 *   observacion si viene, reemplaza la de la herramienta; si no viene, no se toca
 *
 * Es todo o nada: si una linea no cabe, no se aplica ninguna.
 */
@WebServlet("/AjusteHerramientasServlet")
public class AjusteHerramientasServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String motivo = texto(json, "motivo");

        if (motivo == null || motivo.isBlank()) {
            responderError(response, 400, "El motivo es obligatorio: queda en el historial");
            return;
        }
        if (json == null || !json.has("lineas") || !json.get("lineas").isJsonArray()
                || json.getAsJsonArray("lineas").isEmpty()) {
            responderError(response, 400, "Indique al menos una herramienta");
            return;
        }

        List<HerramientaDAO.LineaAjuste> lineas = new ArrayList<>();
        JsonArray arreglo = json.getAsJsonArray("lineas");
        for (int i = 0; i < arreglo.size(); i++) {
            JsonObject l = arreglo.get(i).getAsJsonObject();
            String nombre = texto(l, "nombre");
            if (nombre == null || nombre.isBlank()) {
                responderError(response, 400, "La fila " + (i + 1) + " no tiene herramienta");
                return;
            }
            lineas.add(new HerramientaDAO.LineaAjuste(
                    nombre, entero(l, "delta", 0), texto(l, "observacion")));
        }

        try {
            responderJson(response, new HerramientaDAO().ajustarLote(
                    lineas, motivo, usuarioActual(request).getId()));
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
        }
    }
}
