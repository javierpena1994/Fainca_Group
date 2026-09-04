package Servlets;

import Dao.ProductoDAO;
import Objetos.Producto;
import Util.ImagenUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Corrige el codigo (PK/FK) de un producto que quedo mal cargado -- por ejemplo,
 * una celda vacia o con un caracter suelto en el Excel de origen. Arrastra el
 * historial de movimientos al codigo nuevo. Operacion delicada y poco frecuente:
 * solo admin/superadmin (el AuthFilter ya lo garantiza, no esta en la lista de ventas).
 */
@WebServlet("/RenombrarCodigoServlet")
public class RenombrarCodigoServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String codigoActual = texto(json, "codigo_actual");
        String codigoNuevo = texto(json, "codigo_nuevo");

        if (codigoActual == null || codigoActual.isBlank() || codigoNuevo == null || codigoNuevo.isBlank()) {
            responderError(response, 400, "codigo_actual y codigo_nuevo son requeridos");
            return;
        }
        if (codigoActual.equals(codigoNuevo)) {
            responderError(response, 400, "El código nuevo debe ser distinto al actual");
            return;
        }

        try {
            ProductoDAO dao = new ProductoDAO();
            Producto anterior = dao.obtener(codigoActual);
            dao.renombrarCodigo(codigoActual, codigoNuevo);

            // La fila copio el nombre de archivo viejo tal cual; si el codigo cambia de
            // caracteres, el nombre de archivo "esperado" tambien cambia (ImagenUtil lo deriva
            // del codigo). Se renombra en disco para que ambos queden sincronizados y no se
            // arriesgue una colision futura si algun otro codigo sanitiza al mismo nombre viejo.
            if (anterior != null && anterior.getImagen() != null) {
                String carpeta = ImagenUtil.carpetaConfigurada();
                String nombreNuevo = ImagenUtil.nombreArchivo(codigoNuevo);
                if (!nombreNuevo.equals(anterior.getImagen())) {
                    File archivoViejo = new File(carpeta, anterior.getImagen());
                    File archivoNuevo = new File(carpeta, nombreNuevo);
                    if (archivoViejo.exists() && archivoViejo.renameTo(archivoNuevo)) {
                        dao.actualizarImagen(codigoNuevo, nombreNuevo);
                    }
                }
            }

            responderJson(response, Map.of("ok", true, "codigo", codigoNuevo));
        } catch (IllegalArgumentException e) {
            responderError(response, 400, e.getMessage());
        }
    }
}
