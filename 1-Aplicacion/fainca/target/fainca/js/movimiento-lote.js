// Compartido por ingresoInventario.jsp y salidaInventario.jsp. El tipo de
// movimiento (ingreso/egreso) se lee del atributo data-tipo del <body>.

const TIPO = document.body.dataset.tipo; // 'ingreso' | 'egreso'
const ES_INGRESO = TIPO === 'ingreso';
const tbodyProductos = document.getElementById('tbodyProductos');

// La fecha solo puede retroceder hasta 2 dias (el servidor tambien lo valida).
// Vacia = fecha y hora actual. Se arma la cadena a mano y no con toISOString(),
// porque toISOString() usa UTC y aqui (UTC-5) por la noche daria el dia siguiente.
(function limitarFecha() {
    const inputFecha = document.getElementById('mov-fecha');
    if (!inputFecha) return;
    const texto = (d) => d.getFullYear() + '-'
        + String(d.getMonth() + 1).padStart(2, '0') + '-'
        + String(d.getDate()).padStart(2, '0');
    const hoy = new Date();
    const limite = new Date(hoy);
    limite.setDate(hoy.getDate() - 2);
    inputFecha.max = texto(hoy);
    inputFecha.min = texto(limite);
})();

function filaHTML() {
    return `
        <td class="autocomplete-cell">
            <input type="text" name="codigo" class="input-table" placeholder="Escriba para buscar el producto..." required autocomplete="off">
            <div class="autocomplete-list" style="display:none;"></div>
        </td>
        <td>
            <input type="number" name="cantidad" class="input-table" placeholder="Cantidad" min="1" required>
        </td>
        <td style="text-align: center;">
            <button type="button" class="btn-remove-row" onclick="eliminarFila(this)"><i class="fas fa-times"></i></button>
        </td>`;
}

// --- Autocompletado: el campo de codigo busca productos ya registrados ---
let temporizadorAC = null;

tbodyProductos.addEventListener('input', (e) => {
    if (e.target.name !== 'codigo') return;
    const input = e.target;
    const lista = input.parentElement.querySelector('.autocomplete-list');
    clearTimeout(temporizadorAC);
    const q = input.value.trim();
    if (!q) { lista.style.display = 'none'; return; }
    temporizadorAC = setTimeout(async () => {
        try {
            const productos = await apiFetch(`BuscarProductosServlet?q=${encodeURIComponent(q)}`);
            renderSugerencias(lista, productos);
        } catch (err) { /* si falla la busqueda, simplemente no se muestra la lista */ }
    }, 180);
});

function renderSugerencias(lista, productos) {
    if (productos.length === 0) {
        lista.innerHTML = '<div class="autocomplete-vacio"><i class="fas fa-info-circle"></i> No hay productos registrados con ese código. Regístrelo primero en "Registrar productos".</div>';
        lista.style.display = 'block';
        return;
    }
    lista.innerHTML = productos.slice(0, 8).map((p) => `
        <div class="autocomplete-item" data-codigo="${esc(p.codigo)}">
            <strong>${esc(p.codigo)}</strong>
            <span class="ac-desc">${esc(p.marca)}${p.descripcion ? ' — ' + esc(p.descripcion) : ''}</span>
            <span class="ac-stock">${p.stock_actual} ${esc(p.unidad_medida)}</span>
        </div>
    `).join('');
    lista.style.display = 'block';
}

// Seleccion con clic (mousedown para ganarle al blur del input)
tbodyProductos.addEventListener('mousedown', (e) => {
    const item = e.target.closest('.autocomplete-item');
    if (!item) return;
    e.preventDefault();
    const celda = item.closest('.autocomplete-cell');
    celda.querySelector('input[name="codigo"]').value = item.dataset.codigo;
    celda.querySelector('.autocomplete-list').style.display = 'none';
    // pasar el foco a la cantidad de la misma fila
    celda.closest('tr').querySelector('input[name="cantidad"]').focus();
});

// Navegacion con teclado: flechas + Enter, Escape para cerrar
tbodyProductos.addEventListener('keydown', (e) => {
    if (e.target.name !== 'codigo') return;
    const lista = e.target.parentElement.querySelector('.autocomplete-list');
    if (lista.style.display === 'none') return;
    const items = Array.from(lista.querySelectorAll('.autocomplete-item'));
    if (items.length === 0) { if (e.key === 'Escape') lista.style.display = 'none'; return; }

    const actual = items.findIndex((i) => i.classList.contains('seleccionado'));
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        const siguiente = e.key === 'ArrowDown'
            ? (actual + 1) % items.length
            : (actual - 1 + items.length) % items.length;
        items.forEach((i) => i.classList.remove('seleccionado'));
        items[siguiente].classList.add('seleccionado');
        items[siguiente].scrollIntoView({ block: 'nearest' });
    } else if (e.key === 'Enter') {
        e.preventDefault();
        const elegido = actual >= 0 ? items[actual] : items[0];
        e.target.value = elegido.dataset.codigo;
        lista.style.display = 'none';
        e.target.closest('tr').querySelector('input[name="cantidad"]').focus();
    } else if (e.key === 'Escape') {
        lista.style.display = 'none';
    }
});

