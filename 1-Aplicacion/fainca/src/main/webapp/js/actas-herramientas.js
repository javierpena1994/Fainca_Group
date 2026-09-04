// Actas de entrega: lista con filtros, detalle desplegable y registro de
// devoluciones por tandas (bien / dañado / perdido; lo demas sigue en proyecto).
const tbodyActas = document.getElementById('tbodyActas');
let temporizadorActas = null;

['a-estado', 'a-buscar', 'a-desde', 'a-hasta'].forEach((id) => {
    document.getElementById(id).addEventListener('input', () => {
        clearTimeout(temporizadorActas);
        temporizadorActas = setTimeout(cargarActas, 200);
    });
});

function filtrosActas() {
    const f = new URLSearchParams();
    const estado = document.getElementById('a-estado').value;
    const q = document.getElementById('a-buscar').value.trim();
    const desde = document.getElementById('a-desde').value;
    const hasta = document.getElementById('a-hasta').value;
    if (estado) f.set('estado', estado);
    if (q) f.set('q', q);
    if (desde) f.set('desde', desde);
    if (hasta) f.set('hasta', hasta);
    return f;
}

function formatearFechaActa(fechaSql) {
    if (!fechaSql) return '';
    const [f, h] = fechaSql.split(' ');
    const [anio, mes, dia] = f.split('-');
    return `${dia}/${mes}/${anio} ${h ? h.slice(0, 5) : ''}`;
}

function filaActa(a) {
    return `
        <tr class="fila-doc" data-id="${a.id}">
            <td style="text-align:center;">
                <button type="button" class="btn-detalles btn-desplegar" title="Ver el detalle">
                    <i class="fas fa-chevron-right"></i>
                </button>
            </td>
            <td><strong>${esc(a.numero)}</strong></td>
            <td style="white-space:nowrap;">${formatearFechaActa(a.fecha)}</td>
            <td>${esc(a.solicitante)}</td>
            <td>${esc(a.proyecto)}${a.destino
                ? `<div style="color:#999; font-size:0.8rem;">
                       <i class="fas fa-location-dot"></i> ${esc(a.destino)}</div>` : ''}</td>
            <td style="text-align:center;"><strong>${a.items}</strong></td>
            <td style="text-align:center;">${a.pendientes > 0
                ? `<strong style="color:#b7791f;">${a.pendientes}</strong>`
                : '<span style="color:#bbb;">0</span>'}</td>
            <td style="text-align:center;">
                <span class="badge-tipo ${esc(a.estado)}">${a.estado === 'cerrada' ? 'CERRADA' : 'ABIERTA'}</span>
            </td>
            <td style="text-align:center; white-space:nowrap;">
                <a class="btn-detalles" style="text-decoration:none; display:inline-block;"
                   href="ExportarActaServlet?id=${a.id}" title="Descargar el acta en PDF (para firma)">
                   <i class="fas fa-file-pdf"></i></a>
            </td>
        </tr>`;
}

async function cargarActas() {
    try {
        const actas = await apiFetch(`ActaHerramientasServlet?${filtrosActas().toString()}`);
        if (actas.length === 0) {
            tbodyActas.innerHTML = `
                <tr><td colspan="9" style="text-align:center; padding:30px; color:#777; font-weight:bold;">
                    <i class="fas fa-info-circle"></i> No hay actas con esos filtros.
                </td></tr>`;
            document.getElementById('pie-actas').innerHTML = '';
            return;
        }
        tbodyActas.innerHTML = actas.map(filaActa).join('');
        const abiertas = actas.filter((a) => a.estado === 'abierta').length;
        document.getElementById('pie-actas').innerHTML =
            `Mostrando <b>${actas.length}</b> acta(s)`
            + (abiertas ? ` · <b>${abiertas}</b> abierta(s)` : '');
    } catch (err) {
        tbodyActas.innerHTML = `
            <tr><td colspan="9" style="text-align:center; padding:30px; color:#dc3545;">${esc(err.message)}</td></tr>`;
    }
}

// --- Detalle desplegable + devolucion ---
tbodyActas.addEventListener('click', (e) => {
    const desplegar = e.target.closest('.btn-desplegar');
    if (desplegar) { alternarDetalleActa(desplegar); return; }

    const guardar = e.target.closest('.btn-guardar-devolucion');
    if (guardar) { registrarDevolucion(Number(guardar.dataset.id), guardar.closest('.fila-detalle')); }
});

