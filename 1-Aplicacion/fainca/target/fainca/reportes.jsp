<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Objetos.Marca, Dao.MarcaDAO, java.util.List" %>
<% List<Marca> marcas = new MarcaDAO().listar(); %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Reportes - FAINCA Inventario</title>
<link rel="stylesheet" href="vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="css/style.css?v=32">
<script src="js/sweetalert2.all.min.js?v=32"></script>
</head>
<body>

<jsp:include page="sidebar.jsp">
    <jsp:param name="activo" value="reportes"/>
</jsp:include>

<main class="main-content">
    <div class="form-container">
        <div class="header-modulo" style="text-align:center;">
            <h2><i class="fas fa-file-export" style="color: #f1c40f;"></i> EXPORTAR REPORTE DE INVENTARIO</h2>
            <p>Genera un informe con el código, la cantidad y la ubicación de los productos.</p>
        </div>

        <div class="form-grid">
            <div class="form-group">
                <label><i class="fas fa-industry"></i> ¿De qué marca quieres el reporte?</label>
                <select id="rep-marca">
                    <option value="todas">Todas las marcas</option>
                    <% for (Marca m : marcas) { %>
                    <option value="<%= m.getId() %>"><%= m.getNombre() %></option>
                    <% } %>
                </select>
            </div>
            <br>
            <div class="form-group">
                <label><i class="fas fa-align-left"></i> ¿Incluir la descripción de cada producto?</label>
                <select id="rep-descripcion">
                    <option value="no">No — solo código, cantidad y ubicación</option>
                    <option value="si">Sí — con descripción (reporte más detallado, pero más pesado y extenso)</option>
                </select>
            </div>
            <br>
            <div class="form-group">
                <label><i class="fas fa-camera"></i> ¿Incluir la foto de cada producto?</label>
                <select id="rep-foto">
                    <option value="no">No — sin fotos (archivo liviano)</option>
                    <option value="si">Sí — con foto (catálogo visual, pero bastante más pesado)</option>
                </select>
            </div>
        </div>

        <label style="display:block; margin:18px 0 10px; color:#444; font-weight:bold;">
            <i class="fas fa-file-download"></i> Elige el formato:
        </label>

        <div class="formato-opciones">
            <button type="button" class="btn-formato" data-formato="excel">
                <i class="fas fa-file-excel"></i>
                <span>Excel</span>
                <small>.xlsx — editable, para sumar o filtrar</small>
            </button>
            <button type="button" class="btn-formato" data-formato="pdf">
                <i class="fas fa-file-pdf"></i>
                <span>PDF</span>
                <small>listo para imprimir o enviar</small>
            </button>
        </div>

        <p style="color:#999; font-size:0.85rem; margin-top:20px;">
            <i class="fas fa-info-circle"></i>
            El reporte lleva siempre el logo de FAINCA, la marca elegida y la fecha en que se genera.
            Solo incluye productos activos.
        </p>
    </div>
</main>

<script src="js/app.js?v=32"></script>
<script src="js/reportes.js?v=32"></script>
</body>
</html>
