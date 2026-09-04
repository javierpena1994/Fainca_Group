package Servlets;

import Dao.ProductoDAO;
import Objetos.Producto;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Edita los datos de un producto (el stock NO: ese solo cambia por movimientos).
 * Cada edicion queda registrada en el historial con el detalle de lo que cambio,
 * quien lo hizo y su observacion opcional -- sin importar el perfil, incluido el
 * Administrador.
 */
@WebServlet("/EditarProductoServlet")
public class EditarProductoServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String codigo = texto(json, "codigo");
        int marcaId = entero(json, "marca_id", 0);

        if (codigo == null || marcaId <= 0) {
            responderError(response, 400, "codigo y marca_id son requeridos");
            return;
        }

        ProductoDAO dao = new ProductoDAO();
        Producto p = dao.obtener(codigo);
        if (p == null) {
            responderError(response, 404, "Producto no encontrado");
            return;
        }

        p.setMarcaId(marcaId);
        p.setDescripcion(texto(json, "descripcion"));
        p.setUbicacion(texto(json, "ubicacion"));
        String unidad = texto(json, "unidad_medida");
        p.setUnidadMedida(unidad == null || unidad.isBlank() ? "UND." : unidad);
        p.setActivo(entero(json, "activo", 1) == 1);
        // Nota de texto libre con el contenido de la maleta-kit. Solo llega desde el
        // formulario cuando el producto es una maleta; si no viene, se conserva la actual.
        if (json.has("nota_maletas")) p.setNotaMaletas(texto(json, "nota_maletas"));

        String detalle;
        try {
            detalle = dao.actualizar(p, texto(json, "observaciones"), usuarioActual(request).getId());
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
            return;
        }

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("ok", true);
        salida.put("registrado", detalle != null); // false = no habia nada que cambiar
        if (detalle != null) salida.put("detalle", detalle);
        responderJson(response, salida);
    }
}