// Resumen de como se ha ido saldando la linea (consumido / ok / danado / perdido)
function resumenSaldado(l) {
    if (l.consumido > 0) return `<span style="color:#777;">${l.consumido} consumido(s)</span>`;
    const partes = [];
    if (l.devueltoOk > 0) partes.push(`<span style="color:#1e7e34;">${l.devueltoOk} ok</span>`);
    if (l.devueltoDanado > 0) partes.push(`<span style="color:#b7791f;">${l.devueltoDanado} dañado(s)</span>`);
    if (l.perdido > 0) partes.push(`<span style="color:#c82333;">${l.perdido} perdido(s)</span>`);
    return partes.length ? partes.join(' · ') : '<span style="color:#bbb;">—</span>';
}

function filaLineaDetalle(l, abierta) {
    const conInputs = abierta && l.pendiente > 0;
    return `
        <tr data-linea="${l.id}">
            <td><strong>${esc(l.nombre)}</strong>${l.observacion
                ? `<div style="color:#999; font-size:0.8rem;">${esc(l.observacion)}</div>` : ''}</td>
            <td style="text-align:center;">
                <span class="badge-tipo ${esc(l.tipo)}">${l.tipo === 'consumible' ? 'CONSUMIBLE' : 'HERRAMIENTA'}</span>
            </td>
            <td style="text-align:center;"><strong>${l.cantidad}</strong></td>
            <td style="text-align:center;">${resumenSaldado(l)}</td>
            <td style="text-align:center;">${l.pendiente > 0
                ? `<strong style="color:#b7791f;">${l.pendiente}</strong>`
                : '<span style="color:#bbb;">0</span>'}</td>
            ${abierta ? `
            <td style="text-align:center;">${conInputs
                ? `<input type="number" name="dev-ok" class="input-table" style="width:70px;" min="0" max="${l.pendiente}" placeholder="0">`
                : ''}</td>
            <td style="text-align:center;">${conInputs
                ? `<input type="number" name="dev-danado" class="input-table" style="width:70px;" min="0" max="${l.pendiente}" placeholder="0">`
                : ''}</td>
            <td style="text-align:center;">${conInputs
                ? `<input type="number" name="dev-perdido" class="input-table" style="width:70px;" min="0" max="${l.pendiente}" placeholder="0">`
                : ''}</td>` : ''}
        </tr>`;
}

