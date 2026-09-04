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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Busqueda en vivo (digito por digito) para las pantallas de busqueda y el autocompletado. */
@WebServlet("/BuscarProductosServlet")
public class BuscarProductosServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String q = request.getParameter("q");
        if (q == null || q.isBlank()) {
            responderJson(response, List.of());
            return;
        }

        Usuario u = usuarioActual(request);
        // Solo un admin puede pedir ver los productos dados de baja (para reactivarlos).
        boolean incluirInactivos = u.esAdmin() && "1".equals(request.getParameter("incluir_inactivos"));

        List<Map<String, Object>> salida = new ArrayList<>();
        for (Producto p : new ProductoDAO().buscar(q.trim(), incluirInactivos)) {
            salida.add(comoMapa(p, u.esAdmin()));
        }
        responderJson(response, salida);
    }

    /** El rol ventas recibe solo los campos de consulta; el admin, todo. */
    static Map<String, Object> comoMapa(Producto p, boolean esAdmin) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", p.getCodigo());
        m.put("marca", p.getMarca());
        m.put("descripcion", p.getDescripcion());
        m.put("ubicacion", p.getUbicacion());
        m.put("unidad_medida", p.getUnidadMedida());
        m.put("stock_actual", p.getStockActual());
        m.put("imagen", p.getImagen());
        if (esAdmin) {
            m.put("marca_id", p.getMarcaId());
            m.put("activo", p.isActivo() ? 1 : 0);
            // Nota de contenido de la maleta (texto libre); detalle interno de bodega.
            // A ventas/contabilidad no les aporta, por eso no se les manda.
            m.put("nota_maletas", p.getNotaMaletas());
        }
        return m;
    }
}
