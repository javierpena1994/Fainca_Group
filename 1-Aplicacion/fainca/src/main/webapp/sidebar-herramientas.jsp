<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Objetos.Usuario" %>
<%
    // Menu propio de la BODEGA DE HERRAMIENTAS. A proposito no muestra las opciones
    // de la Bodega #1 (productos): son dos inventarios independientes y mezclarlos
    // confunde a quien despacha. Para cambiar de bodega estan los botones de arriba.
    Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");
    boolean esAdminSidebar = usuarioSesion != null && usuarioSesion.esAdmin();
    String paginaActiva = request.getParameter("activo") == null ? "" : request.getParameter("activo");
%>
<nav class="sidebar">
    <div class="logo-area">
        <div class="titulo-app">
            <h2>FAINCA</h2>
            <span>Bodega de Herramientas</span>
        </div>
        <button type="button" class="menu-toggle" onclick="toggleMenu()" aria-label="Abrir menú">
            <i class="fas fa-bars"></i>
        </button>
    </div>

    <div class="menu-links">
        <div class="menu-group">
            <div class="menu-accesos">
                <a href="<%= esAdminSidebar ? "dashboard.jsp" : "index.jsp" %>" class="menu-home"
                   title="Ir a la Bodega #1 (productos)">
                    <i class="fas fa-house"></i>
                </a>
                <a href="herramientas.jsp" class="menu-home activo" title="Bodega de herramientas">
                    <i class="fas fa-toolbox"></i>
                </a>
            </div>

            <a href="herramientas.jsp" class="menu-item <%= "herramientas".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-toolbox"></i> Herramientas
            </a>
            <a href="ajusteHerramientas.jsp" class="menu-item <%= "ajusteHerr".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-scale-balanced"></i> Ajuste de herramientas
            </a>
            <a href="actaHerramientas.jsp" class="menu-item <%= "acta".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-file-signature"></i> Entregar (acta)
            </a>
            <a href="actasHerramientas.jsp" class="menu-item <%= "actas".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-rotate-left"></i> Actas y devoluciones
            </a>
            <a href="historialHerramientas.jsp" class="menu-item <%= "historialHerr".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-clock-rotate-left"></i> Historial
            </a>
        </div>
    </div>

    <div class="sidebar-footer">
        <span class="usuario-nombre">
            <i class="fas fa-user"></i>
            <%= usuarioSesion == null ? "" : usuarioSesion.getNombre() %> (<%= usuarioSesion == null ? "" : usuarioSesion.getRol() %>)
        </span>
        <a href="cambiarPassword.jsp"><i class="fas fa-key"></i> Cambiar contraseña</a>
        <a href="LogoutServlet"><i class="fas fa-sign-out-alt"></i> Salir</a>
    </div>
</nav>
