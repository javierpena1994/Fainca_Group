package Servlets;

import Dao.UsuarioDAO;
import Objetos.Usuario;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Edita un usuario (nombre, usuario, rol, estado) y opcionalmente restablece
 * su contrasena si viene el campo "password". Solo superadmin.
 */
@WebServlet("/EditarUsuarioServlet")
public class EditarUsuarioServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        int id = entero(json, "id", 0);
        String nombre = texto(json, "nombre");
        String usuario = texto(json, "usuario");
        String rol = texto(json, "rol");
        int activo = entero(json, "activo", 1);
        String passwordNueva = texto(json, "password"); // opcional

        if (id <= 0 || nombre == null || usuario == null || !UsuariosServlet.ROLES.contains(rol)
                || nombre.isBlank() || usuario.isBlank()) {
            responderError(response, 400, "Datos de usuario invalidos");
            return;
        }

        Usuario actual = usuarioActual(request);
        UsuarioDAO dao = new UsuarioDAO();
        boolean quedaraActivo = activo == 1;

        // Salvaguardas: no permitir que el superadmin se bloquee a si mismo
        // ni que se elimine al ultimo superadmin del sistema.
        boolean eraSuperadmin = "superadmin".equals(dao.rolDe(id));
        boolean dejaraDeSerSuperadmin = eraSuperadmin && (!"superadmin".equals(rol) || !quedaraActivo);
        if (dejaraDeSerSuperadmin && dao.contarSuperadminsActivos() <= 1) {
            responderError(response, 400, "No puedes quitar al ultimo Administrador del sistema");
            return;
        }
        if (id == actual.getId() && (!quedaraActivo || !"superadmin".equals(rol))) {
            responderError(response, 400, "No puedes cambiar tu propio rol ni desactivar tu propia cuenta");
            return;
        }

        try {
            dao.editar(id, nombre, usuario, rol, quedaraActivo);
            if (passwordNueva != null && !passwordNueva.isBlank()) {
                if (passwordNueva.length() < 6) {
                    responderError(response, 400, "La contrasena debe tener al menos 6 caracteres");
                    return;
                }
                dao.resetearPassword(id, passwordNueva);
            }
            responderJson(response, Map.of("ok", true));
        } catch (RuntimeException e) {
            responderError(response, 400, "No se pudo editar (¿el nombre de usuario ya existe?)");
        }
    }
}
