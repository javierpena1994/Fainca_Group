<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Objetos.Marca, Dao.MarcaDAO, java.util.List" %>
<% List<Marca> marcas = new MarcaDAO().listar(); %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Registrar Producto - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="registrar"/>
</jsp:include>

<main class="main-content">
    <div class="form-container">
        <div class="header-modulo" style="text-align:center;">
            <h2><i class="fas fa-plus-circle" style="color: #f1c40f;"></i> Registrar Nuevo Producto</h2>
            <p>Ingrese la información técnica para el inventario de automatización</p>
        </div>

        <form id="registroForm">
            <div class="form-grid">
                <div class="form-group">
                    <label><i class="fas fa-industry"></i> Marca</label>
                    <select id="p-marca" required>
                        <option value="" disabled selected>Selecciona una marca</option>>
                        <% for (Marca m : marcas) { %>
                        <option value="<%= m.getId() %>"><%= m.getNombre() %></option>
                        <% } %>
                    </select>
                </div>

                <div class="form-group">
                    <label><i class="fas fa-barcode"></i> Código de Producto</label>
                    <input type="text" id="p-codigo" placeholder="Ej: 2740420000, 751004..." required>
                </div>

                <div class="form-group">
                    <label><i class="fas fa-sort-numeric-up-alt"></i> Cantidad Inicial (conteo)</label>
                    <input type="number" id="p-stock" placeholder="0" min="0" value="0" required>
                </div>

                <div class="form-group">
                    <label><i class="fas fa-map-marker-alt"></i> Ubicación (Percha)</label>
                    <input type="text" id="p-ubicacion" placeholder="Ej: AA03, AA01...">
                </div>

                <div class="form-group">
                    <label><i class="fas fa-balance-scale"></i> Clasificación / Unidad</label>
                    <input type="text" id="p-unidad" value="UND.">
                </div>

                <%-- Sin campo de fecha a proposito: todo producto registrado queda en el
                     historial con la fecha y hora actuales (el servidor lo garantiza). --%>

                <div class="form-group full-width">
                    <label><i class="fas fa-align-left"></i> Características Técnicas (opcional)</label>
                    <textarea id="p-descripcion" placeholder="Describa voltajes, protocolos, dimensiones y especificaciones..."></textarea>
                </div>

                <div class="form-group full-width">
                    <label><i class="fas fa-image"></i> Foto de referencia (opcional)</label>
                    <div class="zona-imagen" id="zona-p-imagen">
                        <input type="file" id="p-imagen" accept="image/*" hidden>
                        <div class="zona-vacia">
                            <i class="fas fa-cloud-arrow-up"></i>
                            <p><strong>Arrastra la foto aquí</strong> o pégala con <kbd>Ctrl</kbd>+<kbd>V</kbd></p>
                            <span>también puedes hacer clic para buscarla en el equipo</span>
                        </div>
                        <img class="zona-preview" alt="Vista previa de la foto" hidden>
                        <button type="button" class="zona-quitar" title="Quitar la foto" hidden>
                            <i class="fas fa-xmark"></i>
                        </button>
                        <small class="zona-nombre" hidden></small>
                    </div>
                </div>
            </div>

            <div class="actions-form">
                <button type="submit" class="btn-registrar">
                    <i class="fas fa-save"></i> Guardar en Inventario
                </button>
            </div>
        </form>

        <hr style="border:none; border-top:1px solid #e0e0e0; margin:25px 0;">

        <form id="form-marca" style="display:flex; gap:12px; align-items:flex-end; flex-wrap:wrap;">
            <div class="form-group" style="flex:1; min-width:200px;">
                <label><i class="fas fa-plus"></i> ¿Marca nueva? Agrégala aquí</label>
                <input type="text" id="nueva-marca" placeholder="Nombre de la marca">
            </div>
            <button type="submit" class="btn-add-row" style="margin-top:0;">Agregar marca</button>
        </form>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/registrar.js?v=32"></script>
</body>
</html>
