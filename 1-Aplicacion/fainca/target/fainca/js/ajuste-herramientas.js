// Ajuste de la bodega de herramientas: sumar o restar unidades y actualizar la
// observacion de cada una. Todas las filas viajan en UNA peticion, asi el
// historial lo muestra como un solo movimiento con su lista.
const tbodyAjusteHerr = document.getElementById('tbodyAjusteHerr');

// Lo que el servidor dice hoy de cada fila, para mostrar el disponible actual y
// precargar la observacion (que se edita, no se vuelve a escribir desde cero).
const estadoFilas = {};
let contadorFilas = 0;

function agregarFilaAjuste() {
    const id = ++contadorFilas;
    const fila = document.createElement('tr');
    fila.dataset.fila = id;
    fila.innerHTML = `
        <td class="autocomplete-cell">
            <input type="text" name="nombre" class="input-table" placeholder="Escriba para buscar..." required autocomplete="off">
            <div class="autocomplete-list" style="display:none;"></div>
        </td>
        <td style="text-align:center;" class="celda-disponible"><span style="color:#bbb;">—</span></td>
        <td>
            <select name="operacion" class="input-table">
                <option value="1">Sumar (+)</option>
                <option value="-1">Restar (−)</option>
            </select>
        </td>
        <td>
            <input type="number" name="cantidad" class="input-table" min="0" placeholder="0">
        </td>
        <td>
            <input type="text" name="observacion" class="input-table"
                   placeholder="Marca, estado, si le falta algo..." autocomplete="off">
        </td>
        <td style="text-align: center;">
            <button type="button" class="btn-remove-row" onclick="eliminarFilaAjuste(this)"><i class="fas fa-times"></i></button>
        </td>`;
    tbodyAjusteHerr.appendChild(fila);
}

function eliminarFilaAjuste(boton) {
    if (tbodyAjusteHerr.rows.length > 1) {
        const fila = boton.closest('tr');
        delete estadoFilas[fila.dataset.fila];
        fila.remove();
    } else {
        Swal.fire({
            title: 'Operación no permitida',
            text: 'Debe quedar al menos una herramienta en el ajuste.',
            icon: 'warning',
            confirmButtonColor: '#f1c40f',
        });
    }
}

// --- Autocompletado contra el catalogo ---
let temporizadorAjAC = null;

tbodyAjusteHerr.addEventListener('input', (e) => {
    if (e.target.name !== 'nombre') return;
    const input = e.target;
    const lista = input.parentElement.querySelector('.autocomplete-list');
    // Si reescriben el nombre, lo que se sabia de la fila deja de valer.
    delete estadoFilas[input.closest('tr').dataset.fila];
    pintarDisponible(input.closest('tr'), null);

    clearTimeout(temporizadorAjAC);
    const q = input.value.trim();
    if (!q) { lista.style.display = 'none'; return; }
    temporizadorAjAC = setTimeout(async () => {
        try {
            const items = await apiFetch(`HerramientasServlet?q=${encodeURIComponent(q)}`);
            if (items.length === 0) {
                lista.innerHTML = '<div class="autocomplete-vacio"><i class="fas fa-info-circle"></i> No hay herramientas con ese nombre. Regístrela primero en "Herramientas".</div>';
            } else {
                lista.innerHTML = items.slice(0, 8).map((h) => `
                    <div class="autocomplete-item" data-nombre="${esc(h.nombre)}">
                        <strong>${esc(h.nombre)}</strong>
                        <span class="ac-desc">${h.tipo === 'consumible' ? 'consumible' : 'herramienta'}${h.observaciones ? ' — ' + esc(h.observaciones) : ''}</span>
                        <span class="ac-stock">${h.disponible} disp.</span>
                    </div>`).join('');
            }
            lista.style.display = 'block';
        } catch (err) { /* sin sugerencias si falla la busqueda */ }
    }, 180);
});

async function elegirHerramienta(fila, nombre) {
    fila.querySelector('input[name="nombre"]').value = nombre;
    fila.querySelector('.autocomplete-list').style.display = 'none';
    try {
        const items = await apiFetch(`HerramientasServlet?q=${encodeURIComponent(nombre)}`);
        const h = items.find((x) => x.nombre === nombre);
        if (!h) return;
        estadoFilas[fila.dataset.fila] = { disponible: h.disponible, observacion: h.observaciones || '' };
        pintarDisponible(fila, h.disponible);
        // Se precarga la observacion actual para editarla, no para reescribirla.
        fila.querySelector('input[name="observacion"]').value = h.observaciones || '';
    } catch (err) { /* si falla, la fila sigue usable: el servidor valida igual */ }
}

