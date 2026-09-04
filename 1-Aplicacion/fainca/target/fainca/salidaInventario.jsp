<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Salida de Inventario - FAINCA</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body data-tipo="egreso">

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="salida"/>
</jsp:include>

<main class="main-content">
    <div class="form-container">
        <div class="header-modulo">
            <h2><i class="fas fa-arrow-up" style="color: #f1c40f;"></i> Salida de Inventario (Despacho)</h2>
            <p>Registre la salida o retiro de uno o múltiples componentes del almacén.</p>
        </div>

        <form id="movimientoForm">
            <table class="dinamic-table">
                <thead>
                    <tr>
                        <th style="width: 60%;">Código del Producto</th>
                        <th style="width: 30%;">Cantidad a Retirar</th>
                        <th style="width: 10%; text-align: center;">Acción</th>
                    </tr>
                </thead>
                <tbody id="tbodyProductos"></tbody>
            </table>

            <button type="button" class="btn-add-row" onclick="agregarFila()">
                <i class="fas fa-plus"></i> Añadir otro producto
            </button>

            <div class="form-grid" style="margin-top:25px;">
                <div class="form-group">
                    <label><i class="fas fa-calendar-alt"></i> Fecha del movimiento (opcional, por defecto hoy; máximo 2 días atrás)</label>
                    <input type="date" id="mov-fecha">
                </div>
            </div>

            <div class="observacion-section">
                <label><i class="fas fa-comment-alt"></i> Observación / Destino del Material</label>
                <textarea id="mov-obs" class="textarea-obs" placeholder="Ej: Despacho para el proyecto de automatización de planta X / Venta bajo factura #4502..." required></textarea>
            </div>

            <div class="actions-form">
                <button type="button" class="btn-procesar" onclick="procesarFormulario()">
                    <i class="fas fa-check-circle"></i> Procesar y Disminuir Stock
                </button>
            </div>
        </form>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/movimiento-lote.js?v=32"></script>
</body>
</html>
