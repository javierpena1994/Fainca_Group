package Servlets;

import Dao.MovimientoDAO;
import Dao.ProductoDAO;
import Objetos.Producto;
import Objetos.Usuario;
import Util.ImagenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * GET  ?archivo=xxx.jpg          -> sirve la foto de referencia desde disco (con cache).
 * POST multipart (codigo + imagen) -> comprime, guarda en disco y la asocia al producto. Solo admin.
 */
@WebServlet("/ImagenServlet")
@MultipartConfig(maxFileSize = 15L * 1024 * 1024) // 15 MB de entrada; se comprime a ~80-150 KB en disco
public class ImagenServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String archivo = request.getParameter("archivo");
        if (archivo == null || archivo.isBlank() || archivo.contains("..")
                || archivo.contains("/") || archivo.contains("\\")) {
            responderError(response, 400, "Archivo invalido");
            return;
        }

        File file = new File(ImagenUtil.carpetaConfigurada(), archivo);
        if (!file.exists()) {
            responderError(response, 404, "Imagen no encontrada");
            return;
        }

        long ultimaModificacion = file.lastModified();
        long desde = request.getDateHeader("If-Modified-Since");
        if (desde != -1 && ultimaModificacion / 1000 <= desde / 1000) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setContentType("image/jpeg");
        response.setDateHeader("Last-Modified", ultimaModificacion);
        response.setHeader("Cache-Control", "public, max-age=86400");
        try (InputStream in = new FileInputStream(file)) {
            in.transferTo(response.getOutputStream());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario u = usuarioActual(request);
        if (u == null || !u.esAdmin()) {
            responderError(response, 403, "Requiere permisos de administrador");
            return;
        }

        String codigo = request.getParameter("codigo");
        if (codigo == null || codigo.isBlank()) {
            responderError(response, 400, "codigo es requerido");
            return;
        }
        codigo = codigo.trim();

        ProductoDAO dao = new ProductoDAO();
        Producto p = dao.obtener(codigo);
        if (p == null) {
            responderError(response, 404, "Producto no encontrado");
            return;
        }

        Part parte = request.getPart("imagen");
        if (parte == null || parte.getSize() == 0) {
            responderError(response, 400, "No se recibio ninguna imagen");
            return;
        }

        try (InputStream entrada = parte.getInputStream()) {
            String archivo = ImagenUtil.comprimirYGuardar(entrada, codigo, ImagenUtil.carpetaConfigurada());
            boolean teniaFoto = p.getImagen() != null;
            dao.actualizarImagen(codigo, archivo);

            // Cambiar la foto de un producto ya existente es una edicion mas y debe quedar
            // en el historial. Se omite cuando la foto llega junto con el alta del producto
            // (registrar.js manda nuevo=1), para no duplicar el registro del ingreso inicial.
            boolean esAlta = "1".equals(request.getParameter("nuevo"));
            if (!esAlta) {
                new MovimientoDAO().registrarEdicion(codigo, p.getStockActual(),
                        teniaFoto ? "Foto de referencia reemplazada" : "Se agregó la foto de referencia",
                        u.getId());
            }

            responderJson(response, Map.of("ok", true, "imagen", archivo));
        } catch (IOException e) {
            responderError(response, 400, "No se pudo procesar la imagen (¿es un archivo de imagen valido?)");
        }
    }
}
