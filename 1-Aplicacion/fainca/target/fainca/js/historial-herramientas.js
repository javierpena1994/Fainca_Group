// Historial de la bodega de herramientas AGRUPADO: una fila por OPERACION, no por
// herramienta. Lo registrado en un mismo movimiento comparte "lote" y se muestra
// junto; al desplegar se piden al servidor las lineas completas de la operacion.
const tbodyHistHerr = document.getElementById('tbodyHistHerr');
let temporizadorHH = null;

// ?q=NOMBRE llega desde el boton "ver historial" del catalogo
const paramsHH = new URLSearchParams(window.location.search);
if (paramsHH.get('q')) {
    document.getElementById('hh-buscar').value = paramsHH.get('q');
}

['hh-buscar', 'hh-tipo', 'hh-desde', 'hh-hasta'].forEach((id) => {
    document.getElementById(id).addEventListener('input', () => {
        clearTimeout(temporizadorHH);
        temporizadorHH = setTimeout(cargarHistHerr, 200);
    });
});

function filtrosHH() {
    const f = new URLSearchParams();
    const q = document.getElementById('hh-buscar').value.trim();
    const tipo = document.getElementById('hh-tipo').value;
    const desde = document.getElementById('hh-desde').value;
    const hasta = document.getElementById('hh-hasta').value;
    if (q) f.set('q', q);
    if (tipo) f.set('tipo', tipo);
    if (desde) f.set('desde', desde);
    if (hasta) f.set('hasta', hasta);
    return f;
}

function etiquetaTipoHerr(tipo) {
    switch (tipo) {
        case 'ingreso': return '<i class="fas fa-arrow-down"></i> INGRESO';
        case 'entrega': return '<i class="fas fa-people-carry-box"></i> ENTREGA';
        case 'devolucion': return '<i class="fas fa-rotate-left"></i> DEVOLUCIÓN';
        case 'dano': return '<i class="fas fa-triangle-exclamation"></i> DAÑADO';
        case 'perdida': return '<i class="fas fa-circle-xmark"></i> PERDIDA';
        case 'reparacion': return '<i class="fas fa-screwdriver-wrench"></i> REPARACIÓN';
        case 'baja': return '<i class="fas fa-ban"></i> BAJA';
        case 'ajuste': return '<i class="fas fa-scale-balanced"></i> AJUSTE';
        case 'edicion': return '<i class="fas fa-pen"></i> EDICIÓN';
        case 'correccion': return '<i class="fas fa-triangle-exclamation"></i> CORRECCIÓN';
        default: return esc(tipo).toUpperCase();
    }
}

// Una tanda de devolucion puede generar lineas de tres tipos a la vez (lo que
// volvio bien, lo dañado y lo perdido). Como fueron UNA sola operacion comparten
// lote, y la fila se rotula DEVOLUCIÓN; el desglose se ve al desplegarla.
const TIPOS_DEVOLUCION = ['devolucion', 'dano', 'perdida'];

// Un ajuste puede tocar la cantidad y la observacion de la misma herramienta,
// generando lineas 'ajuste' y 'edicion' juntas; manda el ajuste, que es lo que
// movio stock. Si solo se cambiaron observaciones, la fila se rotula EDICIÓN.
function tipoDelGrupo(lineas) {
    const tipos = [...new Set(lineas.map((l) => l.tipo))];
    if (tipos.length === 1) return tipos[0];
    if (tipos.every((t) => TIPOS_DEVOLUCION.includes(t))) return 'devolucion';
    if (tipos.every((t) => t === 'ajuste' || t === 'edicion')) return 'ajuste';
    return tipos[0];
}

// Agrupa las lineas consecutivas que pertenecen a la misma operacion.
function agruparHerr(movimientos) {
    const grupos = [];
    for (const mv of movimientos) {
        const ultimo = grupos[grupos.length - 1];
        if (mv.lote && ultimo && ultimo.lote === mv.lote) {
            ultimo.lineas.push(mv);
            ultimo.tipo = tipoDelGrupo(ultimo.lineas);
            continue;
        }
        grupos.push({
            lote: mv.lote || null,
            id: mv.id,
            tipo: mv.tipo,
            fecha: mv.fecha,
            usuario: mv.usuario,
            actaNumero: mv.actaNumero,
            observaciones: mv.observaciones,
            itemsTotales: mv.itemsLote || 1,   // total real de la operacion (lo calcula el servidor)
            lineas: [mv],
        });
    }
    return grupos;
}

function formatearFechaHH(fechaSql) {
    const [f, h] = fechaSql.split(' ');
    const [anio, mes, dia] = f.split('-');
    return `${dia}/${mes}/${anio} ${h ? h.slice(0, 5) : ''}`;
}

// Signo respecto del stock DISPONIBLE en bodega: la entrega resta, la devolucion
// suma... dano/perdida/baja no tocan el disponible (mueven dañadas o el total),
// asi que van sin signo.
function cantidadConSigno(mv) {
    switch (mv.tipo) {
        case 'ingreso':
        case 'devolucion':
        case 'reparacion':
            return `<strong style="color:#1e7e34;">+${mv.cantidad}</strong>`;
        case 'entrega':
            return `<strong style="color:#2166ac;">-${mv.cantidad}</strong>`;
        case 'ajuste':
            return `<strong style="color:${mv.cantidad < 0 ? '#c82333' : '#1e7e34'};">${mv.cantidad > 0 ? '+' : ''}${mv.cantidad}</strong>`;
        // Cambiar la observacion no mueve stock: mostrar un 0 confundiria.
        case 'edicion':
        case 'correccion':
            return '<span style="color:#bbb;">—</span>';
        default:
            return `<strong>${mv.cantidad}</strong>`;
    }
}

