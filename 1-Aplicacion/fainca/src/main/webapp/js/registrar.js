const zonaImagen = document.getElementById('zona-p-imagen');
inicializarZonaImagen(zonaImagen);

// El select de marcas viene renderizado desde el servidor (JSP).
// Esta funcion lo recarga despues de agregar una marca nueva.
async function recargarMarcas() {
    const marcas = await apiFetch('MarcaServlet');
    document.getElementById('p-marca').innerHTML =
        marcas.map((m) => `<option value="${m.id}">${esc(m.nombre)}</option>`).join('');
}

document.getElementById('form-marca').addEventListener('submit', async (e) => {
    e.preventDefault();
    const nombre = document.getElementById('nueva-marca').value.trim();
    if (!nombre) return;
    try {
        await apiFetch('MarcaServlet', { method: 'POST', body: JSON.stringify({ nombre }) });
        document.getElementById('nueva-marca').value = '';
        await recargarMarcas();
        Swal.fire({ title: 'Marca agregada', text: nombre, icon: 'success', confirmButtonColor: '#f1c40f' });
    } catch (err) {
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
});

document.getElementById('registroForm').addEventListener('submit', (e) => {
    e.preventDefault();

    Swal.fire({
        title: '¿Confirmar Registro?',
        text: 'Se agregará el nuevo producto al sistema FAINCA',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#f1c40f',
        cancelButtonColor: '#444',
        confirmButtonText: 'Sí, registrar',
        cancelButtonText: 'Revisar',
    }).then(async (result) => {
        if (!result.isConfirmed) return;

        const codigo = document.getElementById('p-codigo').value.trim();
        const stockInicial = Number(document.getElementById('p-stock').value);

        try {
            const data = await apiFetch('RegistrarProductoServlet', {
                method: 'POST',
                body: JSON.stringify({
                    codigo,
                    marca_id: Number(document.getElementById('p-marca').value),
                    stock_inicial: stockInicial,
                    ubicacion: document.getElementById('p-ubicacion').value.trim(),
                    unidad_medida: document.getElementById('p-unidad').value.trim() || 'UND.',
                    descripcion: document.getElementById('p-descripcion').value.trim(),
                }),
            });

            // La foto se sube despues, porque recien ahora existe el producto al que asociarla.
            const archivoImagen = document.getElementById('p-imagen').files[0];
            let errorFoto = null;
            if (archivoImagen) {
                try {
                    await subirImagenProducto(codigo, archivoImagen, true);
                } catch (err) {
                    errorFoto = err.message;
                }
            }

            await Swal.fire({
                title: errorFoto ? 'Producto guardado, pero sin la foto' : '¡Registrado!',
                html: errorFoto
                    ? `El producto <b>${esc(codigo)}</b> se guardó con stock ${data.stock_actual}, pero la foto no se pudo subir:<br><br>
                       <i>${esc(errorFoto)}</i><br><br>Puedes agregarla después desde "Buscar productos" → editar.`
                    : `El producto <b>${esc(codigo)}</b> ha sido guardado correctamente con stock inicial de ${data.stock_actual}${archivoImagen ? ', junto con su foto' : ''}.`,
                icon: errorFoto ? 'warning' : 'success',
                confirmButtonColor: '#f1c40f',
            });
            document.getElementById('registroForm').reset();
            zonaImagen.limpiar();
        } catch (err) {
            Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
        }
    });
});