function pintarDisponible(fila, disponible) {
    fila.querySelector('.celda-disponible').innerHTML =
        disponible === null || disponible === undefined
            ? '<span style="color:#bbb;">—</span>'
            : `<strong>${disponible}</strong>`;
}

tbodyAjusteHerr.addEventListener('mousedown', (e) => {
    const item = e.target.closest('.autocomplete-item');
    if (!item) return;
    e.preventDefault();
    const fila = item.closest('tr');
    elegirHerramienta(fila, item.dataset.nombre)
        .then(() => fila.querySelector('input[name="cantidad"]').focus());
});

tbodyAjusteHerr.addEventListener('keydown', (e) => {
    if (e.target.name !== 'nombre') return;
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
        const fila = e.target.closest('tr');
        elegirHerramienta(fila, elegido.dataset.nombre)
            .then(() => fila.querySelector('input[name="cantidad"]').focus());
    } else if (e.key === 'Escape') {
        lista.style.display = 'none';
    }
});

document.addEventListener('click', (e) => {
    if (!e.target.closest('.autocomplete-cell')) {
        document.querySelectorAll('.autocomplete-list').forEach((l) => { l.style.display = 'none'; });
    }
});
tbodyAjusteHerr.addEventListener('focusout', (e) => {
    if (e.target.name !== 'nombre') return;
    const lista = e.target.parentElement.querySelector('.autocomplete-list');
    setTimeout(() => { lista.style.display = 'none'; }, 150);
});

agregarFilaAjuste(); // fila inicial

async function procesarAjusteHerr() {
    const form = document.getElementById('ajusteForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const motivo = document.getElementById('aj-motivo').value.trim();
    const lineas = [];
    const sinCambio = [];

    for (const tr of tbodyAjusteHerr.rows) {
        const nombre = tr.querySelector('input[name="nombre"]').value.trim();
        if (!nombre) continue;
        const signo = Number(tr.querySelector('select[name="operacion"]').value);
        const cantidad = Number(tr.querySelector('input[name="cantidad"]').value || 0);
        const observacion = tr.querySelector('input[name="observacion"]').value.trim();
        const previa = estadoFilas[tr.dataset.fila];

        const delta = signo * cantidad;
        // La observacion solo se manda si de verdad cambio: asi no se llena el
        // historial de "ediciones" que no cambiaron nada.
        const cambiaObs = previa !== undefined && observacion !== previa.observacion;

        if (delta === 0 && !cambiaObs) { sinCambio.push(nombre); continue; }
        lineas.push({ nombre, delta, observacion: cambiaObs ? observacion : undefined });
    }

    if (lineas.length === 0) {
        Swal.fire({
            title: 'No hay nada que ajustar',
            text: sinCambio.length
                ? 'Indique una cantidad a sumar o restar, o cambie la observación.'
                : 'Agregue al menos una herramienta.',
            icon: 'info', confirmButtonColor: '#f1c40f',
        });
        return;
    }

    const resumen = lineas.map((l) => {
        const partes = [];
        if (l.delta !== 0) partes.push(`${l.delta > 0 ? '+' : ''}${l.delta} unidad(es)`);
        if (l.observacion !== undefined) partes.push('nueva observación');
        return `<b>${esc(l.nombre)}</b>: ${partes.join(' y ')}`;
    }).join('<br>');

    const confirmacion = await Swal.fire({
        title: '¿Guardar el ajuste?',
        html: `<div style="text-align:left;">${resumen}</div>`
            + (sinCambio.length ? `<div style="color:#999; margin-top:10px; font-size:0.85rem;">
                   Se omiten ${sinCambio.length} fila(s) sin cambios.</div>` : ''),
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Sí, guardar',
        cancelButtonText: 'Cancelar',
    });
    if (!confirmacion.isConfirmed) return;

    try {
        const actualizadas = await apiFetch('AjusteHerramientasServlet', {
            method: 'POST',
            body: JSON.stringify({ motivo, lineas }),
        });
        await Swal.fire({
            title: 'Ajuste guardado',
            html: '<div style="text-align:left;">' + actualizadas.map((h) =>
                `<b>${esc(h.nombre)}</b>: disponible <b>${h.disponible}</b> · total <b>${h.total}</b>`
            ).join('<br>') + '</div>',
            icon: 'success', confirmButtonColor: '#f1c40f',
        });
        form.reset();
        tbodyAjusteHerr.innerHTML = '';
        Object.keys(estadoFilas).forEach((k) => delete estadoFilas[k]);
        agregarFilaAjuste();
    } catch (err) {
        // El ajuste es todo o nada: si falla, no se aplico ninguna linea.
        Swal.fire({
            title: 'No se guardó el ajuste',
            text: err.message,
            icon: 'error',
            confirmButtonColor: '#f1c40f',
        });
    }
}
