package Servlets;

import Dao.MovimientoDAO;
import Dao.ProductoDAO;
import Objetos.Producto;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Registra un producto nuevo. El stock inicial (conteo fisico) entra como
 * movimiento de ingreso para que quede en el historial con fecha y usuario.
 */
@WebServlet("/RegistrarProductoServlet")
public class RegistrarProductoServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String codigo = texto(json, "codigo");
        int marcaId = entero(json, "marca_id", 0);

        if (codigo == null || codigo.isBlank() || marcaId <= 0) {
            responderError(response, 400, "codigo y marca_id son requeridos");
            return;
        }

        Producto p = new Producto(
                codigo,
                marcaId,
                texto(json, "descripcion"),
                texto(json, "ubicacion"),
                texto(json, "unidad_medida") == null ? "UND." : texto(json, "unidad_medida"),
                0);

        try {
            new ProductoDAO().registrar(p);
        } catch (IllegalArgumentException e) {
            // Choque con un codigo equivalente (guiones/espacios): el motivo exacto le sirve al usuario.
            responderError(response, 400, e.getMessage());
            return;
        } catch (RuntimeException e) {
            responderError(response, 400, "No se pudo registrar (¿código duplicado?)");
            return;
        }

        int stockInicial = entero(json, "stock_inicial", 0);
        int stockFinal = 0;
        if (stockInicial > 0) {
            // Sin fecha desde el cliente a proposito: todo registro queda en el
            // historial con la fecha y hora actuales, sin excepcion.
            stockFinal = new MovimientoDAO().registrar(
                    codigo, "ingreso", stockInicial,
                    "Carga inicial de conteo físico",
                    null,
                    usuarioActual(request).getId());
        }

        response.setStatus(201);
        responderJson(response, Map.of("codigo", codigo, "stock_actual", stockFinal));
    }
}
