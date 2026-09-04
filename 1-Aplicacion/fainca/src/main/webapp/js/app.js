// Utilidades compartidas por todas las paginas.
// La sesion viaja en la cookie JSESSIONID (la maneja el servidor).

// Abre/cierra el menu en telefonos (el boton hamburguesa solo se ve ahi).
function toggleMenu() {
    document.querySelector('.sidebar').classList.toggle('abierto');
}

// Escapa texto antes de insertarlo en HTML para evitar XSS con datos
// guardados (descripciones, observaciones, etc.)
function esc(texto) {
    if (texto === null || texto === undefined) return '';
    return String(texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// Llama a un Servlet y devuelve el JSON. Si la sesion expiro, vuelve al login.
async function apiFetch(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', Accept: 'application/json', ...(options.headers || {}) };
    const res = await fetch(url, { ...options, headers });
    if (res.status === 401) {
        window.location.href = 'login.jsp';
        throw new Error('Sesion expirada');
    }
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || 'Error en la peticion');
    return data;
}

// Sube la foto de referencia de un producto (multipart: el navegador pone el
// Content-Type con boundary solo, por eso NO se usa apiFetch aqui).
//
// esAlta = true cuando la foto llega junto con el alta del producto: en ese caso
// el servidor no la anota como "edicion" en el historial, porque el alta ya quedo
// registrada como ingreso.
async function subirImagenProducto(codigo, archivo, esAlta = false) {
    const fd = new FormData();
    fd.append('codigo', codigo);
    fd.append('imagen', archivo);
    if (esAlta) fd.append('nuevo', '1');
    const res = await fetch('ImagenServlet', { method: 'POST', body: fd });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || 'No se pudo subir la imagen');
    return data;
}

// Zona para elegir la foto del producto: se puede hacer clic para buscarla,
// arrastrarla desde una carpeta, o pegarla con Ctrl+V (por ejemplo, copiada
// desde la web del fabricante o desde el Excel).
//
// El archivo elegido se mete en el <input type="file"> mediante DataTransfer, para
// que el resto del formulario lo lea igual que si se hubiera usado "Examinar".
function inicializarZonaImagen(zona) {
    if (!zona) return;
    const input = zona.querySelector('input[type=file]');
    const vacia = zona.querySelector('.zona-vacia');
    const preview = zona.querySelector('.zona-preview');
    const btnQuitar = zona.querySelector('.zona-quitar');
    const nombre = zona.querySelector('.zona-nombre');
    let urlPreview = null;

    function limpiarUrl() {
        if (urlPreview) { URL.revokeObjectURL(urlPreview); urlPreview = null; }
    }

    function aplicar(archivo) {
        if (!archivo || !archivo.type.startsWith('image/')) return false;
        const dt = new DataTransfer();
        dt.items.add(archivo);
        input.files = dt.files;
        limpiarUrl();
        urlPreview = URL.createObjectURL(archivo);
        preview.src = urlPreview;
        preview.hidden = false;
        vacia.hidden = true;
        btnQuitar.hidden = false;
        if (nombre) {
            const kb = Math.round(archivo.size / 1024);
            nombre.textContent = (archivo.name || 'imagen pegada') + ' (' + kb + ' KB)';
            nombre.hidden = false;
        }
        zona.classList.add('con-imagen');
        return true;
    }

    // Muestra la foto que el producto ya tiene guardada (al abrir "editar").
    zona.mostrarExistente = (url) => {
        input.value = '';
        limpiarUrl();
        if (url) {
            preview.src = url;
            preview.hidden = false;
            vacia.hidden = true;
            btnQuitar.hidden = false;
            zona.classList.add('con-imagen');
        } else {
            zona.limpiar();
        }
        if (nombre) nombre.hidden = true;
    };

    zona.limpiar = () => {
        input.value = '';
        limpiarUrl();
        preview.hidden = true;
        preview.removeAttribute('src');
        vacia.hidden = false;
        btnQuitar.hidden = true;
        if (nombre) nombre.hidden = true;
        zona.classList.remove('con-imagen');
    };

    zona.addEventListener('click', (e) => {
        if (e.target.closest('.zona-quitar')) return;
        input.click();
    });
    input.addEventListener('change', () => aplicar(input.files[0]));
    btnQuitar.addEventListener('click', (e) => { e.stopPropagation(); zona.limpiar(); });

    ['dragenter', 'dragover'].forEach((ev) => zona.addEventListener(ev, (e) => {
        e.preventDefault();
        zona.classList.add('arrastrando');
    }));
    ['dragleave', 'drop'].forEach((ev) => zona.addEventListener(ev, (e) => {
        e.preventDefault();
        if (ev === 'dragleave' && zona.contains(e.relatedTarget)) return;
        zona.classList.remove('arrastrando');
    }));
    zona.addEventListener('drop', (e) => {
        const archivo = e.dataTransfer.files[0];
        if (!aplicar(archivo)) {
            Swal.fire({ title: 'Eso no es una imagen', text: 'Arrastra un archivo de imagen (JPG, PNG...).', icon: 'warning', confirmButtonColor: '#f1c40f' });
        }
    });

    // Pegar con Ctrl+V en cualquier parte de la pagina (mientras no se este
    // escribiendo en un campo de texto, para no robarle el pegado al usuario).
    document.addEventListener('paste', (e) => {
        const escribiendo = document.activeElement
            && ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)
            && document.activeElement.type !== 'file';
        if (escribiendo) return;
        const item = [...(e.clipboardData?.items || [])].find((i) => i.type.startsWith('image/'));
        if (!item) return;
        e.preventDefault();
        aplicar(item.getAsFile());
        zona.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
}
