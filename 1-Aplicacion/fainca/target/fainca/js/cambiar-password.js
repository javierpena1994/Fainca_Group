document.getElementById('form-password').addEventListener('submit', async (e) => {
    e.preventDefault();

    const passwordActual = document.getElementById('actual').value;
    const passwordNueva = document.getElementById('nueva').value;
    const confirmar = document.getElementById('confirmar').value;

    if (passwordNueva !== confirmar) {
        Swal.fire({
            title: 'No coinciden',
            text: 'La contraseña nueva no coincide con la confirmación.',
            icon: 'warning',
            confirmButtonColor: '#f1c40f',
        });
        return;
    }

    try {
        await apiFetch('CambiarPasswordServlet', {
            method: 'POST',
            body: JSON.stringify({ passwordActual, passwordNueva }),
        });
        Swal.fire({
            title: 'Contraseña actualizada',
            text: 'Úsala la próxima vez que ingreses al sistema.',
            icon: 'success',
            confirmButtonColor: '#f1c40f',
        }).then(() => { window.location.href = 'index.jsp'; });
    } catch (err) {
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
});
