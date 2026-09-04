// Catalogo de la bodega de herramientas: stock, altas y acciones que no pasan
// por un acta (compras, reparaciones, bajas, reconteos).
const tbodyHerramientas = document.getElementById('tbodyHerramientas');
let temporizadorHerr = null;

['h-buscar', 'h-tipo', 'h-reposicion'].forEach((id) => {
    document.getElementById(id).addEventListener('input', () => {
        clearTimeout(temporizadorHerr);
        temporizadorHerr = setTimeout(cargarHerramientas, 200);
    });
});

function filtrosHerr() {
    const f = new URLSearchParams();
    const q = document.getElementById('h-buscar').value.trim();
    const tipo = document.getElementById('h-tipo').value;
    const reposicion = document.getElementById('h-reposicion').value;
    if (q) f.set('q', q);
    if (tipo) f.set('tipo', tipo);
    if (reposicion) f.set('reposicion', reposicion);
    return f;
}

// Necesita reposicion: tiene danadas, o es consumible en su minimo (o por debajo)
function necesitaReposicion(h) {
    return h.danadas > 0
        || (h.tipo === 'consumible' && h.stockMinimo !== null && h.stockMinimo !== undefined
            && h.disponible <= h.stockMinimo);
}

function filaHerramienta(h) {
    const alerta = necesitaReposicion(h)
        ? `<i class="fas fa-triangle-exclamation" style="color:#b7791f; margin-left:6px;"
              title="${h.danadas > 0 ? 'Tiene unidades dañadas por reponer' : 'En o bajo el stock mínimo'}"></i>`
        : '';
    const minimo = (h.tipo === 'consumible' && h.stockMinimo !== null && h.stockMinimo !== undefined)
        ? `<span style="color:#999; font-size:0.8rem;" title="Stock mínimo configurado"> / mín ${h.stockMinimo}</span>`
        : '';

    return `
        <tr>
            <td><strong>${esc(h.nombre)}</strong>${alerta}</td>
            <td style="text-align:center;">
                <span class="badge-tipo ${esc(h.tipo)}">${h.tipo === 'consumible' ? 'CONSUMIBLE' : 'HERRAMIENTA'}</span>
            </td>
            <td style="text-align:center;"><strong>${h.disponible}</strong>${minimo}</td>
            <td style="text-align:center;">${h.fuera > 0 ? `<strong style="color:#2166ac;">${h.fuera}</strong>` : '<span style="color:#bbb;">0</span>'}</td>
            <td style="text-align:center;">${h.danadas > 0 ? `<strong style="color:#b7791f;">${h.danadas}</strong>` : '<span style="color:#bbb;">0</span>'}</td>
            <td style="text-align:center;">${h.total}</td>
            <td>${esc(h.observaciones) || ''}</td>
            <td style="text-align:center; white-space:nowrap;">
                <button type="button" class="btn-detalles btn-renombrar" title="Corregir el nombre"
                        data-nombre="${esc(h.nombre)}"><i class="fas fa-pen"></i></button>
                ${h.danadas > 0 ? `
                <button type="button" class="btn-detalles btn-danadas" title="Resolver dañadas (reparar o dar de baja)"
                        data-nombre="${esc(h.nombre)}" data-danadas="${h.danadas}"><i class="fas fa-screwdriver-wrench"></i></button>` : ''}
                <a class="btn-detalles" style="text-decoration:none; display:inline-block;"
                   href="historialHerramientas.jsp?q=${encodeURIComponent(h.nombre)}" title="Ver historial">
                   <i class="fas fa-history"></i></a>
            </td>
        </tr>`;
}

async function cargarHerramientas() {
    try {
        const lista = await apiFetch(`HerramientasServlet?${filtrosHerr().toString()}`);
        if (lista.length === 0) {
            tbodyHerramientas.innerHTML = `
                <tr><td colspan="8" style="text-align:center; padding:30px; color:#777; font-weight:bold;">
                    <i class="fas fa-info-circle"></i> No hay herramientas con esos filtros.
                </td></tr>`;
            document.getElementById('pie-herramientas').innerHTML = '';
            return;
        }
        tbodyHerramientas.innerHTML = lista.map(filaHerramienta).join('');
        const reponer = lista.filter(necesitaReposicion).length;
        document.getElementById('pie-herramientas').innerHTML =
            `Mostrando <b>${lista.length}</b> ítem(s)`
            + (reponer ? ` · <span style="color:#b7791f;"><i class="fas fa-triangle-exclamation"></i> <b>${reponer}</b> por reponer</span>` : '');
    } catch (err) {
        tbodyHerramientas.innerHTML = `
            <tr><td colspan="8" style="text-align:center; padding:30px; color:#dc3545;">${esc(err.message)}</td></tr>`;
    }
}

