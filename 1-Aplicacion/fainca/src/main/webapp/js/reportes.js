// Exportar el inventario a Excel o PDF.
const botonesFormato = document.querySelectorAll('.btn-formato');

botonesFormato.forEach((boton) => {
    boton.addEventListener('click', () => generarReporte(boton.dataset.formato, boton));
});

async function generarReporte(formato, boton) {
    const marca = document.getElementById('rep-marca').value;
    const descripcion = document.getElementById('rep-descripcion').value;
    const foto = document.getElementById('rep-foto').value;
    const textoOriginal = boton.innerHTML;

    // Estado de carga (un reporte de "todas las marcas" puede tardar un momento).
    botonesFormato.forEach((b) => (b.disabled = true));
    boton.classList.add('generando');
    boton.querySelector('span').textContent = 'Generando...';

    try {
        const res = await fetch(
            `ExportarServlet?marca=${encodeURIComponent(marca)}&formato=${formato}`
            + `&descripcion=${descripcion}&foto=${foto}`);
        if (res.status === 401) {
            window.location.href = 'login.jsp';
            return;
        }
        if (!res.ok) {
            throw new Error('El servidor respondió con un error (' + res.status + ')');
        }

        // Nombre de archivo que manda el servidor en la cabecera.
        const dispo = res.headers.get('Content-Disposition') || '';
        const match = dispo.match(/filename="?([^"]+)"?/);
        const nombre = match ? match[1] : `Inventario_FAINCA.${formato === 'excel' ? 'xlsx' : 'pdf'}`;

        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombre;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);

        Swal.fire({
            title: 'Reporte generado',
            html: `Se descargó <b>${nombre}</b>.`,
            icon: 'success',
            confirmButtonColor: '#f1c40f',
            timer: 2500,
        });
    } catch (err) {
        Swal.fire({ title: 'No se pudo generar', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    } finally {
        botonesFormato.forEach((b) => (b.disabled = false));
        boton.classList.remove('generando');
        boton.innerHTML = textoOriginal;
    }
}
