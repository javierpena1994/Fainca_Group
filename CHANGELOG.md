# Registro de Cambios (Changelog) — Sistema de Inventario FAINCA

Todas las modificaciones notables realizadas en este proyecto están documentadas en este archivo, siguiendo las pautas de [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y versionado semántico.

---

## [2.0.0] - 2026-08-12

### ✨ Añadido
- **Módulo de Bodega de Herramientas y Consumibles**:
  - Catálogo especializado con contadores en tiempo real: `Disponible`, `En Proyectos`, `Dañadas` y `Total`.
  - Diferenciación operativa entre ítems prestables (`herramienta`) e insumos consumibles (`consumible`).
  - Módulo de **Emisión de Actas de Entrega** con generación automática de comprobantes oficiales en PDF (`HER-XXXXXX.pdf`) y casillas para firma de entrega/recepción física.
  - Módulo de **Liquidación y Devoluciones de Actas** permitiendo retornos en buen estado, registro de averías/daños, declaración de pérdidas y cierre automático del acta.
  - Taller de resolución de unidades dañadas con acciones de **Reparación** (reincorporación a disponible) o **Baja Definitiva** (descuento del total).
  - Libro mayor de auditoría agrupado por lotes de operación en `historialHerramientas.jsp`.
- **Gestión Avanzada de Usuarios y Seguridad**:
  - Introducción del rol **`superadmin`** con privilegios exclusivos para creación, edición, reseteo de claves y activación/desactivación de operadores en `usuarios.jsp`.
  - Protección de fuerza bruta: bloqueo temporal de cuentas por 5 minutos ante 5 intentos fallidos consecutivos.
  - Cifrado seguro de contraseñas con **BCrypt** (cost factor 10).
- **Suite Documental Exhaustiva**:
  - Documentación técnica y funcional completa en `4-Documentacion/` (Arquitectura, Base de Datos, Manual de Usuario, Guía de Despliegue y Matriz de Endpoints).
  - Configuración profesional para Git (`.gitignore`, `.gitattributes`, `README.md`, `CONTRIBUTING.md`).

---

## [1.5.0] - 2026-07-22

### ✨ Añadido
- **Módulo de Bodega #1 (Productos y Repuestos)**:
  - Búsqueda asíncrona en tiempo real con debounce por código, descripción y percha.
  - Catálogo visual con carga y visualización de fotografías en alta resolución (`3-Imagenes-de-productos/`).
  - Operaciones multi-producto por lotes: Ingresos masivos con selección de fecha retroactiva y Egresos con validación de stock.
  - Pantalla de **Ajustes de Inventario** para reconteos físicos periódicos y reubicación de perchas.
  - Libro mayor de auditoría inmutable en `historial.jsp` con soporte de **Correcciones Aclaratorias** sin alterar el registro original.
  - Exportación de catálogo general e inventario valorizado a formatos **Microsoft Excel (.xlsx)** y **PDF corporativo**.
- **Filtro de Control de Acceso (`AuthFilter`)**:
  - Aislamiento de rutas por rol (`admin` y `ventas`).
  - Manejo de respuestas AJAX con códigos de estado HTTP 401 y 403 en JSON.

---

## [1.0.0] - 2026-06-15

### ✨ Añadido
- Migración y consolidación inicial del inventario físico y hojas de cálculo dispersas a una base de datos centralizada en MySQL 8.
- Estructura base del proyecto Java con Jakarta Servlet 5.0 y Jetty 11.
- Tablas iniciales `marcas`, `productos`, `usuarios` y `movimientos`.

