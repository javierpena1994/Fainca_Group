package Servlets;

import Dao.HerramientaDAO;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Resuelve unidades danadas de una herramienta.
 * POST {nombre, accion, cantidad, observaciones?}
 *   accion: reparacion (una danada vuelve a servir)
 *           baja       (una danada se descarta definitivamente)
 *
 * Sumar y restar cantidades NO va por aqui: eso lo hace AjusteHerramientasServlet,
 * detras de la pantalla "Ajuste de herramientas".
 */
@WebServlet("/StockHerramientaServlet")
public class StockHerramientaServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String nombre = texto(json, "nombre");
        String accion = texto(json, "accion");
        int cantidad = entero(json, "cantidad", -1);
        String observaciones = texto(json, "observaciones");

        if (nombre == null || nombre.isBlank()) {
            responderError(response, 400, "Falta el nombre de la herramienta");
            return;
        }
        if (accion == null || !List.of("reparacion", "baja").contains(accion)) {
            responderError(response, 400, "Acción no válida");
            return;
        }
        if (observaciones == null || observaciones.isBlank()) {
            responderError(response, 400, "La observación (motivo) es obligatoria");
            return;
        }

        try {
            responderJson(response, new HerramientaDAO().accionStock(nombre, accion, cantidad,
                    observaciones, usuarioActual(request).getId()));
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
        }
    }
}
