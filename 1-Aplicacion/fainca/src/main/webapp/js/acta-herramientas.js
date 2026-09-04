// Nueva acta de entrega: varias lineas, un solo documento. A diferencia de los
// movimientos de productos, el acta se registra COMPLETA o no se registra
// (es un documento que se firma, no puede quedar a medias).
const tbodyActa = document.getElementById('tbodyActa');

function filaActaHTML() {
    return `
        <td class="autocomplete-cell">
            <input type="text" name="nombre" class="input-table" placeholder="Escriba para buscar en el catálogo..." required autocomplete="off">
            <div class="autocomplete-list" style="display:none;"></div>
        </td>
        <td>
            <input type="number" name="cantidad" class="input-table" placeholder="Cant." min="1" required>
        </td>
        <td>
            <input type="text" name="observacion" class="input-table" placeholder="Opcional" autocomplete="off">
        </td>
        <td style="text-align: center;">
            <button type="button" class="btn-remove-row" onclick="eliminarFilaActa(this)"><i class="fas fa-times"></i></button>
        </td>`;
}

function agregarFilaActa() {
    const fila = document.createElement('tr');
    fila.innerHTML = filaActaHTML();
    tbodyActa.appendChild(fila);
}

function eliminarFilaActa(boton) {
    if (tbodyActa.rows.length > 1) {
        boton.closest('tr').remove();
    } else {
        Swal.fire({
            title: 'Operación no permitida',
            text: 'El acta debe tener al menos una línea.',
            icon: 'warning',
            confirmButtonColor: '#f1c40f',
        });
    }
}

// --- Autocompletado contra el catalogo de herramientas ---
let temporizadorActaAC = null;

tbodyActa.addEventListener('input', (e) => {
    if (e.target.name !== 'nombre') return;
    const input = e.target;
    const lista = input.parentElement.querySelector('.autocomplete-list');
    clearTimeout(temporizadorActaAC);
    const q = input.value.trim();
    if (!q) { lista.style.display = 'none'; return; }
    temporizadorActaAC = setTimeout(async () => {
        try {
            const items = await apiFetch(`HerramientasServlet?q=${encodeURIComponent(q)}`);
            renderSugerenciasActa(lista, items);
        } catch (err) { /* sin sugerencias si falla la busqueda */ }
    }, 180);
});

function renderSugerenciasActa(lista, items) {
    if (items.length === 0) {
        lista.innerHTML = '<div class="autocomplete-vacio"><i class="fas fa-info-circle"></i> No hay herramientas con ese nombre. Regístrela primero en "Herramientas".</div>';
        lista.style.display = 'block';
        return;
    }
    lista.innerHTML = items.slice(0, 8).map((h) => `
        <div class="autocomplete-item" data-nombre="${esc(h.nombre)}">
            <strong>${esc(h.nombre)}</strong>
            <span class="ac-desc">${h.tipo === 'consumible' ? 'consumible' : 'herramienta'}${h.observaciones ? ' — ' + esc(h.observaciones) : ''}</span>
            <span class="ac-stock">${h.disponible} disp.</span>
        </div>
    `).join('');
    lista.style.display = 'block';
}

tbodyActa.addEventListener('mousedown', (e) => {
    const item = e.target.closest('.autocomplete-item');
    if (!item) return;
    e.preventDefault();
    const celda = item.closest('.autocomplete-cell');
    celda.querySelector('input[name="nombre"]').value = item.dataset.nombre;
    celda.querySelector('.autocomplete-list').style.display = 'none';
    celda.closest('tr').querySelector('input[name="cantidad"]').focus();
});

tbodyActa.addEventListener('keydown', (e) => {
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
        e.target.value = elegido.dataset.nombre;
        lista.style.display = 'none';
        e.target.closest('tr').querySelector('input[name="cantidad"]').focus();
    } else if (e.key === 'Escape') {
        lista.style.display = 'none';
    }
});

document.addEventListener('click', (e) => {
    if (!e.target.closest('.autocomplete-cell')) {
        document.querySelectorAll('.autocomplete-list').forEach((l) => { l.style.display = 'none'; });
    }
});
tbodyActa.addEventListener('focusout', (e) => {
    if (e.target.name !== 'nombre') return;
    const lista = e.target.parentElement.querySelector('.autocomplete-list');
    setTimeout(() => { lista.style.display = 'none'; }, 150);
});

agregarFilaActa(); // linea inicial

async function procesarActa() {
    const form = document.getElementById('actaForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const solicitante = document.getElementById('acta-solicitante').value.trim();
    const proyecto = document.getElementById('acta-proyecto').value.trim();
    const destino = document.getElementById('acta-destino').value.trim();
    const lineas = Array.from(tbodyActa.rows).map((tr) => ({
        nombre: tr.querySelector('input[name="nombre"]').value.trim(),
        cantidad: Number(tr.querySelector('input[name="cantidad"]').value),
        observacion: tr.querySelector('input[name="observacion"]').value.trim() || undefined,
    }));

    const confirmacion = await Swal.fire({
        title: '¿Registrar el acta?',
        html: `Se entregarán <b>${lineas.length}</b> línea(s) a <b>${esc(solicitante)}</b>`
            + ` para <b>${esc(proyecto)}</b>.<br>El stock disponible se descuenta al confirmar.`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Sí, registrar',
        cancelButtonText: 'Cancelar',
    });
    if (!confirmacion.isConfirmed) return;

    try {
        const acta = await apiFetch('ActaHerramientasServlet', {
            method: 'POST',
            body: JSON.stringify({
                solicitante,
                proyecto,
                destino: destino || undefined,
                observaciones: document.getElementById('acta-obs').value.trim() || undefined,
                lineas,
            }),
        });

        const resultado = await Swal.fire({
            title: `Acta ${acta.numero} registrada`,
            html: `<b>${acta.items}</b> ítem(s) · <b>${acta.unidades}</b> unidad(es)`
                + (acta.estado === 'cerrada'
                    ? '<br><span style="color:#777;">Solo consumibles: el acta quedó cerrada de una vez.</span>'
                    : '<br><span style="color:#777;">Quedó ABIERTA hasta que devuelvan las herramientas.</span>'),
            icon: 'success',
            showCancelButton: true,
            confirmButtonColor: '#f1c40f',
            cancelButtonColor: '#444',
            confirmButtonText: '<i class="fas fa-file-pdf"></i> Imprimir para firma',
            cancelButtonText: 'Seguir sin imprimir',
        });
        if (resultado.isConfirmed) {
            window.location.href = `ExportarActaServlet?id=${acta.id}`;
        }

        form.reset();
        tbodyActa.innerHTML = '';
        agregarFilaActa();
    } catch (err) {
        // El acta es todo-o-nada: si una linea falla, no se registro nada.
        Swal.fire({
            title: 'No se registró el acta',
            text: err.message,
            icon: 'error',
            confirmButtonColor: '#f1c40f',
        });
    }
}
