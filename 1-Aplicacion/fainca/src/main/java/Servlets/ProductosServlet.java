package Servlets;

import Dao.ProductoDAO;
import Objetos.Producto;
import Objetos.Usuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GET sin parametros: listado completo del inventario (JSON).
 * GET ?codigo=XXX: un solo producto (JSON).
 * El admin puede pedir tambien los inactivos con ?incluir_inactivos=1.
 */
@WebServlet("/ProductosServlet")
public class ProductosServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario u = usuarioActual(request);
        ProductoDAO dao = new ProductoDAO();

        String codigo = request.getParameter("codigo");
        if (codigo != null && !codigo.isBlank()) {
            Producto p = dao.obtener(codigo.trim());
            if (p == null) {
                responderError(response, 404, "Producto no encontrado");
                return;
            }
            responderJson(response, BuscarProductosServlet.comoMapa(p, u.esAdmin()));
            return;
        }

        boolean incluirInactivos = u.esAdmin() && "1".equals(request.getParameter("incluir_inactivos"));
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Producto p : dao.listar(incluirInactivos)) {
            salida.add(BuscarProductosServlet.comoMapa(p, u.esAdmin()));
        }
        responderJson(response, salida);
    }
}
