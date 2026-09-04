<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Historial de Herramientas - FAINCA</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar-herramientas.jsp">
    <jsp:param name="activo" value="historialHerr"/>
</jsp:include>

<main class="main-content">
    <div class="table-container">
        <div class="table-header-box">
            <h2 class="page-title"><i class="fas fa-clock-rotate-left" style="color: #f1c40f;"></i> Historial de Herramientas</h2>
        </div>

        <form class="form-grid" style="grid-template-columns: repeat(4, 1fr); gap:12px; margin-bottom:10px;"
              onsubmit="return false;">
            <div class="form-group">
                <label><i class="fas fa-search"></i> Buscar</label>
                <input type="text" id="hh-buscar" placeholder="Nombre u observación" autocomplete="off">
            </div>
            <div class="form-group">
                <label><i class="fas fa-exchange-alt"></i> Tipo</label>
                <select id="hh-tipo">
                    <option value="">Todos</option>
                    <option value="entrega">Entrega (salió a proyecto)</option>
                    <option value="devolucion">Devolución OK</option>
                    <option value="dano">Devuelto dañado</option>
                    <option value="perdida">Perdida</option>
                    <option value="ingreso">Ingreso de stock</option>
                    <option value="reparacion">Reparación</option>
                    <option value="baja">Baja definitiva</option>
                    <option value="ajuste">Ajuste de cantidad</option>
                    <option value="edicion">Cambio de observación</option>
                </select>
            </div>
            <div class="form-group">
                <label><i class="fas fa-calendar-minus"></i> Desde</label>
                <input type="date" id="hh-desde">
            </div>
            <div class="form-group">
                <label><i class="fas fa-calendar-plus"></i> Hasta</label>
                <input type="date" id="hh-hasta">
            </div>
        </form>

        <%-- Una fila por OPERACIÓN (no por herramienta): lo registrado en un mismo
             movimiento se agrupa y se despliega al hacer clic, igual que en el
             historial de la bodega de productos. --%>
        <table class="user-table">
            <thead>
                <tr>
                    <th style="width:34px;"></th>
                    <th>Fecha</th>
                    <th style="text-align:center;">Tipo</th>
                    <th style="text-align:center;">Herramientas</th>
                    <th style="text-align:center;">Unidades</th>
                    <th style="text-align:center;">Acta</th>
                    <th>Usuario</th>
                    <th>Observaciones</th>
                </tr>
            </thead>
            <tbody id="tbodyHistHerr"></tbody>
        </table>

        <div id="pie-hist-herr" style="margin-top:14px; color:#777;"></div>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/historial-herramientas.js?v=32"></script>
</body>
</html>
