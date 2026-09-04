// Igual que Usuario.esAdmin() en Java: el superadmin (Administrador) puede todo
// lo que un admin (Bodega). Si aqui solo se compara con 'admin', al superadmin
// se le ocultan los botones de editar e historial.
const esAdmin = ['admin', 'superadmin'].includes(document.body.dataset.rol);

const tbody = document.getElementById('userTbody');
const searchInput = document.getElementById('searchInput');
const checkEliminados = document.getElementById('ver-eliminados');
let temporizador = null;

// Un producto dado de baja no se muestra, salvo que el admin marque la casilla
// para poder encontrarlo y reactivarlo.
function verEliminados() {
    return !!(checkEliminados && checkEliminados.checked);
}

// --- Carga inicial: inventario completo ---
async function cargarInventario() {
    renderFilas(await apiFetch(verEliminados() ? 'ProductosServlet?incluir_inactivos=1' : 'ProductosServlet'));
}

// --- Busqueda en vivo contra el servidor (digito por digito) ---
async function refrescar() {
    const q = searchInput.value.trim();
    if (!q) return cargarInventario();
    const ruta = `BuscarProductosServlet?q=${encodeURIComponent(q)}`
        + (verEliminados() ? '&incluir_inactivos=1' : '');
    renderFilas(await apiFetch(ruta));
}

searchInput.addEventListener('input', () => {
    clearTimeout(temporizador);
    temporizador = setTimeout(refrescar, 180);
});

if (checkEliminados) checkEliminados.addEventListener('change', refrescar);