// Cerrar las listas al hacer clic fuera o salir del campo
document.addEventListener('click', (e) => {
    if (!e.target.closest('.autocomplete-cell')) {
        document.querySelectorAll('.autocomplete-list').forEach((l) => { l.style.display = 'none'; });
    }
});
tbodyProductos.addEventListener('focusout', (e) => {
    if (e.target.name !== 'codigo') return;
    const lista = e.target.parentElement.querySelector('.autocomplete-list');
    setTimeout(() => { lista.style.display = 'none'; }, 150);
});

function agregarFila() {
    const fila = document.createElement('tr');
    fila.innerHTML = filaHTML();
    tbodyProductos.appendChild(fila);
}

function eliminarFila(boton) {
    if (tbodyProductos.rows.length > 1) {
        boton.closest('tr').remove();
    } else {
        Swal.fire({
            title: 'Operación no permitida',
            text: 'Debe ingresar al menos un producto para registrar el movimiento.',
            icon: 'warning',
            confirmButtonColor: '#f1c40f',
        });
    }
}

agregarFila(); // fila inicial

async function procesarFormulario() {
    const form = document.getElementById('movimientoForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const filas = Array.from(tbodyProductos.rows).map((tr) => ({
        codigo: tr.querySelector('input[name="codigo"]').value.trim(),
        cantidad: Number(tr.querySelector('input[name="cantidad"]').value),
    }));
    const observaciones = document.getElementById('mov-obs').value.trim();
    const fecha = document.getElementById('mov-fecha').value;

    const confirmacion = await Swal.fire({
        title: ES_INGRESO ? '¿Procesar Ingreso?' : '¿Confirmar Salida de Material?',
        text: ES_INGRESO
            ? 'Se sumarán las cantidades especificadas a los productos correspondientes en el inventario.'
            : 'Se restarán las cantidades especificadas del stock actual. Esta acción quedará registrada en el historial.',
        icon: ES_INGRESO ? 'question' : 'warning',
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: ES_INGRESO ? 'Sí, procesar' : 'Sí, procesar salida',
        cancelButtonText: 'Cancelar',
    });
    if (!confirmacion.isConfirmed) return;

    // TODAS las filas viajan en UNA sola peticion: el servidor les asigna un mismo
    // numero de documento y en el historial aparecen como un solo movimiento con su
    // lista de productos, en vez de un registro suelto por cada uno.
    // Si una linea falla (stock insuficiente, codigo inexistente), las demas se
    // registran igual y el servidor devuelve el detalle de cada una.
    const exitos = [];
    const errores = [];
    try {
        const data = await apiFetch('MovimientoServlet', {
            method: 'POST',
            body: JSON.stringify({
                tipo: TIPO,
                observaciones,
                fecha: fecha || undefined,
                productos: filas.map((f) => ({ producto_codigo: f.codigo, cantidad: f.cantidad })),
            }),
        });
        for (const r of data.resultados || []) {
            if (r.ok) {
                exitos.push(`${r.codigo}: ${ES_INGRESO ? '+' : '-'}${r.cantidad} → stock ${r.stock_actual}`);
            } else {
                errores.push(`${r.codigo}: ${r.error}`);
            }
        }
    } catch (err) {
        // Falla global (ninguna linea se pudo registrar)
        errores.push(err.message);
    }

    const resumen = [
        exitos.length ? `<b style="color:#28a745;">Procesados:</b><br>${exitos.map(esc).join('<br>')}` : '',
        errores.length ? `<b style="color:#dc3545;">Con error:</b><br>${errores.map(esc).join('<br>')}` : '',
    ].filter(Boolean).join('<br><br>');

    await Swal.fire({
        title: errores.length === 0
            ? (ES_INGRESO ? '¡Éxito!' : '¡Despacho Exitoso!')
            : (exitos.length ? 'Procesado con observaciones' : 'No se pudo procesar'),
        html: `<div style="text-align:left;">${resumen}</div>`,
        icon: errores.length === 0 ? 'success' : (exitos.length ? 'warning' : 'error'),
        confirmButtonColor: '#f1c40f',
    });

    if (exitos.length) {
        form.reset();
        tbodyProductos.innerHTML = '';
        agregarFila();
    }
}
