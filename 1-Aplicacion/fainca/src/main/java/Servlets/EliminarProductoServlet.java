package Servlets;

import Dao.ProductoDAO;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** Baja logica del producto: se oculta del inventario pero conserva su historial. */
@WebServlet("/EliminarProductoServlet")
public class EliminarProductoServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String codigo = texto(json, "codigo");
        if (codigo == null || codigo.isBlank()) {
            responderError(response, 400, "codigo requerido");
            return;
        }

        ProductoDAO dao = new ProductoDAO();
        if (dao.obtener(codigo) == null) {
            responderError(response, 404, "Producto no encontrado");
            return;
        }

        dao.darDeBaja(codigo);
        responderJson(response, Map.of("ok", true));
    }
}
