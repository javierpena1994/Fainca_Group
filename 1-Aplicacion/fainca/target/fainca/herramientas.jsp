<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Bodega de Herramientas - FAINCA</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar-herramientas.jsp">
    <jsp:param name="activo" value="herramientas"/>
</jsp:include>

<main class="main-content">
    <div class="table-container">
        <div class="table-header-box">
            <h2 class="page-title"><i class="fas fa-toolbox" style="color: #f1c40f;"></i> Bodega de Herramientas</h2>
            <button type="button" class="btn-add-row" id="btn-nueva" style="margin:0;">
                <i class="fas fa-plus"></i> Registrar herramienta
            </button>
        </div>

        <form class="form-grid" style="grid-template-columns: 2fr 1fr 1fr; gap:12px; margin-bottom:10px;"
              onsubmit="return false;">
            <div class="form-group">
                <label><i class="fas fa-search"></i> Buscar</label>
                <input type="text" id="h-buscar" placeholder="Nombre u observación" autocomplete="off">
            </div>
            <div class="form-group">
                <label><i class="fas fa-tag"></i> Tipo</label>
                <select id="h-tipo">
                    <option value="">Todos</option>
                    <option value="herramienta">Herramienta (se presta)</option>
                    <option value="consumible">Consumible (se gasta)</option>
                </select>
            </div>
            <div class="form-group">
                <label><i class="fas fa-cart-plus"></i> Vista</label>
                <select id="h-reposicion">
                    <option value="">Todo el catálogo</option>
                    <option value="1">Solo lo que hay que reponer</option>
                </select>
            </div>
        </form>

        <table class="user-table">
            <thead>
                <tr>
                    <th>Herramienta</th>
                    <th style="text-align:center;">Tipo</th>
                    <th style="text-align:center;">Disponible</th>
                    <th style="text-align:center;">En proyectos</th>
                    <th style="text-align:center;">Dañadas</th>
                    <th style="text-align:center;">Total</th>
                    <th>Observaciones</th>
                    <th style="text-align:center;">Acciones</th>
                </tr>
            </thead>
            <tbody id="tbodyHerramientas"></tbody>
        </table>

        <div id="pie-herramientas" style="margin-top:14px; color:#777;"></div>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/herramientas.js?v=32"></script>
</body>
</html>
