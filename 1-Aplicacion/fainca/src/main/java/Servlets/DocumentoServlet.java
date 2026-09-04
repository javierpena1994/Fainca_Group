package Servlets;

import Dao.MovimientoDAO;
import Objetos.Movimiento;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detalle de UN movimiento con todos sus productos: ?lote=xxx (documentos nuevos)
 * o ?id=123 (movimientos sueltos anteriores al agrupado).
 *
 * Es la fuente autoritativa del detalle: la pantalla lo consulta al desplegar un
 * documento, en vez de fiarse de las filas que alcanzo a cargar la pagina.
 */
@WebServlet("/DocumentoServlet")
public class DocumentoServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String lote = request.getParameter("lote");
        String id = request.getParameter("id");

        List<Movimiento> lineas;
        if (lote != null && !lote.isBlank()) {
            lineas = new MovimientoDAO().porLote(lote.trim());
        } else if (id != null && id.matches("\\d{1,9}")) {
            lineas = new MovimientoDAO().porId(Integer.parseInt(id));
        } else {
            responderError(response, 400, "Indique lote o id");
            return;
        }

        if (lineas.isEmpty()) {
            responderError(response, 404, "El movimiento no existe");
            return;
        }

        Movimiento cab = lineas.get(0);
        long unidades = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        for (Movimiento l : lineas) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("codigo", l.getProductoCodigo());
            m.put("marca", l.getMarca());
            m.put("descripcion", l.getDescripcion());
            m.put("cantidad", l.getCantidad());
            m.put("stock_resultante", l.getStockResultante());
            m.put("unidad_medida", l.getUnidadMedida());
            m.put("ubicacion", l.getUbicacion());
            m.put("imagen", l.getImagen());
            items.add(m);
            unidades += Math.abs(l.getCantidad());
        }

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("lote", cab.getLote());
        salida.put("numero", numeroDocumento(cab));
        salida.put("tipo", cab.getTipo());
        salida.put("fecha", cab.getFecha());
        salida.put("usuario", cab.getUsuario());
        salida.put("observaciones", cab.getObservaciones());
        salida.put("items", items);
        salida.put("total_items", items.size());
        salida.put("total_unidades", unidades);
        responderJson(response, salida);
    }

    /**
     * Numero visible del documento: prefijo por tipo y el id de su primera linea,
     * que es unico y estable. Ej: ING-000042.
     */
    static String numeroDocumento(Movimiento cab) {
        String prefijo = switch (cab.getTipo() == null ? "" : cab.getTipo()) {
            case "ingreso" -> "ING";
            case "egreso" -> "SAL";
            case "ajuste" -> "AJU";
            default -> "MOV";
        };
        return prefijo + "-" + String.format("%06d", cab.getId());
    }
}
