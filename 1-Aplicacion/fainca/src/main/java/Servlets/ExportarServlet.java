package Servlets;

import Dao.MarcaDAO;
import Dao.ProductoDAO;
import Objetos.Producto;
import Reportes.ReporteExcel;
import Reportes.ReportePdf;
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
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exporta el inventario a Excel o PDF. Parametros:
 *   marca   = id de la marca, o "todas"
 *   formato = "excel" | "pdf"
 *
 * Solo administradores (el AuthFilter ya bloquea al rol ventas). Se responde como
 * descarga de archivo (Content-Disposition: attachment).
 */
@WebServlet("/ExportarServlet")
public class ExportarServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String formato = request.getParameter("formato");
        String marcaParam = request.getParameter("marca");
        if (formato == null || (!formato.equals("excel") && !formato.equals("pdf"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato inválido (usa excel o pdf)");
            return;
        }

        Integer marcaId = null;
        String tituloMarca = "Todas las marcas";
        if (marcaParam != null && !marcaParam.isBlank() && !marcaParam.equals("todas")) {
            try {
                marcaId = Integer.parseInt(marcaParam);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Marca inválida");
                return;
            }
            String nombre = new MarcaDAO().nombrePorId(marcaId);
            if (nombre == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "La marca no existe");
                return;
            }
            tituloMarca = nombre;
        }

        boolean incluirDescripcion = "si".equals(request.getParameter("descripcion"));
        boolean incluirFoto = "si".equals(request.getParameter("foto"));

        List<Producto> productos = new ProductoDAO().paraReporte(marcaId);
        // Si se piden fotos, se generan las miniaturas cuadradas de una vez (en memoria).
        Map<String, byte[]> miniaturas = incluirFoto ? construirMiniaturas(productos) : null;

        byte[] logo = leerLogo();
        LocalDateTime ahora = LocalDateTime.now();

        String fechaArchivo = ahora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String marcaArchivo = tituloMarca.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String extension = formato.equals("excel") ? "xlsx" : "pdf";
        String sufijo = (incluirDescripcion ? "_detallado" : "") + (incluirFoto ? "_con-fotos" : "");
        String nombreArchivo = "Inventario_FAINCA_" + marcaArchivo + sufijo + "_" + fechaArchivo + "." + extension;

        response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
        response.setContentType(formato.equals("excel")
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf");

        try (OutputStream out = response.getOutputStream()) {
            if (formato.equals("excel")) {
                ReporteExcel.generar(out, tituloMarca, productos, logo, ahora, incluirDescripcion, miniaturas);
            } else {
                ReportePdf.generar(out, tituloMarca, productos, logo, ahora, incluirDescripcion, miniaturas);
            }
        } catch (Exception e) {
            throw new ServletException("Error generando el reporte", e);
        }
    }

    /** Miniatura cuadrada (120px) por producto que tenga foto. Los que no la tengan no entran al mapa. */
    private Map<String, byte[]> construirMiniaturas(List<Producto> productos) {
        Map<String, byte[]> mapa = new LinkedHashMap<>();
        String carpeta = ImagenUtil.carpetaConfigurada();
        for (Producto p : productos) {
            if (p.getImagen() == null || p.getImagen().isBlank()) continue;
            byte[] mini = ImagenUtil.miniaturaCuadrada(new File(carpeta, p.getImagen()), 120);
            if (mini != null) mapa.put(p.getCodigo(), mini);
        }
        return mapa;
    }

    private byte[] leerLogo() {
        try (InputStream in = getServletContext().getResourceAsStream("/images/logo-fainca.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null; // el reporte se genera igual, solo sin logo
        }
    }
}
