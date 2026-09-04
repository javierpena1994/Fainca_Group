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
 * "Elimina" un usuario = baja logica (activo = 0), para conservar el historial
 * de movimientos que registro. Solo superadmin.
 */
@WebServlet("/EliminarUsuarioServlet")
public class EliminarUsuarioServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        int id = entero(json, "id", 0);
        if (id <= 0) {
            responderError(response, 400, "id requerido");
            return;
        }

        Usuario actual = usuarioActual(request);
        if (id == actual.getId()) {
            responderError(response, 400, "No puedes eliminar tu propia cuenta");
            return;
        }

        UsuarioDAO dao = new UsuarioDAO();
        if ("superadmin".equals(dao.rolDe(id)) && dao.contarSuperadminsActivos() <= 1) {
            responderError(response, 400, "No puedes eliminar al ultimo Administrador del sistema");
            return;
        }

        dao.desactivar(id);
        responderJson(response, Map.of("ok", true));
    }
}
