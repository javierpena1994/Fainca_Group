const tbodyEliminar = document.getElementById('userTbody');
const searchEliminar = document.getElementById('searchInput');
let temporizadorElim = null;
let productosCache = [];

async function cargarProductos() {
    productosCache = await apiFetch('ProductosServlet');
    renderFilas(productosCache);
}

searchEliminar.addEventListener('input', () => {
    clearTimeout(temporizadorElim);
    const q = searchEliminar.value.trim();
    temporizadorElim = setTimeout(async () => {
        if (!q) return renderFilas(productosCache);
        renderFilas(await apiFetch(`BuscarProductosServlet?q=${encodeURIComponent(q)}`));
    }, 180);
});

function renderFilas(productos) {
    if (productos.length === 0) {
        tbodyEliminar.innerHTML = `
            <tr><td colspan="6" style="text-align:center; padding:30px; color:#777; font-weight:bold;">
                <i class="fas fa-info-circle"></i> No se encontraron productos.
            </td></tr>`;
        return;
    }
    tbodyEliminar.innerHTML = productos.map((p) => `
        <tr>
            <td><strong>${esc(p.codigo)}</strong></td>
            <td>${esc(p.marca)}</td>
            <td>${esc(p.descripcion) || '<span style="color:#bbb;">(sin descripción)</span>'}</td>
            <td>${esc(p.ubicacion) || ''}</td>
            <td style="text-align:center;">${p.stock_actual}</td>
            <td style="text-align:center;">
                <button type="button" class="btn-eliminar" onclick="confirmarEliminacion('${esc(p.codigo)}')" title="Eliminar Producto">
                    <i class="fas fa-trash-alt"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

async function confirmarEliminacion(codigo) {
    const result = await Swal.fire({
        title: '¿Dar de baja este producto?',
        text: `El producto ${codigo} dejará de aparecer en el inventario. Su historial de movimientos se conserva y podrá reactivarse después.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#444',
        confirmButtonText: '<i class="fas fa-trash-alt"></i> Sí, dar de baja',
        cancelButtonText: 'Cancelar',
    });
    if (!result.isConfirmed) return;

    try {
        await apiFetch('EliminarProductoServlet', {
            method: 'POST',
            body: JSON.stringify({ codigo }),
        });
        Swal.fire({
            title: 'Producto dado de baja',
            text: `El producto ${codigo} ha sido retirado del inventario activo.`,
            icon: 'success',
            confirmButtonColor: '#f1c40f',
        });
        searchEliminar.value = '';
        cargarProductos();
    } catch (err) {
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

cargarProductos();
