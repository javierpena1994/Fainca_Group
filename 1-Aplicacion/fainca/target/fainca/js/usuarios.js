const tbodyUsuarios = document.getElementById('tbodyUsuarios');

const NOMBRE_ROL = {
    superadmin: 'Administrador',
    admin: 'Bodega',
    ventas: 'Ventas',
};
const BADGE_ROL = {
    superadmin: 'ingreso', // verde
    admin: 'ajuste',       // gris
    ventas: 'egreso',      // rojo/rosado
};

async function cargarUsuarios() {
    const usuarios = await apiFetch('UsuariosServlet');
    tbodyUsuarios.innerHTML = usuarios.map((u) => `
        <tr class="${u.activo ? '' : 'fila-inactiva'}">
            <td><strong>${esc(u.nombre)}</strong>${u.es_actual ? ' <span style="color:#999;">(tú)</span>' : ''}</td>
            <td>${esc(u.usuario)}</td>
            <td style="text-align:center;">
                <span class="badge-tipo ${BADGE_ROL[u.rol]}">${esc(NOMBRE_ROL[u.rol] || u.rol)}</span>
            </td>
            <td style="text-align:center;">${u.activo ? 'Activo' : 'Inactivo'}</td>
            <td style="text-align:center; white-space:nowrap;">
                <button type="button" class="btn-detalles" title="Editar"
                    onclick='editarUsuario(${JSON.stringify(u)})'><i class="fas fa-pen"></i></button>
                ${u.es_actual ? '' : `<button type="button" class="btn-eliminar" title="Eliminar"
                    onclick="eliminarUsuario(${u.id}, '${esc(u.usuario)}')"><i class="fas fa-trash-alt"></i></button>`}
            </td>
        </tr>
    `).join('');
}

// --- Modo nuevo vs editar ---
function modoNuevo() {
    document.getElementById('u-id').value = '';
    document.getElementById('form-usuario').reset();
    document.getElementById('titulo-form').innerHTML = '<i class="fas fa-user-plus" style="color:#f1c40f;"></i> Nuevo usuario';
    document.getElementById('txt-guardar').textContent = 'Crear usuario';
    document.getElementById('grupo-estado').style.display = 'none';
    document.getElementById('btn-cancelar').style.display = 'none';
    document.getElementById('ayuda-password').style.display = 'none';
    document.getElementById('label-password').textContent = 'Contraseña';
    document.getElementById('u-password').required = true;
    document.getElementById('mensaje-form').innerHTML = '';
}

function editarUsuario(u) {
    document.getElementById('u-id').value = u.id;
    document.getElementById('u-nombre').value = u.nombre;
    document.getElementById('u-usuario').value = u.usuario;
    document.getElementById('u-rol').value = u.rol;
    document.getElementById('u-activo').value = u.activo ? '1' : '0';
    document.getElementById('u-password').value = '';
    document.getElementById('titulo-form').innerHTML = `<i class="fas fa-user-edit" style="color:#f1c40f;"></i> Editar usuario: ${esc(u.usuario)}`;
    document.getElementById('txt-guardar').textContent = 'Guardar cambios';
    document.getElementById('grupo-estado').style.display = u.es_actual ? 'none' : 'flex';
    document.getElementById('btn-cancelar').style.display = 'inline-flex';
    document.getElementById('ayuda-password').style.display = 'block';
    document.getElementById('label-password').textContent = 'Restablecer contraseña (opcional)';
    document.getElementById('u-password').required = false;
    document.getElementById('mensaje-form').innerHTML = '';
    document.getElementById('card-usuario').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// --- Guardar (crear o editar) ---
document.getElementById('form-usuario').addEventListener('submit', async (e) => {
    e.preventDefault();
    const mensaje = document.getElementById('mensaje-form');
    mensaje.innerHTML = '';

    const id = document.getElementById('u-id').value;
    const body = {
        nombre: document.getElementById('u-nombre').value.trim(),
        usuario: document.getElementById('u-usuario').value.trim(),
        rol: document.getElementById('u-rol').value,
        password: document.getElementById('u-password').value,
    };

    try {
        if (id) {
            body.id = Number(id);
            body.activo = Number(document.getElementById('u-activo').value);
            await apiFetch('EditarUsuarioServlet', { method: 'POST', body: JSON.stringify(body) });
        } else {
            await apiFetch('UsuariosServlet', { method: 'POST', body: JSON.stringify(body) });
        }
        Swal.fire({
            title: id ? 'Usuario actualizado' : 'Usuario creado',
            text: id ? `Los cambios de "${body.usuario}" se guardaron.` : `El usuario "${body.usuario}" ya puede iniciar sesión.`,
            icon: 'success',
            confirmButtonColor: '#f1c40f',
        });
        modoNuevo();
        cargarUsuarios();
    } catch (err) {
        mensaje.innerHTML = `<div class="mensaje error">${esc(err.message)}</div>`;
    }
});

// --- Eliminar (baja lógica) ---
async function eliminarUsuario(id, usuario) {
    const result = await Swal.fire({
        title: '¿Eliminar este usuario?',
        text: `"${usuario}" ya no podrá iniciar sesión. Su historial de movimientos se conserva.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#444',
        confirmButtonText: '<i class="fas fa-trash-alt"></i> Sí, eliminar',
        cancelButtonText: 'Cancelar',
    });
    if (!result.isConfirmed) return;

    try {
        await apiFetch('EliminarUsuarioServlet', { method: 'POST', body: JSON.stringify({ id }) });
        Swal.fire({ title: 'Usuario eliminado', text: `"${usuario}" fue desactivado.`, icon: 'success', confirmButtonColor: '#f1c40f' });
        cargarUsuarios();
    } catch (err) {
        Swal.fire({ title: 'Error', text: err.message, icon: 'error', confirmButtonColor: '#f1c40f' });
    }
}

cargarUsuarios();
