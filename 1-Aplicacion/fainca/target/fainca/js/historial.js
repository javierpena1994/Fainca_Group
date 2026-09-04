// Historial de movimientos AGRUPADO: una fila por operacion, no por producto.
// Los productos registrados en el mismo envio comparten "lote" y se muestran juntos;
// al desplegar se piden al servidor las lineas completas del documento.
const tbodyHistorial = document.getElementById('tbodyHistorial');
let temporizadorHist = null;

const POR_PAGINA = 1000;
let cargados = 0;      // cuantas LINEAS se han traido (la paginacion es por linea)
let hayMas = false;

const params = new URLSearchParams(window.location.search);
if (params.get('codigo')) {
    document.getElementById('f-codigo').value = params.get('codigo');
}

['f-codigo', 'f-tipo', 'f-desde', 'f-hasta'].forEach((id) => {
    document.getElementById(id).addEventListener('input', () => {
        clearTimeout(temporizadorHist);
        temporizadorHist = setTimeout(cargarHistorial, 200);
    });
});

function filtrosActuales() {
    const f = new URLSearchParams();
    const codigo = document.getElementById('f-codigo').value.trim();
    const tipo = document.getElementById('f-tipo').value;
    const desde = document.getElementById('f-desde').value;
    const hasta = document.getElementById('f-hasta').value;
    if (codigo) f.set('codigo', codigo);
    if (tipo) f.set('tipo', tipo);
    if (desde) f.set('desde', desde);
    if (hasta) f.set('hasta', hasta);
    return f;
}

function etiquetaTipo(tipo) {
    switch (tipo) {
        case 'ingreso': return '<i class="fas fa-arrow-down"></i> INGRESO';
        case 'egreso': return '<i class="fas fa-arrow-up"></i> SALIDA';
        case 'ajuste': return '<i class="fas fa-scale-balanced"></i> AJUSTE';
        case 'edicion': return '<i class="fas fa-pen"></i> EDICIÓN';
        // El triangulo es obligatorio aqui: hace que una correccion salte a la vista
        // entre cientos de registros, para que ningun cambio pase desapercibido.
        case 'correccion': return '<i class="fas fa-triangle-exclamation"></i> CORRECCIÓN';
        default: return esc(tipo).toUpperCase();
    }
}

function formatearFecha(fechaSql) {
    const [f, h] = fechaSql.split(' ');
    const [anio, mes, dia] = f.split('-');
    return `${dia}/${mes}/${anio} ${h ? h.slice(0, 5) : ''}`;
}

// Agrupa las lineas consecutivas que pertenecen al mismo documento.
// Las anteriores a esta funcion no tienen lote: cada una es su propio grupo.
function agrupar(movimientos) {
    const grupos = [];
    for (const mv of movimientos) {
        const ultimo = grupos[grupos.length - 1];
        if (mv.lote && ultimo && ultimo.lote === mv.lote) {
            ultimo.lineas.push(mv);
            continue;
        }
        grupos.push({
            lote: mv.lote || null,
            id: mv.id,
            tipo: mv.tipo,
            fecha: mv.fecha,
            usuario: mv.usuario,
            observaciones: mv.observaciones,
            itemsTotales: mv.itemsLote || 1,   // total real del documento (lo calcula el servidor)
            lineas: [mv],
        });
    }
    return grupos;
}

function numeroDocumento(g) {
    const prefijo = { ingreso: 'ING', egreso: 'SAL', ajuste: 'AJU' }[g.tipo] || 'MOV';
    return `${prefijo}-${String(g.id).padStart(6, '0')}`;
}

// Las anotaciones (edicion/correccion) no son documentos de bodega: no se exportan.
const esDocumento = (tipo) => ['ingreso', 'egreso', 'ajuste'].includes(tipo);

