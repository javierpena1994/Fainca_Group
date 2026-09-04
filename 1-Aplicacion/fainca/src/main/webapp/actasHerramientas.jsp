<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Actas y Devoluciones - FAINCA</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar-herramientas.jsp">
    <jsp:param name="activo" value="actas"/>
</jsp:include>

<main class="main-content">
    <div class="table-container">
        <div class="table-header-box">
            <h2 class="page-title"><i class="fas fa-rotate-left" style="color: #f1c40f;"></i> Actas y Devoluciones</h2>
        </div>

        <form class="form-grid" style="grid-template-columns: repeat(4, 1fr); gap:12px; margin-bottom:10px;"
              onsubmit="return false;">
            <div class="form-group">
                <label><i class="fas fa-filter"></i> Estado</label>
                <select id="a-estado">
                    <option value="abierta">Abiertas (con cosas afuera)</option>
                    <option value="cerrada">Cerradas</option>
                    <option value="">Todas</option>
                </select>
            </div>
            <div class="form-group">
                <label><i class="fas fa-search"></i> Buscar</label>
                <input type="text" id="a-buscar" placeholder="N° de acta, solicitante, proyecto, destino..." autocomplete="off">
            </div>
            <div class="form-group">
                <label><i class="fas fa-calendar-minus"></i> Desde</label>
                <input type="date" id="a-desde">
            </div>
            <div class="form-group">
                <label><i class="fas fa-calendar-plus"></i> Hasta</label>
                <input type="date" id="a-hasta">
            </div>
        </form>

        <table class="user-table">
            <thead>
                <tr>
                    <th style="width:34px;"></th>
                    <th>Acta</th>
                    <th>Fecha</th>
                    <th>Solicitante</th>
                    <th>Proyecto</th>
                    <th style="text-align:center;">Ítems</th>
                    <th style="text-align:center;">Pendientes</th>
                    <th style="text-align:center;">Estado</th>
                    <th style="text-align:center;">Acciones</th>
                </tr>
            </thead>
            <tbody id="tbodyActas"></tbody>
        </table>

        <div id="pie-actas" style="margin-top:14px; color:#777;"></div>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/actas-herramientas.js?v=32"></script>
</body>
</html>
