# Manual de Usuario Integral — Sistema de Inventario FAINCA

Bienvenido al manual operativo oficial del **Sistema de Gestión de Inventario FAINCA**. Esta guía detalla el uso de todas las funciones y módulos de la plataforma tanto para operadores de bodega como para el equipo de ventas y administradores.

---

## 📑 Contenido del Manual

1. [Acceso al Sistema y Roles](#1-acceso-al-sistema-y-roles)
2. [Navegación General y Conmutación de Bodegas](#2-navegación-general-y-conmutación-de-bodegas)
3. [Módulo: Bodega #1 (Productos y Repuestos)](#3-módulo-bodega-1-productos-y-repuestos)
   - [3.1 Búsqueda y Consulta de Stock en Tiempo Real](#31-búsqueda-y-consulta-de-stock-en-tiempo-real)
   - [3.2 Registro de Nuevos Productos y Fotografías](#32-registro-de-nuevos-productos-y-fotografías)
   - [3.3 Ingresos de Inventario (Entradas Multi-producto)](#33-ingresos-de-inventario-entradas-multi-producto)
   - [3.4 Salidas de Inventario (Egresos Multi-producto)](#34-salidas-de-inventario-egresos-multi-producto)
   - [3.5 Ajustes de Inventario y Reconteo Físico](#35-ajustes-de-inventario-y-reconteo-físico)
   - [3.6 Edición, Baja Lógica y Reactivación](#36-edición-baja-lógica-y-reactivación)
   - [3.7 Libro Mayor de Historial y Correcciones](#37-libro-mayor-de-historial-y-correcciones)
4. [Módulo: Bodega de Herramientas y Consumibles](#4-módulo-bodega-de-herramientas-y-consumibles)
   - [4.1 Comprensión del Catálogo y los 4 Contadores](#41-comprensión-del-catálogo-y-los-4-contadores)
   - [4.2 Registro de Nuevas Herramientas y Consumibles](#42-registro-de-nuevas-herramientas-y-consumibles)
   - [4.3 Ajuste Masivo de Herramientas](#43-ajuste-masivo-de-herramientas)
   - [4.4 Emisión de Actas de Entrega y Generación de PDF](#44-emisión-de-actas-de-entrega-y-generación-de-pdf)
   - [4.5 Gestión de Devoluciones y Liquidación de Actas](#45-gestión-de-devoluciones-y-liquidación-de-actas)
   - [4.6 Taller: Resolución de Unidades Dañadas (Reparación / Baja)](#46-taller-resolución-de-unidades-dañadas-reparación--baja)
   - [4.7 Historial Agrupado de Herramientas](#47-historial-agrupado-de-herramientas)
5. [Módulo: Reportes y Exportación](#5-módulo-reportes-y-exportación)
6. [Módulo: Administración de Usuarios (Superadmin)](#6-módulo-administración-de-usuarios-superadmin)
7. [Preguntas Frecuentes y Solución de Problemas](#7-preguntas-frecuentes-y-solución-de-problemas)

---

## 🔐 1. Acceso al Sistema y Roles

### Inicio de Sesión
1. Abra su navegador web e ingrese a la dirección del servidor (ej. `http://localhost:3000/` o la IP de red local asignada por TI).
2. Introduzca su **Nombre de Usuario** y **Contraseña**.
3. Haga clic en **Iniciar Sesión**.

### Niveles de Acceso (Roles)
- **`superadmin` (Administrador General)**: Acceso total al sistema y gestión de usuarios (crear cuentas, resetear claves y dar de baja operadores).
- **`admin` (Administrador de Bodega)**: Control operativo integral de Bodega #1, Bodega de Herramientas, Actas, Ajustes, Historiales y Reportes.
- **`ventas` (Consulta y Ventas)**: Consulta de existencias de productos en tiempo real, visualización de fotografías, exportación de inventario y cambio de clave personal.

> **Seguridad**: El sistema bloquea temporalmente una cuenta durante **5 minutos** si se ingresa una contraseña errónea 5 veces consecutivas.

### Cambio de Contraseña
Para modificar su contraseña, diríjase al enlace ubicado en el pie del menú lateral: **Cambiar Contraseña**. Requiere ingresar la contraseña actual y la nueva clave dos veces para confirmación.

---

## 🧭 2. Navegación General y Conmutación de Bodegas

En la parte superior del menú lateral (sidebar) encontrará dos botones de alternancia rápida:

- 🏠 **Botón Casita (`Bodega #1`)**: Abre el entorno comercial de repuestos, productos, ingresos/egresos y catálogo con fotos.
- 🧰 **Botón Caja de Herramientas (`Bodega de Herramientas`)**: Cambia la interfaz al entorno de herramientas, actas de préstamo, devoluciones y control de daños.

---

## 📦 3. Módulo: Bodega #1 (Productos y Repuestos)

### 3.1 Búsqueda y Consulta de Stock en Tiempo Real
En la pantalla principal (`index.jsp`):
- **Búsqueda instantánea**: Escriba en la barra superior cualquier coincidencia por **código**, **nombre** o **ubicación física**.
- **Filtro por Marca**: Seleccione una marca en la lista desplegable para ver solo sus repuestos.
- **Fotografías**: Al hacer clic sobre la miniatura o ícono de imagen, se despliega la fotografía de referencia en alta resolución del producto.

### 3.2 Registro de Nuevos Productos y Fotografías
*(Disponible para `admin` y `superadmin` en `registrarProductos.jsp`)*:
1. Ingrese el **Código** físico del repuesto (clave primaria única).
2. Seleccione la **Marca** (o cree una nueva desde el botón lateral `+`).
3. Complete el **Nombre**, **Descripción**, **Unidad de Medida** (unidad, metro, juego, etc.) y **Ubicación en percha** (ej. `BA01`).
4. Ingrese la **Cantidad Inicial** (se registrará automáticamente como movimiento de apertura).
5. Adjunte la **Fotografía** del producto (.jpg, .png).
6. Haga clic en **Guardar Producto**.

### 3.3 Ingresos de Inventario (Entradas Multi-producto)
*(Pantalla `ingresoInventario.jsp`)*:
Permite registrar la llegada de múltiples ítems en un solo documento:
1. Seleccione la **Fecha** del ingreso (permite fecha actual o correcciones retroactivas).
2. Ingrese el **Comprobante / Observación** general (ej. `Factura Proveedor #12345`).
3. Agregue filas seleccionando el producto y la cantidad que ingresa.
4. Presione **Confirmar Ingreso**. El sistema validará todas las líneas y actualizará el stock en una única transacción atómica.

### 3.4 Salidas de Inventario (Egresos Multi-producto)
*(Pantalla `salidaInventario.jsp`)*:
Permite registrar despachos o ventas:
1. Indique el **Documento / Motivo** (ej. `Guía de Despacho Cliente XYZ`).
2. Añada los productos y cantidades a retirar.
3. *Validación automática*: El sistema impide registrar egresos si la cantidad solicitada supera el stock disponible en bodega.
4. Al confirmar, se descuenta el stock y se registra en el historial.

### 3.5 Ajustes de Inventario y Reconteo Físico
*(Pantalla `ajusteInventario.jsp`)*:
Diseñada para cuadres tras inventarios físicos periódicos:
- Permite ingresar el nuevo conteo físico real o la nueva ubicación en percha.
- Exige obligatoriamente un **Motivo del Ajuste** para auditoría interna.

### 3.6 Edición, Baja Lógica y Reactivación
- **Editar**: Permite modificar descripción, ubicación física, marca y actualizar la fotografía.
- **Baja Lógica (Eliminar)**: Desactiva el producto sin destruir su historial de movimientos. Si en el futuro se vuelve a comercializar, se reactiva con un solo clic desde la pantalla de edición.

### 3.7 Libro Mayor de Historial y Correcciones
*(Pantalla `historial.jsp`)*:
- Muestra cronológicamente todas las operaciones de Bodega #1.
- Permite filtrar por rango de fechas, tipo de movimiento (`ingreso`, `egreso`, `ajuste`, `edicion`) o usuario.
- **Correcciones documentales**: Si se digitó una observación errónea, un administrador puede añadir una aclaración que queda registrada como evento de `corrección`, preservando la auditoría original.

---

## 🧰 4. Módulo: Bodega de Herramientas y Consumibles

### 4.1 Comprensión del Catálogo y los 4 Contadores
A diferencia de los productos de venta, las herramientas salen a proyectos y deben regresar. Por ello, el catálogo muestra 4 columnas de balance:

$$\begin{array}{|c|c|c|c|}
\hline
\textbf{Disponible} & \textbf{En Proyectos} & \textbf{Dañadas} & \textbf{Total} \\
\hline
\text{En bodega lista para prestar} & \text{En campo con un técnico} & \text{Averiadas / En taller} & \text{Inventario global} \\
\hline
\end{array}$$

$$\text{En Proyectos} = \text{Total} - \text{Disponible} - \text{Dañadas}$$

- **Herramienta**: Su stock "Total" se mantiene cuando sale; disminuye "Disponible" y aumenta "En Proyectos".
- **Consumible**: Al entregarse, se descuenta inmediatamente del "Total" porque no retorna.

### 4.2 Registro de Nuevas Herramientas y Consumibles
*(Pantalla `herramientas.jsp`)*:
1. Presione **Nueva Herramienta / Consumible**.
2. Escriba el **Nombre** (el sistema lo convierte automáticamente a MAYÚSCULAS y verifica que no existan duplicados).
3. Seleccione el **Tipo**: `Herramienta` o `Consumible`.
4. Defina el **Stock Mínimo** (opcional, para alertas visuales de reposición).
5. Guarde el registro.

### 4.3 Ajuste Masivo de Herramientas
*(Pantalla `ajusteHerramientas.jsp`)*:
Permite incorporar compras de herramientas, dar de alta lotes o corregir existencias indicando la cantidad a sumar o restar y la justificación.

### 4.4 Emisión de Actas de Entrega y Generación de PDF
*(Pantalla `actaHerramientas.jsp`)*:
1. Ingrese los datos de cabecera:
   - **Solicitante**: Nombre del técnico responsable.
   - **Proyecto**: Nombre del cliente, obra o planta.
   - **Destino**: Ubicación física de los trabajos.
   - **Observaciones**: Placa de vehículo, notas de entrega, etc.
2. Agregue las herramientas y consumibles que se entregan, con sus cantidades y observaciones puntuales (ej. números de serie).
3. Presione **Generar Acta de Entrega**.
4. El sistema guardará el acta, descontará las herramientas disponibles y abrirá automáticamente el documento oficial en **PDF (`HER-XXXXXX.pdf`)** listo para imprimir y firmar físicamente.

### 4.5 Gestión de Devoluciones y Liquidación de Actas
*(Pantalla `actasHerramientas.jsp`)*:
Muestra todas las actas abiertas con ítems pendientes de retornar:
1. Busque y despliegue el acta correspondiente.
2. En cada ítem que el técnico devuelve, registre:
   - **Bien**: Unidades que retornan operativas $\rightarrow$ Vuelven a `Disponible`.
   - **Dañado**: Unidades rotas $\rightarrow$ Pasan al contador de `Dañadas` (no se pueden volver a prestar hasta que se reparen).
   - **Perdido**: Unidades extraviadas $\rightarrow$ Se descuentan del `Total` de la empresa.
3. Presione **Guardar Devolución**.
4. El acta admite devoluciones parciales y **se cierra automáticamente** cuando todas sus líneas han sido completamente saldadas.

### 4.6 Taller: Resolución de Unidades Dañadas (Reparación / Baja)
Desde el catálogo de herramientas, en cualquier ítem que tenga unidades dañadas $> 0$, haga clic en el botón de la **Llave inglesa**:
- **Reparar**: Devuelve las unidades al estado `Disponible`.
- **Dar de Baja**: Retira definitivamente las unidades dañadas del `Total` de la empresa con justificación técnica.

### 4.7 Historial Agrupado de Herramientas
*(Pantalla `historialHerramientas.jsp`)*:
Presenta una fila consolidada por cada operación (lote). Al hacer clic en una fila, se despliega el desglose exacto de herramientas entregadas, devueltas o reparadas en esa transacción.

---

## 📊 5. Módulo: Reportes y Exportación

*(Pantalla `reportes.jsp`)*:
- **Exportar a Microsoft Excel (.xlsx)**: Genera un archivo descargable con el inventario completo, clasificado por marcas, descripciones, ubicaciones físicas y existencias.
- **Exportar a PDF**: Genera catálogos formateados con encabezados corporativos de FAINCA Group listos para auditorías o inventarios impresos.

---

## 👥 6. Módulo: Administración de Usuarios (Superadmin)

*(Pantalla `usuarios.jsp` — Exclusiva para `superadmin`)*:
- **Crear Nuevo Usuario**: Define nombre completo, identificador de usuario, contraseña inicial y rol asignado (`superadmin`, `admin`, `ventas`).
- **Editar Usuario**: Permite actualizar el nombre, rol o restablecer la contraseña en caso de olvido.
- **Desactivar / Reactivar**: Inhabilita el acceso de un operador sin borrar los movimientos que haya registrado en el pasado.

---

## ❓ 7. Preguntas Frecuentes y Solución de Problemas

#### ¿Por qué no puedo editar el stock directamente en la pantalla de productos?
Por seguridad y auditoría. Todo cambio de existencias debe provenir de un ingreso, egreso o ajuste justificado para que quede registrado quién lo hizo y cuándo.

#### ¿Qué ocurre si intento prestar una herramienta sin stock disponible?
El sistema valida el balance en tiempo real y mostrará una alerta de error impidiendo la emisión del acta hasta que haya unidades disponibles o se ajuste el inventario.

#### ¿Por qué el navegador muestra un aviso de "No Seguro"?
La aplicación utiliza por defecto el protocolo HTTP para facilitar la conexión en red local de bodega sin requerir la instalación manual de certificados autofirmados en cada teléfono o PC. Para activar HTTPS formalmente, consulte la [Guía de Despliegue y Seguridad](GUIA_DESPLIEGUE_Y_SEGURIDAD.md).

