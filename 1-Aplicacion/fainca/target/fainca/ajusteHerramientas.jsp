<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Ajuste de Herramientas - FAINCA</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar-herramientas.jsp">
    <jsp:param name="activo" value="ajusteHerr"/>
</jsp:include>

<main class="main-content">
    <div class="form-container">
        <div class="header-modulo">
            <h2><i class="fas fa-scale-balanced" style="color: #f1c40f;"></i> Ajuste de Herramientas</h2>
            <p>Sume o reste unidades (compras, correcciones, descartes) y actualice la observación
               de cada herramienta. Todo queda registrado en el historial.</p>
        </div>

        <form id="ajusteForm">
            <table class="dinamic-table">
                <thead>
                    <tr>
                        <th style="width: 26%;">Herramienta</th>
                        <th style="width: 9%; text-align:center;">Disponible</th>
                        <th style="width: 13%;">Operación</th>
                        <th style="width: 11%;">Cantidad</th>
                        <th style="width: 34%;">Observación de la herramienta</th>
                        <th style="width: 7%; text-align: center;">Acción</th>
                    </tr>
                </thead>
                <tbody id="tbodyAjusteHerr"></tbody>
            </table>

            <button type="button" class="btn-add-row" onclick="agregarFilaAjuste()">
                <i class="fas fa-plus"></i> Añadir otra herramienta
            </button>

            <div class="observacion-section">
                <label><i class="fas fa-comment-alt"></i> Motivo del ajuste</label>
                <textarea id="aj-motivo" class="textarea-obs"
                          placeholder="Ej: Compra de 3 taladros según factura 001-234 / Corrección tras conteo físico..."
                          required></textarea>
            </div>

            <div class="actions-form">
                <button type="button" class="btn-procesar" onclick="procesarAjusteHerr()">
                    <i class="fas fa-check-circle"></i> Guardar ajuste
                </button>
            </div>
        </form>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/ajuste-herramientas.js?v=32"></script>
</body>
</html>