async function alternarDetalleActa(boton) {
    const filaActa = boton.closest('tr');
    const siguiente = filaActa.nextElementSibling;

    if (siguiente && siguiente.classList.contains('fila-detalle')) {
        siguiente.remove();
        boton.innerHTML = '<i class="fas fa-chevron-right"></i>';
        return;
    }

    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    try {
        const acta = await apiFetch(`ActaHerramientasServlet?id=${filaActa.dataset.id}`);
        const abierta = acta.estado === 'abierta';

        const filas = acta.lineas.map((l) => filaLineaDetalle(l, abierta)).join('');
        const encabezadoDevolucion = abierta
            ? '<th style="text-align:center; color:#1e7e34;">Vuelve OK</th>'
              + '<th style="text-align:center; color:#b7791f;">Dañado</th>'
              + '<th style="text-align:center; color:#c82333;">Perdido</th>'
            : '';

        filaActa.insertAdjacentHTML('afterend', `
            <tr class="fila-detalle">
                <td colspan="9" style="background:#fafafa; padding:14px 18px;">
                    <div style="font-weight:bold; color:#444; margin-bottom:8px;">
                        <i class="fas fa-list-ol"></i> Detalle de ${esc(acta.numero)}
                        <span style="font-weight:normal; color:#777;">
                            · ${acta.items} ítem(s) · ${acta.unidades} unidad(es)
                            · registrada por ${esc(acta.usuario)}</span>
                        <div style="font-weight:normal; color:#777; margin-top:4px;">
                            <i class="fas fa-diagram-project"></i> ${esc(acta.proyecto)}
                            ${acta.destino ? ` · <i class="fas fa-location-dot"></i> ${esc(acta.destino)}` : ''}
                        </div>
                        ${acta.observaciones ? `<div style="font-weight:normal; color:#777; margin-top:4px;">
                            <i class="fas fa-comment-alt"></i> ${esc(acta.observaciones)}</div>` : ''}
                    </div>
                    <table class="user-table" style="margin:0;">
                        <thead>
                            <tr>
                                <th>Herramienta</th>
                                <th style="text-align:center;">Tipo</th>
                                <th style="text-align:center;">Llevado</th>
                                <th style="text-align:center;">Saldado</th>
                                <th style="text-align:center;">Pendiente</th>
                                ${encabezadoDevolucion}
                            </tr>
                        </thead>
                        <tbody>${filas}</tbody>
                    </table>
                    ${abierta ? `
                    <div style="display:flex; gap:12px; align-items:center; flex-wrap:wrap; margin-top:12px;">
                        <input type="text" name="dev-obs" class="input-table" style="flex:1; min-width:240px;"
                               placeholder="Observación de esta devolución (opcional)" autocomplete="off">
                        <button type="button" class="btn-add-row btn-guardar-devolucion" style="margin:0;"
                                data-id="${acta.id}">
                            <i class="fas fa-rotate-left"></i> Registrar devolución
                        </button>
                    </div>
                    <div style="color:#999; font-size:0.82rem; margin-top:6px;">
                        Reporta solo lo que llega en esta tanda; lo demás sigue "en proyecto".
                        Si ya no queda nada pendiente, el acta se cierra sola.
                    </div>` : ''}
                </td>
            </tr>`);
        boton.innerHTML = '<i class="fas fa-chevron-down"></i>';
    } catch (err) {
        boton.innerHTML = '<i class="fas fa-chevron-right"></i>';
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

async function registrarDevolucion(actaId, contenedor) {
    const lineas = [];
    contenedor.querySelectorAll('tr[data-linea]').forEach((tr) => {
        const leer = (nombre) => {
            const input = tr.querySelector(`input[name="${nombre}"]`);
            return input ? Number(input.value || 0) : 0;
        };
        const linea = {
            linea_id: Number(tr.dataset.linea),
            ok: leer('dev-ok'),
            danado: leer('dev-danado'),
            perdido: leer('dev-perdido'),
        };
        if (linea.ok > 0 || linea.danado > 0 || linea.perdido > 0) lineas.push(linea);
    });

    if (lineas.length === 0) {
        Swal.fire({
            title: 'Nada que registrar',
            text: 'Escribe las cantidades que vuelven (OK, dañadas o perdidas) en alguna línea.',
            icon: 'info', confirmButtonColor: '#f1c40f',
        });
        return;
    }

    const perdidos = lineas.reduce((s, l) => s + l.perdido, 0);
    const confirmacion = await Swal.fire({
        title: '¿Registrar la devolución?',
        html: `Se reportan <b>${lineas.reduce((s, l) => s + l.ok + l.danado + l.perdido, 0)}</b> unidad(es).`
            + (perdidos > 0 ? `<br><span style="color:#c82333;">Incluye <b>${perdidos}</b> declarada(s) como PERDIDA(S):
                se descuentan del inventario de la bodega.</span>` : ''),
        icon: perdidos > 0 ? 'warning' : 'question',
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Sí, registrar',
        cancelButtonText: 'Cancelar',
    });
    if (!confirmacion.isConfirmed) return;

    const obsInput = contenedor.querySelector('input[name="dev-obs"]');
    try {
        const r = await apiFetch('DevolucionActaServlet', {
            method: 'POST',
            body: JSON.stringify({
                acta_id: actaId,
                observaciones: obsInput && obsInput.value.trim() ? obsInput.value.trim() : undefined,
                lineas,
            }),
        });
        await Swal.fire({
            title: r.cerrada ? 'Acta cerrada' : 'Devolución registrada',
            text: r.cerrada
                ? 'Todo quedó saldado: el acta se cerró automáticamente.'
                : `Quedan ${r.acta.pendientes} unidad(es) aún en proyecto.`,
            icon: 'success', confirmButtonColor: '#f1c40f',
        });
        cargarActas();
    } catch (err) {
        Swal.fire({ title: 'No se pudo registrar', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

cargarActas();
