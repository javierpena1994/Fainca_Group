// Panel de control: tarjetas de resumen, grafico de actividad y ultimos movimientos.
const COLOR_INGRESOS = '#2166ac'; // pareja azul/naranja validada para daltonismo
const COLOR_SALIDAS = '#c76b04';

const tooltip = document.getElementById('grafico-tooltip');

async function cargarDashboard() {
    const data = await apiFetch('DashboardServlet');
    pintarResumen(data.resumen);
    pintarGrafico(data.actividad);
    pintarUltimos(data.ultimos);
}

// --- Tarjetas ---
function pintarResumen(r) {
    document.getElementById('st-productos').textContent = r.productos_activos;
    document.getElementById('st-unidades').textContent = Number(r.unidades_totales).toLocaleString('es');
    document.getElementById('st-marcas').textContent = r.marcas;
    document.getElementById('st-mov-hoy').textContent = r.movimientos_hoy;
    document.getElementById('st-hoy-detalle').textContent =
        `+${r.unidades_ingresadas_hoy} / -${r.unidades_salidas_hoy} uds`;
}

// --- Grafico de barras (7 dias x ingresos/salidas) ---
function pintarGrafico(dias) {
    const contenedor = document.getElementById('grafico');
    const maximo = Math.max(1, ...dias.map((d) => Math.max(d.ingresos, d.salidas)));
    const sinDatos = dias.every((d) => d.ingresos === 0 && d.salidas === 0);

    if (sinDatos) {
        contenedor.innerHTML = '<p style="color:#777; text-align:center; padding:30px;">' +
            '<i class="fas fa-info-circle"></i> Sin movimientos en los últimos 7 días.</p>';
        return;
    }

    contenedor.innerHTML = dias.map((d) => {
        const fecha = new Date(d.fecha + 'T00:00:00');
        const etiqueta = fecha.toLocaleDateString('es', { weekday: 'short', day: 'numeric' });
        const hIn = Math.round((d.ingresos / maximo) * 100);
        const hOut = Math.round((d.salidas / maximo) * 100);
        return `
            <div class="g-dia">
                <div class="g-barras">
                    <div class="g-barra" style="height:${hIn}%; background:${COLOR_INGRESOS};"
                         data-info="Ingresos · ${etiqueta}: ${d.ingresos} uds"></div>
                    <div class="g-barra" style="height:${hOut}%; background:${COLOR_SALIDAS};"
                         data-info="Salidas · ${etiqueta}: ${d.salidas} uds"></div>
                </div>
                <div class="g-etiqueta">${etiqueta}</div>
            </div>`;
    }).join('');
}

// Tooltip al pasar el cursor por una barra
document.getElementById('grafico').addEventListener('mousemove', (e) => {
    const barra = e.target.closest('.g-barra');
    if (!barra) { tooltip.style.display = 'none'; return; }
    tooltip.textContent = barra.dataset.info;
    tooltip.style.display = 'block';
    tooltip.style.left = (e.pageX + 12) + 'px';
    tooltip.style.top = (e.pageY - 30) + 'px';
});
document.getElementById('grafico').addEventListener('mouseleave', () => {
    tooltip.style.display = 'none';
});

// --- Ultimos movimientos ---
function formatearFecha(fechaSql) {
    const [f, h] = fechaSql.split(' ');
    const [anio, mes, dia] = f.split('-');
    return `${dia}/${mes}/${anio} ${h ? h.slice(0, 5) : ''}`;
}

function pintarUltimos(movimientos) {
    const tbody = document.getElementById('tbodyUltimos');
    if (movimientos.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:25px; color:#777;">
            <i class="fas fa-info-circle"></i> Aún no hay movimientos registrados.</td></tr>`;
        return;
    }
    tbody.innerHTML = movimientos.map((mv) => `
        <tr>
            <td style="white-space:nowrap;">${formatearFecha(mv.fecha)}</td>
            <td><strong>${esc(mv.productoCodigo)}</strong></td>
            <td>${esc(mv.marca)}</td>
            <td style="text-align:center;">
                <span class="badge-tipo ${esc(mv.tipo)}">
                    ${mv.tipo === 'ingreso' ? '<i class="fas fa-arrow-down"></i> INGRESO'
                      : mv.tipo === 'egreso' ? '<i class="fas fa-arrow-up"></i> SALIDA'
                      : mv.tipo === 'edicion' ? '<i class="fas fa-pen"></i> EDICIÓN'
                      : mv.tipo === 'correccion' ? '<i class="fas fa-triangle-exclamation"></i> CORRECCIÓN'
                      : '<i class="fas fa-scale-balanced"></i> AJUSTE'}
                </span>
            </td>
            <td style="text-align:center;">${
                (mv.tipo === 'edicion' || mv.tipo === 'correccion') ? '<span style="color:#bbb;">—</span>'
                : mv.tipo === 'egreso' ? '-' + mv.cantidad
                : mv.tipo === 'ajuste' ? (mv.cantidad > 0 ? '+' + mv.cantidad : mv.cantidad)
                : '+' + mv.cantidad
            }</td>
            <td>${esc(mv.usuario)}</td>
        </tr>
    `).join('');
}

cargarDashboard();
