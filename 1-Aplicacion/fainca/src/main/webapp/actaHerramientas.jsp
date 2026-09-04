<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Entrega de Herramientas (Acta) - FAINCA</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar-herramientas.jsp">
    <jsp:param name="activo" value="acta"/>
</jsp:include>

<main class="main-content">
    <div class="form-container">
        <div class="header-modulo">
            <h2><i class="fas fa-file-signature" style="color: #f1c40f;"></i> Entrega de Herramientas (Acta)</h2>
            <p>Registre todo lo que el técnico se lleva en una sola acta. Al guardarla podrá imprimir el PDF para la firma.</p>
        </div>

        <form id="actaForm">
            <div class="form-grid" style="grid-template-columns: repeat(3, 1fr); gap:12px;">
                <div class="form-group">
                    <label><i class="fas fa-user"></i> Solicitante (quien retira y firma)</label>
                    <input type="text" id="acta-solicitante" placeholder="Nombre del técnico" required autocomplete="off">
                </div>
                <div class="form-group">
                    <label><i class="fas fa-diagram-project"></i> Proyecto</label>
                    <input type="text" id="acta-proyecto" placeholder="Ej: Proyecto Terrafértil" required autocomplete="off">
                </div>
                <div class="form-group">
                    <label><i class="fas fa-location-dot"></i> Destino (planta/bodega, opcional)</label>
                    <input type="text" id="acta-destino" placeholder="Ej: Bodega Mapasingue" autocomplete="off">
                </div>
            </div>

            <table class="dinamic-table" style="margin-top:15px;">
                <thead>
                    <tr>
                        <th style="width: 42%;">Herramienta o consumible</th>
                        <th style="width: 14%;">Cantidad</th>
                        <th style="width: 36%;">Observación de la línea (series, nº de parte...)</th>
                        <th style="width: 8%; text-align: center;">Acción</th>
                    </tr>
                </thead>
                <tbody id="tbodyActa"></tbody>
            </table>

            <button type="button" class="btn-add-row" onclick="agregarFilaActa()">
                <i class="fas fa-plus"></i> Añadir otra línea
            </button>

            <div class="observacion-section">
                <label><i class="fas fa-comment-alt"></i> Observación general del acta (opcional)</label>
                <textarea id="acta-obs" class="textarea-obs"></textarea>
            </div>

            <div class="actions-form">
                <button type="button" class="btn-procesar" onclick="procesarActa()">
                    <i class="fas fa-check-circle"></i> Registrar acta de entrega
                </button>
            </div>
        </form>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/acta-herramientas.js?v=32"></script>
</body>
</html>
