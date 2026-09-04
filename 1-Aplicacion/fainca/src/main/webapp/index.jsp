<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Objetos.Usuario, Objetos.Marca, Dao.MarcaDAO, java.util.List" %>
<%
    Usuario usuarioActual = (Usuario) session.getAttribute("usuario");
    boolean esAdminPagina = usuarioActual != null && usuarioActual.esAdmin();
    List<Marca> marcas = esAdminPagina ? new MarcaDAO().listar() : List.of();
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Buscar Productos - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body data-rol="<%= usuarioActual == null ? "" : usuarioActual.getRol() %>">

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="index"/>
</jsp:include>

<main class="main-content">
    <div class="table-container">
        <div class="table-header-box">
            <h2 class="page-title"><i class="fas fa-box-open" style="color: #f1c40f;"></i> Inventario de Productos</h2>
            <div class="search-box">
                <i class="fas fa-search"></i>
                <input type="text" id="searchInput" autocomplete="off" placeholder="Buscar código, marca o característica...">
            </div>
        </div>

        <% if (esAdminPagina) { %>
        <!-- Los productos dados de baja no se ven salvo que se pidan a proposito.
             Sin esta casilla no habria forma de encontrarlos para reactivarlos. -->
        <label class="check-eliminados">
            <input type="checkbox" id="ver-eliminados">
            <i class="fas fa-trash-can-arrow-up"></i> Ver también los productos eliminados
        </label>
        <% } %>

        <table class="user-table">
            <thead>
                <tr>
                    <th>Código</th>
                    <th>Marca</th>
                    <th>Característica</th>
                    <th>Ubicación</th>
                    <th style="text-align: center;">Cantidad</th>
                    <th style="text-align: center;">Acción</th>
                </tr>
            </thead>
            <tbody id="userTbody"></tbody>
        </table>
    </div>

    <% if (esAdminPagina) { %>
    <!-- Panel de edicion (se abre con el boton de lapiz) -->
    <div class="form-container" id="card-editar" style="display:none; margin-top:25px;">
        <div class="header-modulo">
            <h2><i class="fas fa-pen" style="color: #f1c40f;"></i> Editar producto <span id="editar-codigo" style="color:#777; font-weight:normal;"></span>
                <button type="button" class="btn-detalles" id="btn-editar-codigo" title="Corregir código" style="margin-left:6px; vertical-align:middle;"><i class="fas fa-i-cursor"></i></button>
            </h2>
        </div>
        <form id="form-editar">
            <div class="form-grid">
                <div class="form-group">
                    <label><i class="fas fa-industry"></i> Marca</label>
                    <select id="e-marca" required>
                        <% for (Marca m : marcas) { %>
                        <option value="<%= m.getId() %>"><%= m.getNombre() %></option>
                        <% } %>
                    </select>
                </div>
                <div class="form-group">
                    <label><i class="fas fa-balance-scale"></i> Unidad de medida</label>
                    <input id="e-unidad" type="text" required>
                </div>
                <div class="form-group">
                    <label><i class="fas fa-map-marker-alt"></i> Ubicación (Percha)</label>
                    <input id="e-ubicacion" type="text">
                </div>
                <div class="form-group full-width">
                    <label><i class="fas fa-align-left"></i> Característica / Descripción</label>
                    <textarea id="e-descripcion"></textarea>
                </div>
                <!-- Solo se muestra cuando el producto es una maleta-kit (BAN-TC..). -->
                <div class="form-group full-width" id="grupo-nota-maletas" style="display:none;">
                    <label><i class="fas fa-briefcase"></i> Contenido de la maleta</label>
                    <textarea id="e-nota-maletas" class="textarea-obs"
                              placeholder="Lista de las piezas que trae el kit y su cantidad. Ej: BAN-201 = 5, BAN-F01 = 2..."></textarea>
                    <small style="color:#999; margin-top:4px; display:block;">
                        <i class="fas fa-info-circle"></i>
                        Anota aquí qué piezas hay en las maletas y en qué cantidad. Cada cambio queda registrado en el historial.
                    </small>
                </div>
                <div class="form-group full-width">
                    <label><i class="fas fa-image"></i> Foto de referencia</label>
                    <div class="zona-imagen" id="zona-e-imagen">
                        <input type="file" id="e-imagen" accept="image/*" hidden>
                        <div class="zona-vacia">
                            <i class="fas fa-cloud-arrow-up"></i>
                            <p><strong>Arrastra la foto aquí</strong> o pégala con <kbd>Ctrl</kbd>+<kbd>V</kbd></p>
                            <span>también puedes hacer clic para buscarla en el equipo</span>
                        </div>
                        <img class="zona-preview" alt="Foto del producto" hidden>
                        <button type="button" class="zona-quitar" title="Quitar la foto" hidden>
                            <i class="fas fa-xmark"></i>
                        </button>
                        <small class="zona-nombre" hidden></small>
                    </div>
                </div>
                <div class="form-group">
                    <label><i class="fas fa-toggle-on"></i> Estado</label>
                    <select id="e-activo">
                        <option value="1">Activo</option>
                        <option value="0">Inactivo (dado de baja)</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label><i class="fas fa-comment-alt"></i> Observaciones (opcional)</label>
                    <textarea id="e-observaciones" class="textarea-obs"
                              placeholder="Ej: Se corrigió el código según la factura del proveedor / Cambio de percha por reorganización de bodega..."></textarea>
                    <small style="color:#999; margin-top:4px; display:block;">
                        <i class="fas fa-info-circle"></i>
                        Toda edición queda registrada en el historial con tu nombre, la fecha y lo que cambiaste.
                    </small>
                </div>
            </div>
            <div class="actions-form">
                <button class="btn-registrar" type="submit"><i class="fas fa-save"></i> Guardar cambios</button>
                <button class="btn-secundario" type="button" onclick="cerrarEdicion()">Cancelar</button>
            </div>
        </form>
    </div>
    <% } %>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/index.js?v=32"></script>
</body>
</html>
