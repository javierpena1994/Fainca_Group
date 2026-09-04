<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Gestionar Usuarios - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="usuarios"/>
</jsp:include>

<main class="main-content">
    <div class="table-container">
        <div class="table-header-box">
            <h2 class="page-title"><i class="fas fa-users-cog" style="color: #f1c40f;"></i> Gestionar Usuarios</h2>
        </div>

        <p style="color:#777; margin-top:0;">
            <i class="fas fa-info-circle"></i>
            <strong>Administrador</strong>: acceso total, incluida esta pantalla.
            <strong>Bodega</strong>: registra productos, ingresos, salidas e historial.
            <strong>Ventas</strong>: solo consulta el stock.
        </p>

        <table class="user-table">
            <thead>
                <tr>
                    <th>Nombre</th>
                    <th>Usuario (login)</th>
                    <th style="text-align:center;">Rol</th>
                    <th style="text-align:center;">Estado</th>
                    <th style="text-align:center;">Acciones</th>
                </tr>
            </thead>
            <tbody id="tbodyUsuarios"></tbody>
        </table>
    </div>

    <!-- Formulario: agregar / editar usuario -->
    <div class="form-container" id="card-usuario" style="margin-top:25px;">
        <div class="header-modulo">
            <h2 id="titulo-form"><i class="fas fa-user-plus" style="color: #f1c40f;"></i> Nuevo usuario</h2>
        </div>
        <div id="mensaje-form"></div>
        <form id="form-usuario">
            <input type="hidden" id="u-id" value="">
            <div class="form-grid">
                <div class="form-group">
                    <label><i class="fas fa-id-card"></i> Nombre visible</label>
                    <input type="text" id="u-nombre" placeholder="Ej: Administrador, Bodega, Ventas..." required>
                </div>
                <div class="form-group">
                    <label><i class="fas fa-user"></i> Usuario (para iniciar sesión)</label>
                    <input type="text" id="u-usuario" placeholder="Ej: nelly, victor, ventas1..." required>
                </div>
                <div class="form-group">
                    <label><i class="fas fa-user-tag"></i> Rol</label>
                    <select id="u-rol" required>
                        <option value="superadmin">Administrador (acceso total + usuarios)</option>
                        <option value="admin">Bodega (inventario completo)</option>
                        <option value="ventas">Ventas (solo consulta)</option>
                    </select>
                </div>
                <div class="form-group" id="grupo-estado" style="display:none;">
                    <label><i class="fas fa-toggle-on"></i> Estado</label>
                    <select id="u-activo">
                        <option value="1">Activo</option>
                        <option value="0">Inactivo (no puede entrar)</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>
                        <i class="fas fa-key"></i>
                        <span id="label-password">Contraseña</span>
                    </label>
                    <input type="password" id="u-password" placeholder="Mínimo 6 caracteres" autocomplete="new-password">
                    <small id="ayuda-password" style="color:#999; margin-top:4px; display:none;">
                        Déjalo en blanco para conservar la contraseña actual.
                    </small>
                </div>
            </div>
            <div class="actions-form">
                <button type="submit" class="btn-registrar"><i class="fas fa-save"></i> <span id="txt-guardar">Crear usuario</span></button>
                <button type="button" class="btn-secundario" id="btn-cancelar" onclick="modoNuevo()" style="display:none;">Cancelar edición</button>
            </div>
        </form>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/usuarios.js?v=32"></script>
</body>
</html>