function filaGrupoHerr(g) {
    const unidades = g.lineas.reduce((s, l) => s + Math.abs(l.cantidad), 0);
    const parcial = g.lineas.length < g.itemsTotales;
    const clave = g.lote ? `lote:${g.lote}` : `id:${g.id}`;

    return `
        <tr class="fila-doc" data-clave="${esc(clave)}">
            <td style="text-align:center;">
                <button type="button" class="btn-detalles btn-desplegar" title="Ver las herramientas">
                    <i class="fas fa-chevron-right"></i>
                </button>
            </td>
            <td style="white-space:nowrap;">${formatearFechaHH(g.fecha)}</td>
            <td style="text-align:center;">
                <span class="badge-tipo ${esc(g.tipo)}">${etiquetaTipoHerr(g.tipo)}</span>
            </td>
            <td style="text-align:center;"><strong>${g.itemsTotales}</strong></td>
            <td style="text-align:center;">${
                unidades === 0 && (g.tipo === 'edicion' || g.tipo === 'correccion')
                    ? '<span style="color:#bbb;">—</span>'
                    : `<strong>${parcial ? '≥' : ''}${unidades}</strong>`
            }</td>
            <td style="text-align:center;">${g.actaNumero ? esc(g.actaNumero) : '<span style="color:#bbb;">—</span>'}</td>
            <td>${esc(g.usuario)}</td>
            <td>${esc(g.observaciones) || ''}</td>
        </tr>`;
}

async function cargarHistHerr() {
    try {
        const movimientos = await apiFetch(`HistorialHerramientasServlet?${filtrosHH().toString()}`);
        if (movimientos.length === 0) {
            tbodyHistHerr.innerHTML = `
                <tr><td colspan="8" style="text-align:center; padding:30px; color:#777; font-weight:bold;">
                    <i class="fas fa-info-circle"></i> No hay movimientos con esos filtros.
                </td></tr>`;
            document.getElementById('pie-hist-herr').innerHTML = '';
            return;
        }
        const grupos = agruparHerr(movimientos);
        tbodyHistHerr.innerHTML = grupos.map(filaGrupoHerr).join('');
        document.getElementById('pie-hist-herr').innerHTML =
            `Mostrando <b>${grupos.length}</b> movimiento(s) · <b>${movimientos.length}</b> línea(s)`
            + (movimientos.length === 1000 ? ' (las 1000 más recientes)' : '');
    } catch (err) {
        tbodyHistHerr.innerHTML = `
            <tr><td colspan="8" style="text-align:center; padding:30px; color:#dc3545;">${esc(err.message)}</td></tr>`;
    }
}

// --- Desplegar el detalle de una operacion ---
tbodyHistHerr.addEventListener('click', (e) => {
    const desplegar = e.target.closest('.btn-desplegar');
    if (desplegar) alternarDetalleHerr(desplegar);
});

async function alternarDetalleHerr(boton) {
    const filaGrupo = boton.closest('tr');
    const siguiente = filaGrupo.nextElementSibling;

    if (siguiente && siguiente.classList.contains('fila-detalle')) {
        siguiente.remove();
        boton.innerHTML = '<i class="fas fa-chevron-right"></i>';
        return;
    }

    boton.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    try {
        const [tipo, valor] = filaGrupo.dataset.clave.split(/:(.*)/s);
        const lineas = await apiFetch(`OperacionHerramientasServlet?${tipo}=${encodeURIComponent(valor)}`);

        const filas = lineas.map((l, i) => `
            <tr>
                <td style="text-align:center; color:#777;">${i + 1}</td>
                <td><strong>${esc(l.nombre)}</strong></td>
                <td style="text-align:center;">
                    <span class="badge-tipo ${esc(l.tipo)}">${etiquetaTipoHerr(l.tipo)}</span>
                </td>
                <td style="text-align:center;">${cantidadConSigno(l)}</td>
                <td style="text-align:center;"><strong>${l.disponibleResultante}</strong></td>
                <td>${esc(l.observaciones) || ''}</td>
            </tr>`).join('');

        const unidades = lineas.reduce((s, l) => s + Math.abs(l.cantidad), 0);
        filaGrupo.insertAdjacentHTML('afterend', `
            <tr class="fila-detalle">
                <td colspan="8" style="background:#fafafa; padding:14px 18px;">
                    <div style="font-weight:bold; color:#444; margin-bottom:8px;">
                        <i class="fas fa-list-ol"></i> Herramientas de este movimiento
                        <span style="font-weight:normal; color:#777;">
                            · ${lineas.length} ítem(s) · ${unidades} unidad(es)</span>
                    </div>
                    <table class="user-table" style="margin:0;">
                        <thead>
                            <tr>
                                <th style="width:50px; text-align:center;">ÍTEM</th>
                                <th>Herramienta</th>
                                <th style="text-align:center;">Resultado</th>
                                <th style="text-align:center;">Cantidad</th>
                                <th style="text-align:center;">Disp. resultante</th>
                                <th>Observación</th>
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

cargarHistHerr();
