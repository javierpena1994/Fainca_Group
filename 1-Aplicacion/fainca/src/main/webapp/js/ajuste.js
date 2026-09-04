// Ajuste de inventario por reconteo fisico, de varios productos a la vez.
// Todas las filas se registran en UN solo documento del historial, con un unico motivo.
const tbodyAjuste = document.getElementById('tbodyAjuste');

// Estado por fila: se guarda lo que dice el sistema para poder calcular la diferencia
// y saber si la ubicacion cambio. Se indexa con un numero de fila propio.
let secuencia = 0;
const estado = {};   // { [id]: { stock: n|null, ubicacion: string|null } }

function filaHTML(id) {
    return `
        <td class="autocomplete-cell" style="position:relative;">
            <input type="text" class="input-table aj-codigo" data-fila="${id}"
                   placeholder="Escriba para buscar el producto..." required autocomplete="off">
            <div class="autocomplete-list" style="display:none;"></div>
        </td>
        <td style="text-align:center;">
            <input type="text" class="input-table aj-stock" data-fila="${id}" value="—" disabled
                   style="background:#f4f4f4; font-weight:bold; text-align:center;">
        </td>
        <td>
            <input type="number" class="input-table aj-real" data-fila="${id}" min="0"
                   placeholder="Contado" required style="text-align:center;">
        </td>
        <td style="text-align:center;">
            <input type="text" class="input-table aj-dif" data-fila="${id}" value="—" disabled
                   style="background:#f4f4f4; font-weight:bold; text-align:center;">
        </td>
        <td>
            <input type="text" class="input-table aj-ubicacion" data-fila="${id}" placeholder="Ej: AA03">
        </td>
        <td style="text-align:center;">
            <button type="button" class="btn-eliminar-fila" onclick="eliminarFila(this)" title="Quitar">
                <i class="fas fa-times"></i>
            </button>
        </td>`;
}

function agregarFila() {
    const id = ++secuencia;
    estado[id] = { stock: null, ubicacion: null };
    const tr = document.createElement('tr');
    tr.innerHTML = filaHTML(id);
    tbodyAjuste.appendChild(tr);
}

function eliminarFila(boton) {
    if (tbodyAjuste.rows.length > 1) {
        boton.closest('tr').remove();
    } else {
        Swal.fire({
            title: 'Operación no permitida',
            text: 'Debe quedar al menos un producto para registrar el ajuste.',
            icon: 'warning',
            confirmButtonColor: '#f1c40f',
        });
    }
}

agregarFila(); // fila inicial

// --- Autocompletado (delegado: las filas se crean sobre la marcha) ---
let temporizador = null;
tbodyAjuste.addEventListener('input', (e) => {
    if (e.target.classList.contains('aj-real')) { actualizarDiferencia(e.target.dataset.fila); return; }
    if (!e.target.classList.contains('aj-codigo')) return;

    const id = e.target.dataset.fila;
    estado[id] = { stock: null, ubicacion: null };
    fila(id, 'aj-stock').value = '—';
    fila(id, 'aj-ubicacion').value = '';
    fila(id, 'aj-real').value = '';
    actualizarDiferencia(id);

    const lista = e.target.parentElement.querySelector('.autocomplete-list');
    const q = e.target.value.trim();
    clearTimeout(temporizador);
    if (!q) { lista.style.display = 'none'; return; }
    temporizador = setTimeout(async () => {
        try {
            const productos = await apiFetch(`BuscarProductosServlet?q=${encodeURIComponent(q)}`);
            lista.innerHTML = productos.length === 0
                ? '<div class="autocomplete-vacio"><i class="fas fa-info-circle"></i> No hay productos con ese código.</div>'
                : productos.slice(0, 8).map((p) => `
                    <div class="autocomplete-item" data-codigo="${esc(p.codigo)}"
                         data-stock="${p.stock_actual}" data-ubicacion="${esc(p.ubicacion || '')}">
                        <strong>${esc(p.codigo)}</strong>
                        <span class="ac-desc">${esc(p.marca)}${p.descripcion ? ' — ' + esc(p.descripcion) : ''}</span>
                        <span class="ac-stock">${p.stock_actual} ${esc(p.unidad_medida)}</span>
                    </div>`).join('');
            lista.style.display = 'block';
        } catch (err) { /* sin lista si falla */ }
    }, 180);
});

tbodyAjuste.addEventListener('mousedown', (e) => {
    const item = e.target.closest('.autocomplete-item');
    if (!item) return;
    e.preventDefault();
    const celda = item.closest('.autocomplete-cell');
    const input = celda.querySelector('.aj-codigo');
    seleccionarProducto(input.dataset.fila, item.dataset.codigo,
        Number(item.dataset.stock), item.dataset.ubicacion);
    celda.querySelector('.autocomplete-list').style.display = 'none';
});

// Si escriben el codigo exacto sin usar la lista, se consulta al salir del campo
tbodyAjuste.addEventListener('focusout', async (e) => {
    if (!e.target.classList.contains('aj-codigo')) return;
    const lista = e.target.parentElement.querySelector('.autocomplete-list');
    setTimeout(() => { lista.style.display = 'none'; }, 150);

    const id = e.target.dataset.fila;
    const codigo = e.target.value.trim();
    if (!codigo || estado[id].stock !== null) return;
    try {
        const p = await apiFetch(`ProductosServlet?codigo=${encodeURIComponent(codigo)}`);
        seleccionarProducto(id, p.codigo, p.stock_actual, p.ubicacion || '');
    } catch (err) { /* codigo inexistente: se valida al procesar */ }
});

