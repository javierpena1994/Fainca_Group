package Servlets;

import Dao.HerramientaDAO;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Corrige el nombre con el que quedo registrada una herramienta.
 * POST {nombre, nombre_nuevo}
 *
 * El nombre nuevo se guarda en mayusculas y sin espacios sobrantes, y el cambio
 * queda anotado en el historial como 'edicion' (no mueve stock).
 */
@WebServlet("/RenombrarHerramientaServlet")
public class RenombrarHerramientaServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String nombre = texto(json, "nombre");
        String nombreNuevo = texto(json, "nombre_nuevo");

        if (nombre == null || nombre.isBlank()) {
            responderError(response, 400, "Falta la herramienta a renombrar");
            return;
        }
        if (nombreNuevo == null || nombreNuevo.isBlank()) {
            responderError(response, 400, "El nombre nuevo es obligatorio");
            return;
        }

        try {
            responderJson(response, new HerramientaDAO().renombrar(
                    nombre, nombreNuevo, usuarioActual(request).getId()));
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
        }
    }
}