function filaGrupo(g) {
    const unidades = g.lineas.reduce((s, l) => s + Math.abs(l.cantidad), 0);
    const parcial = g.lineas.length < g.itemsTotales;
    const clave = g.lote ? `lote:${g.lote}` : `id:${g.id}`;
    // Toda fila que sea un movimiento de stock se puede desplegar para ver sus
    // productos (incluso las de un solo producto), asi que se quito la columna
    // "Documento" sin perder el detalle: esta a un clic.
    const desplegable = esDocumento(g.tipo);

    return `
        <tr class="fila-doc" data-clave="${esc(clave)}">
            <td style="text-align:center;">
                ${desplegable ? `<button type="button" class="btn-detalles btn-desplegar" title="Ver productos">
                                  <i class="fas fa-chevron-right"></i>
                              </button>` : ''}
            </td>
            <td style="white-space:nowrap;">${formatearFecha(g.fecha)}</td>
            <td style="text-align:center;">
                <span class="badge-tipo ${esc(g.tipo)}">${etiquetaTipo(g.tipo)}</span>
            </td>
            <td style="text-align:center;"><strong>${g.itemsTotales}</strong></td>
            <td style="text-align:center;">${
                (g.tipo === 'edicion' || g.tipo === 'correccion')
                    ? '<span style="color:#bbb;">—</span>'
                    : `<strong>${parcial ? '≥' : ''}${unidades}</strong>`
            }</td>
            <td>${esc(g.usuario)}</td>
            <td>${g.tipo === 'correccion'
                ? `<i class="fas fa-triangle-exclamation" style="color:#b7791f; margin-right:6px;"
                      title="Corrección de un registro anterior"></i>${esc(g.observaciones) || ''}`
                : esc(g.observaciones) || ''}</td>
            ${ES_ADMIN_HISTORIAL ? `
            <td style="text-align:center; white-space:nowrap;">
                <button type="button" class="btn-detalles btn-corregir" title="Corregir/aclarar la observación"
                        data-id="${g.id}" data-obs="${esc(g.observaciones || '')}">
                    <i class="fas fa-pen"></i>
                </button>
                ${esDocumento(g.tipo) ? `
                <button type="button" class="btn-detalles btn-pdf" title="Exportar comprobante en PDF"
                        data-clave="${esc(clave)}" data-numero="${numeroDocumento(g)}">
                    <i class="fas fa-file-pdf"></i>
                </button>` : ''}
            </td>` : ''}
        </tr>`;
}

function cuerpoDe(grupos) {
    return grupos.map(filaGrupo).join('');
}

async function cargarHistorial() {
    const colspan = ES_ADMIN_HISTORIAL ? 8 : 7;
    cargados = 0;
    try {
        const movimientos = await apiFetch(`HistorialServlet?${filtrosActuales().toString()}&salto=0`);
        if (movimientos.length === 0) {
            tbodyHistorial.innerHTML = `
                <tr><td colspan="${colspan}" style="text-align:center; padding:30px; color:#777; font-weight:bold;">
                    <i class="fas fa-info-circle"></i> No hay movimientos con esos filtros.
                </td></tr>`;
            hayMas = false;
            actualizarPie(0);
            return;
        }
        const grupos = agrupar(movimientos);
        tbodyHistorial.innerHTML = cuerpoDe(grupos);
        cargados = movimientos.length;
        hayMas = movimientos.length === POR_PAGINA;
        actualizarPie(grupos.length);
    } catch (err) {
        tbodyHistorial.innerHTML = `
            <tr><td colspan="${colspan}" style="text-align:center; padding:30px; color:#dc3545;">${esc(err.message)}</td></tr>`;
    }
}

