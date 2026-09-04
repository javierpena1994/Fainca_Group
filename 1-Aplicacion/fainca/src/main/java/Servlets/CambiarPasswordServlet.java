package Servlets;

import Dao.UsuarioDAO;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/CambiarPasswordServlet")
public class CambiarPasswordServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String actual = texto(json, "passwordActual");
        String nueva = texto(json, "passwordNueva");

        if (actual == null || nueva == null || nueva.length() < 6) {
            responderError(response, 400, "La contrasena nueva debe tener al menos 6 caracteres");
            return;
        }

        boolean ok = new UsuarioDAO().cambiarPassword(usuarioActual(request).getId(), actual, nueva);
        if (!ok) {
            responderError(response, 401, "La contrasena actual no es correcta");
            return;
        }
        responderJson(response, Map.of("ok", true));
    }
}
