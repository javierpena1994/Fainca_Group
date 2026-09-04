package Servlets;

import Dao.UsuarioDAO;
import Objetos.Usuario;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gestion de usuarios (solo superadmin).
 * GET  -> lista de usuarios (sin contrasenas)
 * POST -> crear usuario nuevo
 */
@WebServlet("/UsuariosServlet")
public class UsuariosServlet extends BaseServlet {

    static final Set<String> ROLES = Set.of("superadmin", "admin", "ventas");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<Map<String, Object>> salida = new ArrayList<>();
        for (Usuario u : new UsuarioDAO().listar()) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("nombre", u.getNombre());
            m.put("usuario", u.getUsuario());
            m.put("rol", u.getRol());
            m.put("activo", u.isActivo() ? 1 : 0);
            m.put("es_actual", u.getId() == usuarioActual(request).getId());
            salida.add(m);
        }
        responderJson(response, salida);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String nombre = texto(json, "nombre");
        String usuario = texto(json, "usuario");
        String password = texto(json, "password");
        String rol = texto(json, "rol");

        if (nombre == null || usuario == null || password == null || rol == null
                || nombre.isBlank() || usuario.isBlank() || !ROLES.contains(rol)) {
            responderError(response, 400, "Nombre, usuario, contrasena y rol son requeridos");
            return;
        }
        if (password.length() < 6) {
            responderError(response, 400, "La contrasena debe tener al menos 6 caracteres");
            return;
        }

        try {
            int id = new UsuarioDAO().crear(nombre, usuario, password, rol);
            response.setStatus(201);
            responderJson(response, Map.of("id", id));
        } catch (RuntimeException e) {
            responderError(response, 400, "No se pudo crear (¿el nombre de usuario ya existe?)");
        }
    }
}
