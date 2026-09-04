<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Ajuste de Inventario - FAINCA</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="ajuste"/>
</jsp:include>

<main class="main-content">
    <div class="form-container">
        <div class="header-modulo">
            <h2><i class="fas fa-scale-balanced" style="color: #f1c40f;"></i> Ajuste de Inventario (Reconteo)</h2>
            <p>Cuando el conteo físico no coincide con el sistema: ingrese la cantidad REAL contada
               y el sistema corrige el stock registrando la diferencia en el historial.</p>
        </div>

        <form id="ajusteForm">
            <%-- Un reconteo suele abarcar una percha entera, asi que se cargan varios
                 productos a la vez y todos quedan en UN solo documento del historial. --%>
            <table class="dinamic-table">
                <thead>
                    <tr>
                        <th style="width: 34%;">Código del Producto</th>
                        <th style="width: 13%; text-align:center;">Stock sistema</th>
                        <th style="width: 15%; text-align:center;">Cantidad REAL</th>
                        <th style="width: 13%; text-align:center;">Diferencia</th>
                        <th style="width: 17%;">Ubicación</th>
                        <th style="width: 8%; text-align:center;">Acción</th>
                    </tr>
                </thead>
                <tbody id="tbodyAjuste"></tbody>
            </table>

            <button type="button" class="btn-add-row" onclick="agregarFila()">
                <i class="fas fa-plus"></i> Añadir otro producto
            </button>

            <%-- Sin campo de fecha a proposito: un reconteo fisico siempre se registra
                 con la fecha y hora del momento en que se hace (y el servidor lo garantiza). --%>

            <div class="observacion-section">
                <label><i class="fas fa-comment-alt"></i> Motivo del ajuste (obligatorio)</label>
                <textarea id="aj-obs" class="textarea-obs" placeholder="Ej: Reconteo físico mensual / Se encontraron 3 unidades dañadas / Error de registro anterior..." required></textarea>
                <small style="color:#999; margin-top:4px; display:block;">
                    <i class="fas fa-info-circle"></i>
                    El motivo se aplica a todos los productos de este reconteo.
                </small>
            </div>

            <div class="actions-form">
                <button type="button" class="btn-procesar" onclick="procesarAjuste()">
                    <i class="fas fa-check-circle"></i> Registrar Ajuste
                </button>
            </div>
        </form>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/ajuste.js?v=32"></script>
</body>
</html>
