package Servlets;

import Dao.MovimientoDAO;
import Objetos.Usuario;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Corrige/aclara un movimiento ya guardado (por ejemplo una observacion de salida
 * con un dato equivocado u olvidado), sin borrar ni editar el registro original:
 * agrega una nueva entrada tipo 'correccion' que documenta el cambio (marcada con un
 * triangulo de advertencia en el historial). Solo admin/superadmin.
 */
@WebServlet("/CorregirMovimientoServlet")
public class CorregirMovimientoServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario u = usuarioActual(request);
        if (u == null || !u.esAdmin()) {
            responderError(response, 403, "Requiere permisos de administrador");
            return;
        }

        JsonObject json = leerJson(request);
        int movimientoId = entero(json, "movimiento_id", 0);
        String correccion = texto(json, "correccion");

        if (movimientoId <= 0 || correccion == null || correccion.isBlank()) {
            responderError(response, 400, "movimiento_id y correccion son requeridos");
            return;
        }

        try {
            new MovimientoDAO().corregirObservacion(movimientoId, correccion, u.getId());
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
            return;
        }

        response.setStatus(201);
        responderJson(response, Map.of("ok", true));
    }
}
