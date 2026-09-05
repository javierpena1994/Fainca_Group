# Módulo de Aplicación — Sistema de Inventario FAINCA (Java EE / JSP)

Núcleo de software del **Sistema de Inventario FAINCA**, desarrollado con arquitectura multicapa desacoplada sobre el stack **Jakarta Servlet 5.0 + JSP + JDBC + MySQL 8** con empaquetado Apache Maven.

---

## 🏗 Arquitectura del Código Fuente (`fainca/`)

```text
fainca/
├── pom.xml                               # Configuración de dependencias Maven y plugin Jetty 11
├── iniciar-servidor.sh                  # Lanzador del servidor de desarrollo para macOS/Linux
├── config/                               # Infraestructura de certificados y configuración HTTPS
│   ├── LEEME-HTTPS.md                   # Procedimiento de activación de TLS
│   ├── fainca-certificado.cer           # Certificado público
│   ├── fainca-ssl.p12                   # Almacén de claves PKCS12
│   └── jetty-ssl.xml                    # Configuración SSL para Jetty
└── src/main/
    ├── java/
    │   ├── Dao/                         # Capa de Acceso a Datos (JDBC + PreparedStatements)
    │   │   ├── Db.java                  # Administrador central de conexiones MySQL
    │   │   ├── ProductoDAO.java         # CRUD y control de stock de Bodega #1
    │   │   ├── MovimientoDAO.java       # Libro mayor de movimientos de Bodega #1
    │   │   ├── HerramientaDAO.java      # Control de herramientas, consumibles y contadores
    │   │   ├── ActaHerramientaDAO.java  # Gestión transaccional de actas y líneas de devolución
    │   │   ├── UsuarioDAO.java          # Autenticación, bloqueo por fuerza bruta y usuarios
    │   │   ├── MarcaDAO.java            # Gestión de marcas comerciales
    │   │   ├── ReporteDAO.java          # Consultas para exportación y dashboard
    │   │   └── Codigo.java              # Utilidades de normalización y códigos
    │   ├── Filtros/                     # Interceptores de peticiones HTTP
    │   │   ├── AuthFilter.java          # Control de autenticación y roles (superadmin, admin, ventas)
    │   │   └── CacheFilter.java         # Encabezados de control de caché en respuestas
    │   ├── Objetos/                     # Modelos de Dominio / POJOs
    │   │   ├── Producto.java            # Entidad de producto de Bodega #1
    │   │   ├── Movimiento.java          # Registro de auditoría de Bodega #1
    │   │   ├── Herramienta.java         # Entidad de herramienta/consumible
    │   │   ├── ActaHerramienta.java     # Cabecera de acta de entrega
    │   │   ├── ActaLineaHerramienta.java# Detalle y estado de liquidación por ítem
    │   │   ├── MovimientoHerramienta.java # Auditoría de movimientos de herramientas
    │   │   ├── Usuario.java             # Usuario del sistema y evaluación de rol
    │   │   └── Marca.java               # Marca de producto
    │   ├── Reportes/                    # Generadores de documentos ofimáticos
    │   │   ├── ActaHerramientasPdf.java # Emisión del documento oficial de acta de herramientas
    │   │   ├── ComprobanteMovimiento.java # Comprobante de ingreso/egreso
    │   │   ├── ReportePdf.java          # Reportes consolidados en PDF (OpenPDF)
    │   │   └── ReporteExcel.java        # Exportación de inventario a .xlsx (Apache POI)
    │   └── Servlets/                    # Controladores Web (33 Servlets con @WebServlet)
    ├── resources/
    │   ├── db.properties                # Configuración activa de base de datos y rutas
    │   └── db.properties.example        # Plantilla de referencia para nuevos entornos
    └── webapp/
        ├── *.jsp                        # Vistas web (login, dashboard, index, actas, etc.)
        ├── css/style.css                # Estilos corporativos FAINCA (paleta amarillo/gris)
        ├── js/                          # Lógica frontend asíncrona (Fetch API, SweetAlert2)
        ├── images/                      # Logotipos e isotipos estáticos de la interfaz
        └── vendor/                      # Librerías cliente locales (FontAwesome, etc.)
```

---

## 👥 Control de Acceso Basado en Roles (RBAC)

| Rol | Alcance y Pantallas Permitidas |
|---|---|
| **`superadmin`** | Acceso irrestricto a todo el sistema. Es el **único rol** facultado para acceder a `usuarios.jsp` (`/UsuariosServlet`, `/EditarUsuarioServlet`, `/EliminarUsuarioServlet`) y gestionar cuentas/contraseñas. |
| **`admin`** | Control operativo total sobre Bodega #1 (alta, edición, baja, ingresos, egresos, ajustes), Bodega de Herramientas (catálogo, actas, devoluciones, reparaciones), consulta de historial completo y generación de reportes. |
| **`ventas`** | Consulta de stock en tiempo real (`index.jsp`), visualización de fotos de productos, exportación de reportes (`reportes.jsp`) y cambio de su propia clave (`cambiarPassword.jsp`). |

---

## 🔒 Reglas Críticas de Integridad de Datos

1. **Inmutabilidad del Stock Directo**:
   El stock de un producto o herramienta jamás se modifica mediante un simple `UPDATE ... stock = X`. Toda variación pasa obligatoriamente por una transacción controlada en la capa DAO que registra de forma simultánea el movimiento en el libro mayor (`movimientos` o `movimientos_herramientas`).
2. **Balance de Herramientas**:
   Para todo ítem de tipo herramienta, se cumple rigurosamente la ecuación contable:
   $$\text{En Proyectos} = \text{Cantidad Total} - \text{Cantidad Disponible} - \text{Cantidad Dañada}$$
   Cualquier devolución actualiza `disponible` (si fue en buen estado), `danada` (si sufrió avería) o descuenta el `total` (si fue declarada perdida).
3. **Baja Lógica / Soft Delete**:
   Los productos y herramientas eliminados no se borran físicamente de la base de datos (`activo = 0`), protegiendo la integridad referencial de todos los movimientos y actas pasadas. Pueden ser reactivados desde la interfaz de edición.
4. **Protección contra Inyección SQL**:
   El 100% de las sentencias SQL ejecutadas en la capa DAO utilizan `PreparedStatement` con asignación explícita de parámetros tipados.

---

## 🚀 Comandos de Construcción y Ejecución

### Ejecución en Desarrollo con Jetty:
```bash
cd fainca
mvn jetty:run
```

### Empaquetado de Producción (WAR):
```bash
cd fainca
mvn clean package
```
El archivo `target/fainca.war` generado está listo para desplegarse en **Apache Tomcat 10.1+** o **Eclipse Jetty 11+**.
