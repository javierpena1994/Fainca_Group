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
 * Alta de un item en el catalogo de herramientas.
 * POST {nombre, tipo, cantidad, stock_minimo?, observaciones?}
 *
 * No hay codigo: el nombre identifica la herramienta ("TALADRO"), y la marca,
 * el estado o lo que le falte van en la observacion.
 */
@WebServlet("/RegistrarHerramientaServlet")
public class RegistrarHerramientaServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String nombre = texto(json, "nombre");
        String tipo = texto(json, "tipo");
        int cantidad = entero(json, "cantidad", 0);
        int minimo = entero(json, "stock_minimo", -1);
        String observaciones = texto(json, "observaciones");

        if (nombre == null || nombre.isBlank()) {
            responderError(response, 400, "El nombre es obligatorio");
            return;
        }
        if (tipo == null || !List.of("herramienta", "consumible").contains(tipo)) {
            responderError(response, 400, "El tipo debe ser 'herramienta' o 'consumible'");
            return;
        }
        if (cantidad < 0) {
            responderError(response, 400, "La cantidad inicial no puede ser negativa");
            return;
        }

        try {
            responderJson(response, new HerramientaDAO().crear(nombre, tipo, cantidad,
                    minimo >= 0 ? minimo : null, observaciones, usuarioActual(request).getId()));
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
        }
    }
}
