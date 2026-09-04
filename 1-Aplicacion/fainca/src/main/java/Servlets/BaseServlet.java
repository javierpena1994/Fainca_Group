package Servlets;

import Objetos.Usuario;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Utilidades comunes: leer/escribir JSON y obtener el usuario de la sesion. */
public abstract class BaseServlet extends HttpServlet {

    protected static final Gson GSON = new Gson();

    protected JsonObject leerJson(HttpServletRequest request) throws IOException {
        return GSON.fromJson(request.getReader(), JsonObject.class);
    }

    protected void responderJson(HttpServletResponse response, Object cuerpo) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(cuerpo));
    }

    protected void responderError(HttpServletResponse response, int codigo, String mensaje) throws IOException {
        response.setStatus(codigo);
        JsonObject error = new JsonObject();
        error.addProperty("error", mensaje);
        responderJson(response, error);
    }

    protected Usuario usuarioActual(HttpServletRequest request) {
        return (Usuario) request.getSession().getAttribute("usuario");
    }

    /** Texto de un campo JSON; null si no viene o es JSON null. */
    protected String texto(JsonObject json, String campo) {
        return (json != null && json.has(campo) && !json.get(campo).isJsonNull())
                ? json.get(campo).getAsString().trim()
                : null;
    }

    protected int entero(JsonObject json, String campo, int porDefecto) {
        return (json != null && json.has(campo) && !json.get(campo).isJsonNull())
                ? json.get(campo).getAsInt()
                : porDefecto;
    }
}