function fila(id, clase) {
    return tbodyAjuste.querySelector(`.${clase}[data-fila="${id}"]`);
}

function seleccionarProducto(id, codigo, stock, ubicacion) {
    fila(id, 'aj-codigo').value = codigo;
    estado[id] = { stock, ubicacion: ubicacion || '' };
    fila(id, 'aj-stock').value = stock;
    fila(id, 'aj-ubicacion').value = ubicacion || '';
    // Se precarga con el stock actual: si solo se corrige la ubicación, no hay que tocarlo.
    fila(id, 'aj-real').value = stock;
    actualizarDiferencia(id);
}

function actualizarDiferencia(id) {
    const campo = fila(id, 'aj-dif');
    const real = fila(id, 'aj-real');
    if (!campo || !real) return;
    const st = estado[id] ? estado[id].stock : null;
    if (st === null || real.value === '') {
        campo.value = '—';
        campo.style.color = '';
        return;
    }
    const dif = Number(real.value) - st;
    if (dif === 0) {
        campo.value = 'Sin cambio';
        campo.style.color = '#777';
    } else {
        campo.value = (dif > 0 ? '+' : '') + dif;
        campo.style.color = dif > 0 ? '#28a745' : '#dc3545';
    }
}

// --- Procesar todo el reconteo ---
async function procesarAjuste() {
    const form = document.getElementById('ajusteForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    // Se arma una linea por fila, descartando las que no cambian nada.
    const productos = [];
    const resumen = [];
    let sinCambios = 0;

    for (const tr of tbodyAjuste.rows) {
        const inputCodigo = tr.querySelector('.aj-codigo');
        const id = inputCodigo.dataset.fila;
        const codigo = inputCodigo.value.trim();
        if (!codigo) continue;

        const real = Number(tr.querySelector('.aj-real').value);
        const ubicacionNueva = tr.querySelector('.aj-ubicacion').value.trim();
        const st = estado[id].stock;
        const ubicacionActual = estado[id].ubicacion;

        const dif = st === null ? null : real - st;
        const cambiaUbicacion = ubicacionActual !== null && ubicacionNueva !== ubicacionActual;

        if (dif === 0 && !cambiaUbicacion) { sinCambios++; continue; }

        const linea = { producto_codigo: codigo, cantidad_real: real };
        // Solo se envía si se llegó a cargar el producto, para no borrar la ubicación
        // por accidente cuando el código no se pudo consultar.
        if (ubicacionActual !== null) linea.ubicacion = ubicacionNueva;
        productos.push(linea);

        if (dif === null) {
            resumen.push(`<b>${esc(codigo)}</b>: quedará en <b>${real}</b>`);
        } else if (dif !== 0) {
            resumen.push(`<b>${esc(codigo)}</b>: ${st} → <b>${real}</b> `
                + `(<span style="color:${dif > 0 ? '#28a745' : '#dc3545'};">${dif > 0 ? '+' : ''}${dif}</span>)`);
        } else {
            resumen.push(`<b>${esc(codigo)}</b>: solo cambia la ubicación a <b>${esc(ubicacionNueva) || '(sin ubicación)'}</b>`);
        }
    }

    if (productos.length === 0) {
        Swal.fire({
            title: 'Nada que ajustar',
            text: sinCambios > 0
                ? 'Las cantidades y ubicaciones ya coinciden con el sistema.'
                : 'Agregue al menos un producto.',
            icon: 'info',
            confirmButtonColor: '#f1c40f',
        });
        return;
    }

    const confirmacion = await Swal.fire({
        title: `¿Registrar ajuste de ${productos.length} producto(s)?`,
        html: resumen.join('<br>') + (sinCambios ? `<br><br><small style="color:#777;">${sinCambios} fila(s) sin cambios se omitirán.</small>` : ''),
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Sí, ajustar',
        cancelButtonText: 'Cancelar',
    });
    if (!confirmacion.isConfirmed) return;

    try {
        // Sin fecha: el ajuste siempre queda con la fecha/hora del momento (el servidor
        // lo impone aunque alguien mande una fecha a mano).
        const r = await apiFetch('AjusteServlet', {
            method: 'POST',
            body: JSON.stringify({
                observaciones: document.getElementById('aj-obs').value.trim(),
                productos,
            }),
        });

        const ok = [];
        const fallos = [];
        for (const x of r.resultados || []) {
            if (x.ok) {
                ok.push(`${x.codigo}: ${x.stock_anterior} → ${x.stock_actual}`
                    + (x.diferencia !== 0 ? ` (${x.diferencia > 0 ? '+' : ''}${x.diferencia})` : ' (solo ubicación)'));
            } else {
                fallos.push(`${x.codigo}: ${x.error}`);
            }
        }

        await Swal.fire({
            title: fallos.length ? 'Ajuste registrado con observaciones' : 'Ajuste registrado',
            html: [
                ok.length ? `<b style="color:#28a745;">Ajustados:</b><br>${ok.map(esc).join('<br>')}` : '',
                fallos.length ? `<b style="color:#dc3545;">Con error:</b><br>${fallos.map(esc).join('<br>')}` : '',
                '<br><small style="color:#777;">Quedó como un solo documento en el historial.</small>',
            ].filter(Boolean).join('<br><br>'),
            icon: fallos.length ? 'warning' : 'success',
            confirmButtonColor: '#f1c40f',
        });

        // Se deja la pantalla lista para el siguiente reconteo
        form.reset();
        tbodyAjuste.innerHTML = '';
        for (const k of Object.keys(estado)) delete estado[k];
        agregarFila();
    } catch (err) {
        Swal.fire({ title: 'No se pudo ajustar', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}
