<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Objetos.Usuario" %>
<%
    Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");
    boolean esAdminHistorial = usuarioSesion != null && usuarioSesion.esAdmin();
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Historial de Movimientos - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="historial"/>
</jsp:include>

<main class="main-content">
    <div class="table-container">
        <div class="table-header-box">
            <h2 class="page-title"><i class="fas fa-history" style="color: #f1c40f;"></i> Historial de Movimientos</h2>
        </div>

        <form id="filtros" class="form-grid" style="grid-template-columns: repeat(4, 1fr); gap:12px; margin-bottom:10px;">
            <div class="form-group">
                <label><i class="fas fa-search"></i> Buscar</label>
                <input type="text" id="f-codigo" placeholder="Código, marca u observación" autocomplete="off">
            </div>
            <div class="form-group">
                <label><i class="fas fa-exchange-alt"></i> Tipo</label>
                <select id="f-tipo">
                    <option value="">Todos</option>
                    <option value="ingreso">Ingreso</option>
                    <option value="egreso">Salida</option>
                    <option value="ajuste">Ajuste</option>
                    <option value="edicion">Edición de datos</option>
                    <option value="correccion">Corrección de observación</option>
                </select>
            </div>
            <div class="form-group">
                <label><i class="fas fa-calendar-minus"></i> Desde</label>
                <input type="date" id="f-desde">
            </div>
            <div class="form-group">
                <label><i class="fas fa-calendar-plus"></i> Hasta</label>
                <input type="date" id="f-hasta">
            </div>
        </form>

        <%-- Una fila por MOVIMIENTO (no por producto): los productos registrados en la
             misma operacion se agrupan y se despliegan al hacer clic. --%>
        <table class="user-table">
            <thead>
                <tr>
                    <th style="width:34px;"></th>
                    <th>Fecha</th>
                    <th style="text-align:center;">Tipo</th>
                    <th style="text-align:center;">Productos</th>
                    <th style="text-align:center;">Unidades</th>
                    <th>Usuario</th>
                    <th>Observaciones</th>
                    <% if (esAdminHistorial) { %>
                    <th style="text-align:center;">Acciones</th>
                    <% } %>
                </tr>
            </thead>
            <tbody id="tbodyHistorial"></tbody>
        </table>

        <%-- Cuenta de registros mostrados y boton "Cargar 1000 más" (lo llena historial.js) --%>
        <div id="pie-historial" style="display:flex; align-items:center; gap:14px;
                                       flex-wrap:wrap; margin-top:14px;"></div>
    </div>
</main>

<script>const ES_ADMIN_HISTORIAL = <%= esAdminHistorial %>;</script>
<script src="js/app.js?v=32"></script>
<script src="js/historial.js?v=32"></script>
</body>
</html>
