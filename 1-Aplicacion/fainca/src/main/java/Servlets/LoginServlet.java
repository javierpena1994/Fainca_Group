package Servlets;

import Dao.UsuarioDAO;
import Objetos.Usuario;
import Util.ControlAccesos;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/LoginServlet")
public class LoginServlet extends BaseServlet {

    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String usuario = request.getParameter("usuario");
        String password = request.getParameter("password");

        if (usuario == null || password == null || usuario.isBlank() || password.isBlank()) {
            response.sendRedirect("login.jsp?error=1");
            return;
        }
        usuario = usuario.trim();

        // 1) Cuenta bloqueada por demasiados intentos fallidos: ni se consulta la base.
        if (ControlAccesos.estaBloqueado(usuario)) {
            long minutos = ControlAccesos.minutosRestantes(usuario);
            LOG.warning("Intento de acceso a una cuenta bloqueada: '" + usuario
                    + "' desde " + request.getRemoteAddr());
            response.sendRedirect("login.jsp?bloqueado=" + minutos);
            return;
        }

        Usuario u = new UsuarioDAO().autenticar(usuario, password);

        // 2) Credenciales incorrectas: se anota el fallo y se avisa cuantos intentos quedan.
        if (u == null) {
            ControlAccesos.registrarFallo(usuario);
            LOG.warning("Acceso fallido para '" + usuario + "' desde " + request.getRemoteAddr());
            if (ControlAccesos.estaBloqueado(usuario)) {
                response.sendRedirect("login.jsp?bloqueado=" + ControlAccesos.minutosRestantes(usuario));
            } else {
                response.sendRedirect("login.jsp?error=1&quedan=" + ControlAccesos.intentosRestantes(usuario));
            }
            return;
        }

        // 3) Acceso correcto: se limpia el contador de fallos de esa cuenta.
        ControlAccesos.registrarExito(usuario);

        // Se descarta la sesion anterior y se crea una nueva. Asi, si alguien logro
        // fijarle a la victima un identificador de sesion conocido antes de entrar,
        // ese identificador queda invalidado al autenticarse (fijacion de sesion).
        HttpSession vieja = request.getSession(false);
        if (vieja != null) vieja.invalidate();

        HttpSession sesion = request.getSession(true);
        sesion.setAttribute("usuario", u);
        sesion.setMaxInactiveInterval(12 * 60 * 60); // 12 horas

        LOG.info("Acceso correcto: '" + usuario + "' (" + u.getRol() + ") desde " + request.getRemoteAddr());

        // Bodega y Administrador aterrizan en el panel de control; ventas, en la busqueda
        response.sendRedirect(u.esAdmin() ? "dashboard.jsp" : "index.jsp");
    }
}
