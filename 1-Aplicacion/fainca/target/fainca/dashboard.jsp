<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Panel de Control - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="panel"/>
</jsp:include>

<main class="main-content">

    <!-- Tarjetas de resumen -->
    <div class="stat-grid">
        <div class="stat-tile">
            <div class="stat-numero" id="st-productos">—</div>
            <div class="stat-label"><i class="fas fa-box-open"></i> Productos activos</div>
        </div>
        <div class="stat-tile">
            <div class="stat-numero" id="st-unidades">—</div>
            <div class="stat-label"><i class="fas fa-cubes"></i> Unidades en bodega</div>
        </div>
        <div class="stat-tile">
            <div class="stat-numero" id="st-marcas">—</div>
            <div class="stat-label"><i class="fas fa-industry"></i> Marcas</div>
        </div>
        <div class="stat-tile">
            <div class="stat-numero" id="st-mov-hoy">—</div>
            <div class="stat-label"><i class="fas fa-exchange-alt"></i> Movimientos hoy</div>
            <div class="stat-detalle" id="st-hoy-detalle"></div>
        </div>
    </div>

    <!-- Accesos rapidos -->
    <div class="table-container" style="margin-top:18px;">
        <h3 style="margin:0 0 12px 0;"><i class="fas fa-bolt" style="color:#f1c40f;"></i> Accesos rápidos</h3>
        <div class="quick-actions">
            <a href="ingresoInventario.jsp"><i class="fas fa-arrow-down"></i> Registrar Ingreso</a>
            <a href="salidaInventario.jsp"><i class="fas fa-arrow-up"></i> Registrar Salida</a>
            <a href="ajusteInventario.jsp"><i class="fas fa-scale-balanced"></i> Ajustar Inventario</a>
            <a href="registrarProductos.jsp"><i class="fas fa-plus-circle"></i> Nuevo Producto</a>
            <a href="index.jsp"><i class="fas fa-search"></i> Buscar Producto</a>
            <a href="historial.jsp"><i class="fas fa-history"></i> Ver Historial</a>
        </div>
    </div>

    <!-- Actividad de la semana -->
    <div class="table-container" style="margin-top:18px;">
        <div class="table-header-box" style="margin-bottom:8px;">
            <h3 style="margin:0;"><i class="fas fa-chart-column" style="color:#f1c40f;"></i> Actividad de los últimos 7 días (unidades)</h3>
            <div class="leyenda">
                <span><span class="chip" style="background:#2166ac;"></span> <i class="fas fa-arrow-down"></i> Ingresos</span>
                <span><span class="chip" style="background:#c76b04;"></span> <i class="fas fa-arrow-up"></i> Salidas</span>
            </div>
        </div>
        <div id="grafico" class="grafico"></div>
        <div id="grafico-tooltip" class="grafico-tooltip" style="display:none;"></div>
    </div>

    <!-- Ultimos movimientos -->
    <div class="table-container" style="margin-top:18px;">
        <h3 style="margin:0 0 10px 0;"><i class="fas fa-clock-rotate-left" style="color:#f1c40f;"></i> Últimos movimientos</h3>
        <table class="user-table">
            <thead>
                <tr>
                    <th>Fecha</th>
                    <th>Código</th>
                    <th>Marca</th>
                    <th style="text-align:center;">Tipo</th>
                    <th style="text-align:center;">Cantidad</th>
                    <th>Usuario</th>
                </tr>
            </thead>
            <tbody id="tbodyUltimos"></tbody>
        </table>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/dashboard.js?v=32"></script>
</body>
</html>
