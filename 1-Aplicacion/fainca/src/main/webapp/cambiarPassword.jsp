<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Cambiar Contraseña - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp"/>

<main class="main-content">
    <div class="form-container" style="max-width:480px;">
        <div class="header-modulo" style="text-align:center;">
            <h2><i class="fas fa-key" style="color: #f1c40f;"></i> Cambiar Contraseña</h2>
            <p>La nueva contraseña debe tener al menos 6 caracteres</p>
        </div>

        <form id="form-password">
            <div class="form-grid" style="grid-template-columns: 1fr;">
                <div class="form-group">
                    <label><i class="fas fa-lock"></i> Contraseña actual</label>
                    <input id="actual" type="password" required autocomplete="current-password">
                </div>
                <div class="form-group">
                    <label><i class="fas fa-lock-open"></i> Contraseña nueva</label>
                    <input id="nueva" type="password" required minlength="6" autocomplete="new-password">
                </div>
                <div class="form-group">
                    <label><i class="fas fa-check-double"></i> Repetir contraseña nueva</label>
                    <input id="confirmar" type="password" required minlength="6" autocomplete="new-password">
                </div>
            </div>
            <div class="actions-form">
                <button class="btn-registrar" type="submit"><i class="fas fa-save"></i> Actualizar contraseña</button>
            </div>
        </form>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/cambiar-password.js?v=32"></script>
</body>
</html>