async function cargarMas(boton) {
    const textoOriginal = boton.innerHTML;
    boton.disabled = true;
    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cargando...';
    try {
        const mas = await apiFetch(`HistorialServlet?${filtrosActuales().toString()}&salto=${cargados}`);
        if (mas.length > 0) {
            tbodyHistorial.insertAdjacentHTML('beforeend', cuerpoDe(agrupar(mas)));
            cargados += mas.length;
        }
        hayMas = mas.length === POR_PAGINA;
        actualizarPie(tbodyHistorial.querySelectorAll('.fila-doc').length);
    } catch (err) {
        boton.disabled = false;
        boton.innerHTML = textoOriginal;
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

function actualizarPie(documentos) {
    const pie = document.getElementById('pie-historial');
    if (!pie) return;
    if (cargados === 0) { pie.innerHTML = ''; return; }
    pie.innerHTML = `
        <span style="color:#777;">Mostrando <b>${documentos}</b> movimiento(s)
            · <b>${cargados}</b> línea(s)${hayMas ? ' (hay más)' : ''}.</span>
        ${hayMas ? `<button type="button" class="btn-add-row" id="btn-cargar-mas" style="margin:0;">
                        <i class="fas fa-angles-down"></i> Cargar ${POR_PAGINA} más
                    </button>` : ''}`;
    const btn = document.getElementById('btn-cargar-mas');
    if (btn) btn.addEventListener('click', () => cargarMas(btn));
}

// --- Acciones de cada fila (delegadas: las filas se redibujan) ---
tbodyHistorial.addEventListener('click', (e) => {
    const desplegar = e.target.closest('.btn-desplegar');
    if (desplegar) { alternarDetalle(desplegar); return; }

    const corregir = e.target.closest('.btn-corregir');
    if (corregir) { corregirMovimiento(Number(corregir.dataset.id), corregir.dataset.obs || ''); return; }

    const pdf = e.target.closest('.btn-pdf');
    if (pdf) { exportarComprobante(pdf.dataset.clave, pdf.dataset.numero); }
});

async function alternarDetalle(boton) {
    const filaDoc = boton.closest('tr');
    const siguiente = filaDoc.nextElementSibling;

    if (siguiente && siguiente.classList.contains('fila-detalle')) {
        siguiente.remove();
        boton.innerHTML = '<i class="fas fa-chevron-right"></i>';
        return;
    }

    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    const colspan = ES_ADMIN_HISTORIAL ? 8 : 7;
    try {
        const doc = await pedirDocumento(filaDoc.dataset.clave);
        const filas = doc.items.map((it, i) => `
            <tr>
                <td style="text-align:center; color:#777;">${i + 1}</td>
                <td><strong>${esc(it.codigo)}</strong></td>
                <td>${esc(it.marca)}</td>
                <td>${esc((it.descripcion || '').split('\n')[0])}</td>
                <td style="text-align:center;">${esc(it.ubicacion) || '—'}</td>
                <td style="text-align:center; font-weight:bold;">${
                    doc.tipo === 'egreso' ? '-' + it.cantidad
                    : doc.tipo === 'ajuste' ? (it.cantidad > 0 ? '+' + it.cantidad : it.cantidad)
                    : '+' + it.cantidad}</td>
                <td style="text-align:center;"><strong>${it.stock_resultante}</strong></td>
            </tr>`).join('');

        filaDoc.insertAdjacentHTML('afterend', `
            <tr class="fila-detalle">
                <td colspan="${colspan}" style="background:#fafafa; padding:14px 18px;">
                    <div style="font-weight:bold; color:#444; margin-bottom:8px;">
                        <i class="fas fa-list-ol"></i> Productos de ${esc(doc.numero)}
                        <span style="font-weight:normal; color:#777;">
                            · ${doc.total_items} ítem(s) · ${doc.total_unidades} unidad(es)</span>
                    </div>
                    <table class="user-table" style="margin:0;">
                        <thead>
                            <tr>
                                <th style="width:50px; text-align:center;">ÍTEM</th>
                                <th>Código</th><th>Marca</th><th>Descripción</th>
                                <th style="text-align:center;">Ubicación</th>
                                <th style="text-align:center;">Cantidad</th>
                                <th style="text-align:center;">Stock resultante</th>
                            </tr>
                        </thead>
                        <tbody>${filas}</tbody>
                    </table>
                </td>
            </tr>`);
        boton.innerHTML = '<i class="fas fa-chevron-down"></i>';
    } catch (err) {
        boton.innerHTML = '<i class="fas fa-chevron-right"></i>';
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

function pedirDocumento(clave) {
    const [tipo, valor] = clave.split(/:(.*)/s);
    return apiFetch(`DocumentoServlet?${tipo}=${encodeURIComponent(valor)}`);
}

// --- Exportar el comprobante en PDF ---
async function exportarComprobante(clave, numero) {
    const { value: opcion } = await Swal.fire({
        title: 'Exportar comprobante',
        html: `Documento <b>${esc(numero)}</b>`,
        input: 'radio',
        inputOptions: {
            no: 'Sin fotos de los productos (archivo liviano)',
            si: 'Con la foto de cada producto',
        },
        inputValue: 'no',
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: '<i class="fas fa-file-pdf"></i> Descargar PDF',
        cancelButtonText: 'Cancelar',
    });
    if (!opcion) return;

    const [tipo, valor] = clave.split(/:(.*)/s);
    window.location.href =
        `ExportarMovimientoServlet?${tipo}=${encodeURIComponent(valor)}&fotos=${opcion}`;
}

async function corregirMovimiento(movimientoId, observacionOriginal) {
    const { value: correccion } = await Swal.fire({
        title: 'Corregir / aclarar movimiento',
        html: `<div style="text-align:left; margin-bottom:10px;">
                   <strong>Observación actual:</strong>
                   <div style="background:#f7f7f7; border-radius:6px; padding:8px; margin-top:4px; color:#555;">
                       ${esc(observacionOriginal) || '<em>(vacía)</em>'}
                   </div>
               </div>`,
        input: 'textarea',
        inputPlaceholder: 'Escribe aquí la aclaración o el dato correcto',
        inputAttributes: { style: 'height:100px;' },
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Guardar corrección',
        cancelButtonText: 'Cancelar',
        inputValidator: (v) => (!v || !v.trim()) ? 'Escribe la corrección antes de guardar.' : undefined,
    });
    if (!correccion) return;

    try {
        await apiFetch('CorregirMovimientoServlet', {
            method: 'POST',
            body: JSON.stringify({ movimiento_id: movimientoId, correccion: correccion.trim() }),
        });
        await Swal.fire({
            title: 'Corrección guardada',
            text: 'Se agregó como una nueva entrada en el historial, sin alterar el registro original.',
            icon: 'success',
            confirmButtonColor: '#f1c40f',
        });
        cargarHistorial();
    } catch (err) {
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

cargarHistorial();
