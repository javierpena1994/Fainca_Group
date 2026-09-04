package Filtros;

import Objetos.Usuario;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Protege toda la aplicacion: sin sesion iniciada solo se puede ver el login.
 * El rol "ventas" unicamente puede consultar productos y cambiar su contrasena;
 * el resto de pantallas y acciones son de administradores.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    // Rutas publicas (sin sesion)
    private static final Set<String> PUBLICAS = Set.of("/login.jsp", "/LoginServlet");

    // Rutas permitidas al rol ventas (ademas de las publicas)
    private static final Set<String> VENTAS = Set.of(
            "/", "/index.jsp", "/BuscarProductosServlet", "/ProductosServlet",
            "/cambiarPassword.jsp", "/CambiarPasswordServlet", "/LogoutServlet", "/ImagenServlet",
            "/reportes.jsp", "/ExportarServlet");

    // Rutas de gestion de usuarios: SOLO superadmin (ni siquiera un admin normal)
    private static final Set<String> SOLO_SUPERADMIN = Set.of(
            "/usuarios.jsp", "/UsuariosServlet", "/EditarUsuarioServlet", "/EliminarUsuarioServlet");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String ruta = req.getServletPath();

        // Nota: aqui iba la redireccion automatica a HTTPS. Se retiro junto con el
        // conector cifrado (ver pom.xml y config/LEEME-HTTPS.md). Si TI reactiva HTTPS,
        // hay que volver a habilitarla para que nadie entre por el canal sin cifrar.

        // Recursos estaticos y rutas publicas pasan directo
        if (ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/images/")
                || ruta.startsWith("/vendor/") || ruta.equals("/favicon.ico")
                || PUBLICAS.contains(ruta)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession sesion = req.getSession(false);
        Usuario usuario = (sesion == null) ? null : (Usuario) sesion.getAttribute("usuario");

        if (usuario == null) {
            if (esAjax(req)) {
                res.setStatus(401);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Sesion expirada\"}");
            } else {
                res.sendRedirect(req.getContextPath() + "/login.jsp");
            }
            return;
        }

        if (!usuario.esAdmin() && !VENTAS.contains(ruta)) {
            if (esAjax(req)) {
                res.setStatus(403);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Requiere permisos de administrador\"}");
            } else {
                res.sendRedirect(req.getContextPath() + "/index.jsp");
            }
            return;
        }

        // La gestion de usuarios es exclusiva del superadmin (Administrador)
        if (SOLO_SUPERADMIN.contains(ruta) && !usuario.esSuperAdmin()) {
            if (esAjax(req)) {
                res.setStatus(403);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Solo el Administrador puede gestionar usuarios\"}");
            } else {
                res.sendRedirect(req.getContextPath() + "/index.jsp");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean esAjax(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return req.getServletPath().endsWith("Servlet")
                && !"/LoginServlet".equals(req.getServletPath())
                || (accept != null && accept.contains("application/json"));
    }
}
