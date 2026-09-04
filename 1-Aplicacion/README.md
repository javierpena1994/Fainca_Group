# Inventario FAINCA — Bodega #1 (Java / JSP)

Sistema de control de inventario que reemplaza los Excel dispersos por una
sola fuente de verdad en MySQL, con historial completo de movimientos.

Construido con el stack del equipo: **JSP + Servlets + JDBC + MySQL**
(proyecto Maven en `fainca/`, estructura `Objetos/`, `Dao/`, `Servlets/`).

## Roles

- **admin**: consulta completa, registrar/editar/eliminar productos, marcas,
  ingresos/egresos (multi-producto, con fecha opcional), historial.
- **ventas**: solo consulta de stock por código/descripción, en tiempo real.

## Requisitos

- Java JDK 21+ (instalado: JDK 22)
- Maven (instalado en `C:\Users\PC\apache-maven`)
- MySQL 8+ (servicio `MySQL84`, arranca automático)

## Base de datos

```bash
# Crear tablas (solo la primera vez)
mysql -u root -p < db/schema.sql
# Usuarios iniciales (contraseña temporal: CambiarClave123)
mysql -u root -p < db/usuarios-iniciales.sql
```

Conexión configurada en `fainca/src/main/resources/db.properties`.

## Ejecutar en desarrollo

```bash
cd fainca
mvn jetty:run          # levanta en http://localhost:3000/
mvn jetty:run -Dpuerto=3001   # en otro puerto si el 3000 esta ocupado
```

Usuarios de prueba: `victor` (admin) y `ventas1` (ventas), contraseña
temporal `CambiarClave123`.

## Servidor local (arranque automático + acceso desde celular)

Ejecutar `configurar-servidor.ps1` en PowerShell **como Administrador**
(instrucciones dentro del archivo). Deja la IP fija en `192.168.10.65`,
abre el puerto 3000 y crea la tarea que arranca el servidor al iniciar
sesión en esta PC.

- Desde esta PC: `http://localhost:3000/`
- Desde celulares/PCs de ventas (misma red Wi-Fi): `http://192.168.10.65:3000/`

## Estructura del proyecto

```
fainca/
├── pom.xml                      # Maven: dependencias y servidor Jetty
├── iniciar-servidor.cmd         # lanzador usado por la tarea programada
└── src/main/
    ├── java/
    │   ├── Objetos/             # Usuario, Marca, Producto, Movimiento
    │   ├── Dao/                 # Db (conexión), UsuarioDAO, MarcaDAO, ProductoDAO, MovimientoDAO
    │   ├── Servlets/            # Login, Buscar, Productos, Registrar, Editar, Eliminar,
    │   │                        #   Movimiento, Historial, Marca, CambiarPassword, Logout
    │   └── Filtros/AuthFilter   # sesión + roles en todas las rutas
    ├── resources/db.properties  # conexión MySQL
    └── webapp/
        ├── *.jsp                # login, index (buscar), registrar, eliminar,
        │                        #   ingreso, salida, historial, cambiarPassword, sidebar
        ├── css/style.css        # tema FAINCA (amarillo/gris)
        └── js/                  # busqueda en vivo, autocompletado, SweetAlert
```

## Reglas de integridad (importantes)

- El **stock nunca se edita directo**: todo cambio pasa por un movimiento
  (ingreso/egreso) que queda en el historial con usuario, fecha y observación.
- **Eliminar** un producto es baja lógica (`activo = 0`): desaparece de las
  consultas pero conserva su historial; se reactiva desde Editar.
- La **fecha** de un movimiento es opcional: por defecto es el momento del
  registro; se puede indicar manualmente para correcciones.

## Siguiente paso: pasar al servidor de la empresa

1. Instalar JDK + MySQL en el servidor (o usar el MySQL existente).
2. Crear la base con `db/schema.sql` + `db/usuarios-iniciales.sql`.
3. Copiar la carpeta `fainca/`, ajustar `db.properties`.
4. `mvn package` genera `target/fainca.war` para desplegar en Tomcat,
   o correr con `mvn jetty:run` como en esta PC.