function renderFilas(productos) {
    if (productos.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="6" style="text-align:center; padding:30px; color:#777; font-weight:bold;">
                <i class="fas fa-info-circle"></i> No se encontraron productos.
            </td></tr>`;
        return;
    }

    tbody.innerHTML = productos.map((p) => {
        const inactivo = esAdmin && p.activo === 0;
        const botones = esAdmin
            ? `<button type="button" class="btn-detalles" onclick="verDetalles('${esc(p.codigo)}')" title="Ver Detalles"><i class="fas fa-eye"></i></button>
               <button type="button" class="btn-detalles" onclick="editarProducto('${esc(p.codigo)}')" title="Editar"><i class="fas fa-pen"></i></button>
               <a class="btn-detalles" style="text-decoration:none; display:inline-block;" href="historial.jsp?codigo=${encodeURIComponent(p.codigo)}" title="Historial"><i class="fas fa-history"></i></a>`
            : `<button type="button" class="btn-detalles" onclick="verDetalles('${esc(p.codigo)}')" title="Ver Detalles"><i class="fas fa-eye"></i></button>`;

        return `
            <tr class="${inactivo ? 'fila-inactiva' : ''}">
                <td><strong>${esc(p.codigo)}</strong></td>
                <td>${esc(p.marca)}</td>
                <td>${esc(p.descripcion) || '<span style="color:#bbb;">(sin descripción)</span>'}</td>
                <td>${esc(p.ubicacion) || ''}</td>
                <td style="text-align:center;">${p.stock_actual} ${esc(p.unidad_medida)}</td>
                <td style="text-align:center; white-space:nowrap;">${botones}</td>
            </tr>`;
    }).join('');
}

// --- Detalles ---
async function verDetalles(codigo) {
    try {
        const p = await apiFetch(`ProductosServlet?codigo=${encodeURIComponent(codigo)}`);
        const foto = p.imagen
            ? `<div style="text-align:center; margin-bottom:12px;">
                   <img src="ImagenServlet?archivo=${encodeURIComponent(p.imagen)}" alt="Foto de ${esc(codigo)}"
                        style="max-width:100%; max-height:220px; border-radius:8px; border:1px solid #e0e0e0;">
               </div>`
            : '';
        const lineas = [
            `<b>Marca:</b> ${esc(p.marca)}`,
            `<b>Característica:</b> ${esc(p.descripcion) || '(sin descripción)'}`,
            `<b>Ubicación:</b> ${esc(p.ubicacion) || '-'}`,
            `<b>Stock:</b> ${p.stock_actual} ${esc(p.unidad_medida)}`,
        ];
        if (p.nota_maletas) lineas.push(`<b>Contenido de la maleta:</b> ${esc(p.nota_maletas)}`);
        Swal.fire({
            title: `Producto ${esc(codigo)}`,
            html: `${foto}<div style="text-align:left;">${lineas.join('<br>')}</div>`,
            icon: foto ? undefined : 'info',
            confirmButtonColor: '#444',
            confirmButtonText: 'Cerrar',
        });
    } catch (err) {
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

// --- Edicion (solo admin; el panel solo existe en el HTML si es admin) ---
const cardEditar = document.getElementById('card-editar');
const zonaImagenEditar = document.getElementById('zona-e-imagen');
inicializarZonaImagen(zonaImagenEditar);
let codigoEnEdicion = null;

// El campo "Contenido de la maleta" solo aparece en las maletas-kit (codigos BAN-TC..),
// o en cualquier producto al que ya se le haya escrito una nota (por si el patron cambia).
function esMaleta(codigo, notaExistente) {
    return /^BAN-?TC/i.test(codigo || '') || !!(notaExistente && notaExistente.trim());
}

async function editarProducto(codigo) {
    try {
        const p = await apiFetch(`ProductosServlet?codigo=${encodeURIComponent(codigo)}`);
        codigoEnEdicion = p.codigo;
        document.getElementById('editar-codigo').textContent = `(${p.codigo})`;
        document.getElementById('e-marca').value = p.marca_id;
        document.getElementById('e-unidad').value = p.unidad_medida;
        document.getElementById('e-ubicacion').value = p.ubicacion || '';
        // Campo de contenido de maleta: solo visible (y enviado) si el producto es una maleta.
        const grupoMaletas = document.getElementById('grupo-nota-maletas');
        if (esMaleta(p.codigo, p.nota_maletas)) {
            document.getElementById('e-nota-maletas').value = p.nota_maletas || '';
            grupoMaletas.style.display = '';
        } else {
            document.getElementById('e-nota-maletas').value = '';
            grupoMaletas.style.display = 'none';
        }
        document.getElementById('e-descripcion').value = p.descripcion || '';
        document.getElementById('e-activo').value = p.activo ? '1' : '0';
        document.getElementById('e-observaciones').value = '';
        zonaImagenEditar.mostrarExistente(
            p.imagen ? `ImagenServlet?archivo=${encodeURIComponent(p.imagen)}` : null);
        cardEditar.style.display = 'block';
        cardEditar.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (err) {
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

function cerrarEdicion() {
    codigoEnEdicion = null;
    cardEditar.style.display = 'none';
    zonaImagenEditar.limpiar();
}

// Corregir el codigo (PK/FK) de un producto que quedo mal cargado (ej. celda vacia
// o con un caracter suelto al importar desde Excel). Arrastra el historial consigo.
const btnEditarCodigo = document.getElementById('btn-editar-codigo');
if (btnEditarCodigo) {
    btnEditarCodigo.addEventListener('click', async () => {
        if (!codigoEnEdicion) return;
        const { value: nuevoCodigo } = await Swal.fire({
            title: 'Corregir código',
            html: `El código actual es <b>${esc(codigoEnEdicion)}</b>. Esto cambia su identificador en todo el
                   sistema, incluido el historial de movimientos ya registrado. Úsalo solo para corregir un
                   código mal cargado, no para renombrar productos por gusto.`,
            input: 'text',
            inputValue: codigoEnEdicion,
            inputPlaceholder: 'Nuevo código',
            showCancelButton: true,
            confirmButtonColor: '#f1c40f',
            cancelButtonColor: '#444',
            confirmButtonText: 'Corregir',
            cancelButtonText: 'Cancelar',
            inputValidator: (valor) => {
                if (!valor || !valor.trim()) return 'El código no puede estar vacío';
                if (valor.trim() === codigoEnEdicion) return 'Escribe un código distinto al actual';
            },
        });
        if (!nuevoCodigo) return;

        try {
            await apiFetch('RenombrarCodigoServlet', {
                method: 'POST',
                body: JSON.stringify({ codigo_actual: codigoEnEdicion, codigo_nuevo: nuevoCodigo.trim() }),
            });
            const codigoAnterior = codigoEnEdicion;
            codigoEnEdicion = nuevoCodigo.trim();
            document.getElementById('editar-codigo').textContent = `(${codigoEnEdicion})`;
            await Swal.fire({
                title: 'Código corregido',
                html: `<b>${esc(codigoAnterior)}</b> ahora es <b>${esc(codigoEnEdicion)}</b>. El historial se movió con él.`,
                icon: 'success',
                confirmButtonColor: '#f1c40f',
            });
            await refrescar();
        } catch (err) {
            Swal.fire({ title: 'No se pudo corregir', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
        }
    });
}

if (cardEditar) {
    document.getElementById('form-editar').addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!codigoEnEdicion) return;

        const body = {
            codigo: codigoEnEdicion,
            marca_id: Number(document.getElementById('e-marca').value),
            unidad_medida: document.getElementById('e-unidad').value.trim() || 'UND.',
            ubicacion: document.getElementById('e-ubicacion').value.trim(),
            descripcion: document.getElementById('e-descripcion').value.trim(),
            activo: Number(document.getElementById('e-activo').value),
            observaciones: document.getElementById('e-observaciones').value.trim() || undefined,
        };
        // Solo se manda la nota de maletas si el campo esta visible (producto = maleta),
        // para no borrar por accidente el valor en los productos que no lo usan.
        if (document.getElementById('grupo-nota-maletas').style.display !== 'none') {
            body.nota_maletas = document.getElementById('e-nota-maletas').value.trim();
        }

        try {
            await apiFetch('EditarProductoServlet', { method: 'POST', body: JSON.stringify(body) });

            const archivoImagen = document.getElementById('e-imagen').files[0];
            let errorFoto = null;
            if (archivoImagen) {
                try {
                    await subirImagenProducto(codigoEnEdicion, archivoImagen);
                } catch (err) {
                    errorFoto = err.message;
                }
            }

            await Swal.fire({
                title: errorFoto ? 'Guardado, pero sin la foto' : 'Actualizado',
                html: errorFoto
                    ? `Los datos de <b>${esc(codigoEnEdicion)}</b> se guardaron, pero la foto no se pudo subir:<br><br><i>${esc(errorFoto)}</i>`
                    : `Producto <b>${esc(codigoEnEdicion)}</b> guardado correctamente${archivoImagen ? ', junto con su foto' : ''}.`,
                icon: errorFoto ? 'warning' : 'success',
                confirmButtonColor: '#f1c40f',
            });
            cerrarEdicion();
            await refrescar();
        } catch (err) {
            Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
        }
    });
}

cargarInventario();
