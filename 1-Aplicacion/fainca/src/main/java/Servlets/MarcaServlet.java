package Servlets;

import Dao.MarcaDAO;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/MarcaServlet")
public class MarcaServlet extends BaseServlet {

    /** Lista de marcas (para recargar el select despues de agregar una). */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        responderJson(response, new MarcaDAO().listar());
    }

    /** Crea una marca nueva. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String nombre = texto(json, "nombre");
        if (nombre == null || nombre.isBlank()) {
            responderError(response, 400, "Nombre requerido");
            return;
        }

        try {
            int id = new MarcaDAO().crear(nombre);
            response.setStatus(201);
            responderJson(response, Map.of("id", id, "nombre", nombre));
        } catch (RuntimeException e) {
            responderError(response, 400, "No se pudo crear la marca (¿ya existe?)");
        }
    }
}
