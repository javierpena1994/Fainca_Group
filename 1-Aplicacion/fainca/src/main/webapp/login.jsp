<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Ingreso - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>
<div class="login-wrapper">
    <div class="login-box">
        <div class="logo-area">
            <img src="images/logo-fainca.png?v=2" alt="FAINCA Group" class="logo-img"
                 width="220" height="57" style="width:220px; height:57px; max-width:80%; display:block; margin:0 auto;">
            <span>Inventario &mdash; Bodega #1</span>
        </div>
        <form action="LoginServlet" method="POST">
            <div class="form-group">
                <label><i class="fas fa-user"></i> Usuario</label>
                <input type="text" name="usuario" required autocomplete="username" autofocus>
            </div>
            <div class="form-group">
                <label><i class="fas fa-lock"></i> Contraseña</label>
                <input type="password" name="password" required autocomplete="current-password">
            </div>
            <button class="btn-registrar" type="submit" style="justify-content:center;">
                <i class="fas fa-sign-in-alt"></i> Entrar
            </button>
        </form>
    </div>
</div>

<footer class="credito-desarrollo">Desarrollado por J. Peña &amp; S. Vinces</footer>

<%-- Cuenta bloqueada por demasiados intentos fallidos --%>
<% String bloqueado = request.getParameter("bloqueado"); %>
<% if (bloqueado != null && bloqueado.matches("\\d+")) { %>
<script>
    Swal.fire({
        title: 'Cuenta bloqueada temporalmente',
        html: 'Se superó el número de intentos permitidos.<br><br>' +
              'Vuelve a intentarlo en <b><%= bloqueado %> minuto(s)</b>.<br>' +
              '<small style="color:#777;">Si olvidaste tu contraseña, pídele al Administrador que la restablezca.</small>',
        icon: 'warning',
        confirmButtonColor: '#f1c40f'
    });
</script>
<% } else if ("1".equals(request.getParameter("error"))) { %>
<%  String quedan = request.getParameter("quedan"); %>
<script>
    Swal.fire({
        title: 'No se pudo ingresar',
        html: 'Usuario o contraseña incorrectos.<%= (quedan != null && quedan.matches("\\d+"))
                  ? "<br><br><small style=\\\"color:#777;\\\">Intentos restantes antes del bloqueo: <b>" + quedan + "</b></small>"
                  : "" %>',
        icon: 'error',
        confirmButtonColor: '#f1c40f'
    });
</script>
<% } %>
</body>
</html>
