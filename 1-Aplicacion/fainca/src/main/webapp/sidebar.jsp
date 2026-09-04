<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Objetos.Usuario" %>
<%
    Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");
    boolean esAdminSidebar = usuarioSesion != null && usuarioSesion.esAdmin();
    boolean esSuperAdminSidebar = usuarioSesion != null && usuarioSesion.esSuperAdmin();
    String paginaActiva = request.getParameter("activo") == null ? "" : request.getParameter("activo");
%>
<nav class="sidebar">
    <div class="logo-area">
        <div class="titulo-app">
            <h2>FAINCA GROUP</h2>
            <span>Inventario &mdash; Bodega #1</span>
        </div>
        <button type="button" class="menu-toggle" onclick="toggleMenu()" aria-label="Abrir menú">
            <i class="fas fa-bars"></i>
        </button>
    </div>

    <div class="menu-links">
        <div class="menu-group">
            <%-- Acceso a cada bodega. La de herramientas tiene su propio menu
                 (sidebar-herramientas.jsp): son dos inventarios independientes. --%>
            <div class="menu-accesos">
                <a href="<%= esAdminSidebar ? "dashboard.jsp" : "index.jsp" %>"
                   class="menu-home <%= "panel".equals(paginaActiva) ? "activo" : "" %>"
                   title="<%= esAdminSidebar ? "Inicio — Panel de control" : "Inicio" %>">
                    <i class="fas fa-house"></i>
                </a>
                <% if (esAdminSidebar) { %>
                <a href="herramientas.jsp" class="menu-home" title="Ir a la Bodega de herramientas">
                    <i class="fas fa-toolbox"></i>
                </a>
                <% } %>
            </div>

            <a href="index.jsp" class="menu-item <%= "index".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-search"></i> Buscar productos
            </a>
            
            <% if (esAdminSidebar) { %>
            <a href="ingresoInventario.jsp" class="menu-item <%= "ingreso".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-arrow-down"></i> Ingreso
            </a>
            <a href="salidaInventario.jsp" class="menu-item <%= "salida".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-arrow-up"></i> Salida
            </a>
            <a href="ajusteInventario.jsp" class="menu-item <%= "ajuste".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-scale-balanced"></i> Ajuste
            </a>
            <a href="historial.jsp" class="menu-item <%= "historial".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-history"></i> Historial
            </a>
            <a href="registrarProductos.jsp" class="menu-item <%= "registrar".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-box-open"></i> Registrar productos
            </a>
            <a href="eliminarProductos.jsp" class="menu-item <%= "eliminar".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-trash-alt"></i> Eliminar productos
            </a>
            <% } %>
            <% if (esSuperAdminSidebar) { %>
            <a href="usuarios.jsp" class="menu-item <%= "usuarios".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-users-cog"></i> Gestionar usuarios
            </a>
            <% } %>
            <a href="reportes.jsp" class="menu-item <%= "reportes".equals(paginaActiva) ? "activo" : "" %>">
                <i class="fas fa-file-export"></i> Reportes
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
