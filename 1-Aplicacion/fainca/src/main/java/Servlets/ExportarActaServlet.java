package Servlets;

import Dao.ActaHerramientaDAO;
import Objetos.ActaHerramienta;
import Objetos.Usuario;
import Reportes.ActaHerramientasPdf;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;

/**
 * Descarga en PDF el acta de entrega de herramientas (para imprimir y firmar).
 * GET ?id=N
 */
@WebServlet("/ExportarActaServlet")
public class ExportarActaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        if (id == null || !id.matches("\\d{1,9}")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Indique el id del acta");
            return;
        }
        ActaHerramienta acta = new ActaHerramientaDAO().detalle(Integer.parseInt(id));
        if (acta == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "El acta no existe");
            return;
        }

        Usuario u = (Usuario) request.getSession().getAttribute("usuario");
        String generadoPor = u == null ? "—" : u.getNombre();

        response.setHeader("Content-Disposition",
                "attachment; filename=\"Acta_" + acta.getNumero() + ".pdf\"");
        response.setContentType("application/pdf");

        try (OutputStream out = response.getOutputStream()) {
            ActaHerramientasPdf.generar(out, acta, leerLogo(), LocalDateTime.now(), generadoPor);
        } catch (Exception e) {
            throw new ServletException("Error generando el acta en PDF", e);
        }
    }

    private byte[] leerLogo() {
        try (InputStream in = getServletContext().getResourceAsStream("/images/logo-fainca.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null; // el acta se genera igual, solo sin logo
        }
    }
}
