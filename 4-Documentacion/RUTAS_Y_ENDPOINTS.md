# Matriz de Rutas, Servlets y Endpoints API — FAINCA Group

Este documento proporciona la referencia técnica completa de todos los **33 Servlets** que componen la capa de controladores del sistema, detallando sus rutas HTTP, métodos aceptados, parámetros, formatos de respuesta y políticas de autorización por rol.

---

## 🧭 1. Resumen de Niveles de Autorización

- **Público**: Accesible sin sesión iniciada (recursos estáticos, login).
- **`ventas`**: Requiere sesión con rol `ventas`, `admin` o `superadmin`.
- **`admin`**: Requiere sesión con rol `admin` o `superadmin`.
- **`superadmin`**: Requiere sesión exclusiva con rol `superadmin`.

---

## 📋 2. Matriz de Controladores y Endpoints

### 2.1 Autenticación y Sesión de Usuario

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/LoginServlet` | `POST` | `usuario`, `password` | JSON / Redirección | **Público** | Valida credenciales con BCrypt, gestiona bloqueo por fuerza bruta e inicia sesión HTTP. |
| `/LogoutServlet` | `GET` / `POST` | *Ninguno* | Redirección (`/login.jsp`) | **`ventas`** | Invalida la sesión actual del usuario. |
| `/CambiarPasswordServlet` | `POST` | `passwordActual`, `passwordNueva` | JSON (`{"ok": bool, "mensaje": str}`) | **`ventas`** | Permite al usuario autenticado cambiar su propia contraseña. |

---

### 2.2 Bodega #1: Productos y Catálogo

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/BuscarProductosServlet` | `GET` | `q` (texto), `marcaId` (opcional), `pagina` | JSON (`{"productos": [...]}`) | **`ventas`** | Búsqueda asíncrona de productos con debounce en tiempo real. |
| `/ProductosServlet` | `GET` | `codigo` | JSON (`{"producto": {...}}`) | **`ventas`** | Obtiene la ficha técnica y stock actual de un producto específico. |
| `/RegistrarProductoServlet`| `POST` | `codigo`, `nombre`, `marcaId`, `descripcion`, `unidad`, `stockInicial`, `ubicacion`, `foto` (Multipart) | JSON / Redirección | **`admin`** | Registra un nuevo producto, guarda la foto y genera movimiento de apertura. |
| `/EditarProductoServlet` | `POST` | `codigo`, `nombre`, `marcaId`, `descripcion`, `unidad`, `ubicacion`, `foto` (Multipart), `activo` | JSON / Redirección | **`admin`** | Modifica información del producto o reactiva una baja lógica. |
| `/EliminarProductoServlet`| `POST` | `codigo` | JSON (`{"ok": true}`) | **`admin`** | Realiza baja lógica del producto (`activo = 0`). |
| `/RenombrarCodigoServlet` | `POST` | `codigoActual`, `nuevoCodigo` | JSON (`{"ok": true}`) | **`admin`** | Renombra la clave primaria física actualizando en cascada sus movimientos. |
| `/ImagenServlet` | `GET` | `foto` (nombre de archivo) | Stream de Imagen (`image/jpeg`, `image/png`) | **`ventas`** | Sirve de forma segura las fotos de los productos desde el disco externo. |

---

### 2.3 Bodega #1: Movimientos, Ajustes e Historial

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/MovimientoServlet` | `POST` | `tipo` (`ingreso`/`egreso`), `fecha`, `observacion`, `items` (JSON Array con `codigo` y `cantidad`) | JSON (`{"ok": true}`) | **`admin`** | Ejecuta ingresos o egresos masivos multi-producto en una sola transacción. |
| `/AjusteServlet` | `POST` | `codigo`, `nuevoStock`, `nuevaUbicacion`, `motivo` | JSON (`{"ok": true}`) | **`admin`** | Aplica ajustes por reconteo físico o cambio de percha con justificación. |
| `/HistorialServlet` | `GET` | `desde`, `hasta`, `tipo`, `codigo`, `usuarioId`, `pagina` | JSON / HTML | **`admin`** | Consulta paginada del libro mayor de movimientos de Bodega #1. |
| `/CorregirMovimientoServlet`| `POST`| `movimientoId`, `aclaracion` | JSON (`{"ok": true}`) | **`admin`** | Añade una nota aclaratoria de auditoría sobre un movimiento pasado. |

---

### 2.4 Bodega #1: Marcas y Dashboard

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/MarcaServlet` | `GET` / `POST` | `nombre` (en POST) | JSON (`{"marcas": [...]}`) | **`admin`** | Lista todas las marcas o crea una nueva marca comercial. |
| `/DashboardServlet` | `GET` | *Ninguno* | JSON / HTML | **`admin`** | Métricas consolidadas: total productos, valorizados, movimientos del mes. |

