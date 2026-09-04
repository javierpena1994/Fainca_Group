package Servlets;

import Dao.MovimientoDAO;
import Objetos.Movimiento;
import Objetos.Usuario;
import Reportes.ComprobanteMovimiento;
import Util.ImagenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exporta a PDF el comprobante de un movimiento completo.
 *
 * Parametros:
 *   lote  = identificador del documento (o id = movimiento suelto antiguo)
 *   fotos = "si" para incluir la foto de cada producto; por defecto NO se incluyen,
 *           para que el archivo quede liviano.
 */
@WebServlet("/ExportarMovimientoServlet")
public class ExportarMovimientoServlet extends HttpServlet {

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
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Indique lote o id");
            return;
        }
        if (lineas.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "El movimiento no existe");
            return;
        }

        boolean conFotos = "si".equals(request.getParameter("fotos"));
        Map<String, byte[]> miniaturas = conFotos ? construirMiniaturas(lineas) : null;

        Movimiento cab = lineas.get(0);
        String numero = DocumentoServlet.numeroDocumento(cab);

        Usuario u = (Usuario) request.getSession().getAttribute("usuario");
        String generadoPor = u == null ? "—" : u.getNombre();

        String nombreArchivo = "Comprobante_" + numero + (conFotos ? "_con-fotos" : "") + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
        response.setContentType("application/pdf");

        try (OutputStream out = response.getOutputStream()) {
            ComprobanteMovimiento.generar(out, lineas, numero, leerLogo(),
                    LocalDateTime.now(), generadoPor, miniaturas);
        } catch (Exception e) {
            throw new ServletException("Error generando el comprobante", e);
        }
    }

    /** Una miniatura por producto que tenga foto; los que no la tengan no entran al mapa. */
    private Map<String, byte[]> construirMiniaturas(List<Movimiento> lineas) {
        Map<String, byte[]> mapa = new LinkedHashMap<>();
        String carpeta = ImagenUtil.carpetaConfigurada();
        for (Movimiento l : lineas) {
            if (l.getImagen() == null || l.getImagen().isBlank()) continue;
            if (mapa.containsKey(l.getProductoCodigo())) continue;
            byte[] mini = ImagenUtil.miniaturaCuadrada(new File(carpeta, l.getImagen()), 110);
            if (mini != null) mapa.put(l.getProductoCodigo(), mini);
        }
        return mapa;
    }

    private byte[] leerLogo() {
        try (InputStream in = getServletContext().getResourceAsStream("/images/logo-fainca.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null; // el comprobante se genera igual, solo sin logo
        }
    }
}