// --- Alta de un item nuevo ---
document.getElementById('btn-nueva').addEventListener('click', async () => {
    const { value: datos } = await Swal.fire({
        title: 'Registrar herramienta',
        html: `
            <div style="text-align:left; display:grid; gap:10px;">
                <input id="sw-nombre" class="swal2-input" style="margin:0; text-transform:uppercase;"
                       placeholder="Nombre (ej: TALADRO)" autocomplete="off">
                <select id="sw-tipo" class="swal2-select" style="margin:0;">
                    <option value="herramienta">Herramienta — se presta y debe volver</option>
                    <option value="consumible">Consumible — se entrega y se gasta</option>
                </select>
                <input id="sw-cantidad" class="swal2-input" style="margin:0;" type="number" min="0" placeholder="Cantidad inicial en bodega">
                <input id="sw-minimo" class="swal2-input" style="margin:0; display:none;" type="number" min="0"
                       placeholder="Stock mínimo para avisar reposición (opcional)">
                <textarea id="sw-obs" class="swal2-textarea" style="margin:0; height:70px;"
                          placeholder="Observación: marca, estado, si le falta algo..."></textarea>
            </div>`,
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Registrar',
        cancelButtonText: 'Cancelar',
        didOpen: () => {
            const tipo = document.getElementById('sw-tipo');
            const minimo = document.getElementById('sw-minimo');
            tipo.addEventListener('change', () => {
                minimo.style.display = tipo.value === 'consumible' ? '' : 'none';
            });
            // Los nombres van siempre en mayusculas. El text-transform del CSS solo
            // cambia como se ve, asi que ademas se convierte el valor real mientras
            // se escribe (el servidor lo vuelve a hacer, por si acaso).
            const nombre = document.getElementById('sw-nombre');
            nombre.addEventListener('input', () => {
                const pos = nombre.selectionStart;
                nombre.value = nombre.value.toUpperCase();
                nombre.setSelectionRange(pos, pos);
            });
            nombre.focus();
        },
        preConfirm: () => {
            const nombre = document.getElementById('sw-nombre').value.trim();
            const tipo = document.getElementById('sw-tipo').value;
            const cantidad = document.getElementById('sw-cantidad').value;
            const minimo = document.getElementById('sw-minimo').value;
            if (!nombre) {
                Swal.showValidationMessage('El nombre es obligatorio');
                return false;
            }
            if (cantidad === '' || Number(cantidad) < 0) {
                Swal.showValidationMessage('Indica la cantidad inicial (puede ser 0)');
                return false;
            }
            return {
                nombre, tipo,
                cantidad: Number(cantidad),
                stock_minimo: (tipo === 'consumible' && minimo !== '') ? Number(minimo) : undefined,
                observaciones: document.getElementById('sw-obs').value.trim() || undefined,
            };
        },
    });
    if (!datos) return;

    try {
        const h = await apiFetch('RegistrarHerramientaServlet', { method: 'POST', body: JSON.stringify(datos) });
        await Swal.fire({
            title: 'Registrada',
            text: `${h.nombre} quedó en el catálogo con ${h.disponible} unidad(es).`,
            icon: 'success', confirmButtonColor: '#f1c40f',
        });
        cargarHerramientas();
    } catch (err) {
        Swal.fire({ title: 'No se pudo registrar', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
});

// --- Acciones de cada fila (delegadas) ---
// Sumar y restar unidades ya no se hace aqui: vive en "Ajuste de herramientas",
// que ademas permite cambiar la observacion y tratar varias a la vez.
tbodyHerramientas.addEventListener('click', (e) => {
    const renombrar = e.target.closest('.btn-renombrar');
    if (renombrar) { renombrarHerramienta(renombrar.dataset.nombre); return; }

    const danadas = e.target.closest('.btn-danadas');
    if (danadas) { resolverDanadas(danadas.dataset.nombre, Number(danadas.dataset.danadas)); }
});

// Corregir como quedo escrito el nombre (ej: "corta frio" -> "CORTA FRIO").
// El historial y las actas apuntan al id de la herramienta, no a su nombre, asi
// que no se pierde ni se despega ningun registro anterior.
async function renombrarHerramienta(nombre) {
    const { value: nuevo } = await Swal.fire({
        title: 'Corregir el nombre',
        html: `<div style="text-align:left; color:#666; margin-bottom:8px;">
                   Se llama <b>${esc(nombre)}</b>. El historial y las actas anteriores
                   seguirán ligados a esta herramienta y pasarán a mostrar el nombre nuevo.
               </div>`,
        input: 'text',
        inputValue: nombre,
        inputAttributes: { style: 'text-transform: uppercase;', autocomplete: 'off' },
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Guardar nombre',
        cancelButtonText: 'Cancelar',
        inputValidator: (v) => (!v || !v.trim()) ? 'El nombre no puede quedar vacío.' : undefined,
    });
    if (!nuevo) return;

    try {
        const h = await apiFetch('RenombrarHerramientaServlet', {
            method: 'POST',
            body: JSON.stringify({ nombre, nombre_nuevo: nuevo.trim() }),
        });
        await Swal.fire({
            title: 'Nombre corregido',
            html: `Ahora se llama <b>${esc(h.nombre)}</b>.<br>
                   <span style="color:#777; font-size:0.9rem;">El cambio quedó anotado en el historial.</span>`,
            icon: 'success', confirmButtonColor: '#f1c40f',
        });
        cargarHerramientas();
    } catch (err) {
        Swal.fire({ title: 'No se pudo renombrar', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

async function resolverDanadas(nombre, danadas) {
    const { value: datos } = await Swal.fire({
        title: `Dañadas de ${nombre}`,
        html: `
            <div style="text-align:left; display:grid; gap:10px;">
                <div style="color:#666;">Hay <b>${danadas}</b> unidad(es) dañada(s).</div>
                <select id="sw-que" class="swal2-select" style="margin:0;">
                    <option value="reparacion">Reparada — vuelve al stock disponible</option>
                    <option value="baja">Baja definitiva — se descarta</option>
                </select>
                <input id="sw-cant" class="swal2-input" style="margin:0;" type="number" min="1" max="${danadas}" placeholder="Cantidad">
                <textarea id="sw-motivo" class="swal2-textarea" style="margin:0; height:70px;"
                          placeholder="Motivo / detalle"></textarea>
            </div>`,
        showCancelButton: true,
        confirmButtonColor: '#f1c40f', cancelButtonColor: '#444',
        confirmButtonText: 'Guardar', cancelButtonText: 'Cancelar',
        preConfirm: () => {
            const datos = leerCantidadYMotivo();
            if (!datos) return false;
            return { ...datos, accion: document.getElementById('sw-que').value };
        },
    });
    if (!datos) return;
    enviarAccion({ nombre, ...datos });
}

function leerCantidadYMotivo() {
    const cantidad = document.getElementById('sw-cant').value;
    const motivo = document.getElementById('sw-motivo').value.trim();
    if (cantidad === '' || Number(cantidad) <= 0) {
        Swal.showValidationMessage('Indica la cantidad');
        return false;
    }
    if (!motivo) {
        Swal.showValidationMessage('El motivo es obligatorio: queda en el historial');
        return false;
    }
    return { cantidad: Number(cantidad), observaciones: motivo };
}

async function enviarAccion(cuerpo) {
    try {
        const h = await apiFetch('StockHerramientaServlet', { method: 'POST', body: JSON.stringify(cuerpo) });
        await Swal.fire({
            title: 'Guardado',
            html: `${esc(h.nombre)}: disponible <b>${h.disponible}</b> · en proyectos <b>${h.fuera}</b>`
                + ` · dañadas <b>${h.danadas}</b> · total <b>${h.total}</b>`,
            icon: 'success', confirmButtonColor: '#f1c40f',
        });
        cargarHerramientas();
    } catch (err) {
        Swal.fire({ title: 'No se pudo guardar', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

cargarHerramientas();
