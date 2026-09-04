<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Eliminar Productos - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="eliminar"/>
</jsp:include>

<main class="main-content">
    <div class="table-container zona-peligro">
        <div class="table-header-box">
            <h2 class="page-title"><i class="fas fa-trash-alt" style="color: #dc3545;"></i> Eliminar Productos</h2>
            <div class="search-box">
                <i class="fas fa-search"></i>
                <input type="text" id="searchInput" autocomplete="off" placeholder="Buscar producto a eliminar...">
            </div>
        </div>

        <p style="color:#777; margin-top:0;">
            <i class="fas fa-info-circle"></i> El producto se da de baja (deja de aparecer en el inventario),
            pero su historial de movimientos se conserva. Para recuperarlo: en "Buscar productos" marca
            <strong>"Ver también los productos eliminados"</strong>, ábrelo con el lápiz y cambia su Estado a Activo.
        </p>

        <table class="user-table">
            <thead>
                <tr>
                    <th>Código</th>
                    <th>Marca</th>
                    <th>Característica</th>
                    <th>Ubicación</th>
                    <th style="text-align: center;">Cantidad</th>
                    <th style="text-align: center;">Eliminar</th>
                </tr>
            </thead>
            <tbody id="userTbody"></tbody>
        </table>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/eliminar.js?v=32"></script>
</body>
</html>