---

### 2.5 Bodega de Herramientas: Catálogo y Balance

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/HerramientasServlet` | `GET` | `filtro` (opcional: `reposicion`, `danadas`), `q` | JSON (`{"herramientas": [...]}`) | **`admin`** | Consulta del catálogo con balance de 4 contadores (Disponible, En Proyectos, Dañadas, Total). |
| `/RegistrarHerramientaServlet` | `POST` | `nombre`, `tipo` (`herramienta`/`consumible`), `stockMinimo`, `observaciones` | JSON (`{"ok": true}`) | **`admin`** | Da de alta una nueva herramienta/consumible con nombre único en mayúsculas. |
| `/StockHerramientaServlet` | `GET` | `id` | JSON (`{"herramienta": {...}}`) | **`admin`** | Consulta en tiempo real de contadores y disponibilidad de un ítem. |
| `/RenombrarHerramientaServlet`| `POST`| `id`, `nuevoNombre` | JSON (`{"ok": true}`) | **`admin`** | Corrige la redacción del nombre registrando el cambio en el historial. |
| `/AjusteHerramientasServlet` | `POST` | `items` (JSON Array con `id`, `deltaCantidad`, `observacion`), `motivoGeneral` | JSON (`{"ok": true}`) | **`admin`** | Ajuste transaccional de cantidades totales/disponibles (compras o mermas). |
| `/OperacionHerramientasServlet`| `POST`| `herramientaId`, `accion` (`reparar`/`baja`), `cantidad`, `motivo` | JSON (`{"ok": true}`) | **`admin`** | Resuelve unidades dañadas: las reincorpora a Disponible o las descuenta del Total. |

---

### 2.6 Bodega de Herramientas: Actas y Devoluciones

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/ActaHerramientasServlet` | `POST` | `solicitante`, `proyecto`, `destino`, `observaciones`, `lineas` (JSON con `herramientaId`, `cantidad`, `obs`) | JSON (`{"ok": true, "actaId": int}`) | **`admin`** | Emisión transaccional de acta de entrega, descuento de disponible y apertura de expediente. |
| `/DevolucionActaServlet` | `POST` | `actaId`, `devoluciones` (JSON con `lineaId`, `ok`, `danado`, `perdido`, `consumido`), `obs` | JSON (`{"ok": true, "cerrada": bool}`) | **`admin`** | Procesa devoluciones parciales o totales y cierra el acta si todo quedó saldado. |
| `/HistorialHerramientasServlet`| `GET` | `desde`, `hasta`, `tipo`, `herramientaId`, `pagina` | JSON / HTML | **`admin`** | Libro mayor agrupado por lotes de operaciones sobre herramientas. |

---

### 2.7 Módulo de Reportes y Documentos Oficiales

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/ExportarServlet` | `GET` | `formato` (`excel`/`pdf`), `marcaId`, `filtro` | Archivo binario (`.xlsx` o `.pdf`) | **`ventas`** | Exportación del catálogo general de existencias e inventario valorizado. |
| `/ExportarActaServlet` | `GET` | `actaId` | Documento PDF (`application/pdf`) | **`admin`** | Genera y descarga el comprobante oficial de Acta de Entrega (`HER-XXXXXX.pdf`) con casillas de firma. |
| `/ExportarMovimientoServlet` | `GET` | `loteId` o `movimientoId` | Documento PDF (`application/pdf`) | **`admin`** | Emite el comprobante oficial de ingreso o egreso de Bodega #1. |
| `/DocumentoServlet` | `GET` | `tipo`, `id` | Stream de Documento | **`admin`** | Servidor genérico de documentos anexos y respaldos. |

---

### 2.8 Administración de Usuarios (Superadmin)

| Ruta (`@WebServlet`) | Método | Parámetros | Formato Respuesta | Rol Mínimo | Descripción |
|---|:---:|---|:---:|:---:|---|
| `/UsuariosServlet` | `GET` / `POST` | `nombre`, `usuario`, `password`, `rol` (en POST) | JSON (`{"usuarios": [...]}`) / (`{"ok": true}`) | **`superadmin`** | Lista todas las cuentas o crea un nuevo usuario en el sistema. |
| `/EditarUsuarioServlet` | `POST` | `id`, `nombre`, `rol`, `passwordNueva` (opcional), `activo` | JSON (`{"ok": true}`) | **`superadmin`** | Modifica rol, nombre, estado activo o restablece la clave de cualquier usuario. |
| `/EliminarUsuarioServlet` | `POST` | `id` | JSON (`{"ok": true}`) | **`superadmin`** | Desactiva una cuenta de usuario del sistema. |

