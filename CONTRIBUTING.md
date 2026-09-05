# Guía de Contribución y Buenas Prácticas — FAINCA Group

Agradecemos las contribuciones al **Sistema de Inventario FAINCA**. Para mantener la alta calidad del código, la seguridad de los datos de inventario y la consistencia en el repositorio Git, solicitamos seguir las directrices descritas en este documento.

---

## 🌿 1. Flujo de Trabajo con Git (GitFlow)

El repositorio sigue un esquema estructurado de ramas:

- **`main`**: Rama de producción. Contiene código probado, estable y listo para despliegue en el servidor de la empresa.
- **`develop`**: Rama de integración para desarrollo continuo y pruebas preliminares.
- **`feature/<nombre-de-funcionalidad>`**: Ramas temporales para el desarrollo de nuevas características (ej. `feature/modulo-auditoria-avanzada`).
- **`hotfix/<nombre-del-bug>`**: Correcciones críticas y urgentes sobre producción.

### Proceso para Crear un Cambio:
1. Cree su rama a partir de `develop` (o `main` según corresponda):
   ```bash
   git checkout -b feature/nombre-funcionalidad
   ```
2. Realice sus modificaciones respetando los estándares de código.
3. Verifique que la aplicación compile y empaquete limpiamente:
   ```bash
   cd 1-Aplicacion/fainca && mvn clean test
   ```
4. Realice sus commits utilizando el formato de **Commits Convencionales**.
5. Abra un Pull Request (PR) hacia `develop`.

---

## 💬 2. Convención de Mensajes de Commit

Utilizamos el estándar de **Conventional Commits**:

```text
<tipo>(<área/módulo>): <descripción breve en presente imperativo>

[cuerpo opcional con detalles]
```

### Tipos Permitidos:
- **`feat`**: Nueva funcionalidad (ej. `feat(herramientas): agregar filtro de reposición por stock mínimo`).
- **`fix`**: Corrección de un error o bug (ej. `fix(auth): corregir tiempo de expiración de sesión`).
- **`docs`**: Cambios exclusivos en la documentación (ej. `docs(readme): actualizar instrucciones de despliegue en Tomcat`).
- **`refactor`**: Refactorización de código sin alterar comportamiento (ej. `refactor(dao): optimizar consultas en ProductoDAO`).
- **`security`**: Endurecimiento o parches de seguridad (ej. `security(filter): reforzar validación de parámetros multipart`).
- **`chore`**: Tareas de mantenimiento o configuración (ej. `chore(pom): actualizar versión de Apache POI`).

---

## ☕ 3. Estándares de Código Java y JSP

1. **Java 21 LTS**:
   - Aprovechar características modernas de Java cuando mejoren la legibilidad (ej. `var`, *pattern matching*, *text blocks* para SQL legibles).
2. **Seguridad y Acceso a Datos (DAO)**:
   - **Prohibido** concatenar cadenas en sentencias SQL. Utilice siempre `PreparedStatement` con placeholders `?`.
   - Cierre explícito de recursos (`Connection`, `PreparedStatement`, `ResultSet`) mediante bloques `try-with-resources`.
   - Las operaciones que muten más de una tabla deben encapsularse en transacciones atómicas (`setAutoCommit(false)`, `commit()`, `rollback()`).
3. **Controladores (Servlets)**:
   - Mantener los servlets delgados (*thin controllers*): delegar la lógica de base de datos a los DAOs y la lógica de negocio a los servicios/objetos.
   - Retornar siempre tipos MIME correctos (`application/json;charset=UTF-8`, `application/pdf`, etc.).
4. **Vistas JSP y Frontend**:
   - Evitar código Java embebido (*scriptlets* `<% ... %>`) en la medida de lo posible; preferir JSTL y consumo de APIs AJAX vía JavaScript.
   - Utilizar `SweetAlert2` para confirmaciones destructivas y mensajes de retroalimentación al operador.

---

## 🗄 4. Modificaciones a la Base de Datos

Cualquier cambio en la estructura de base de datos debe:
1. Diseñarse de forma no destructiva hacia los datos históricos existentes.
2. Registrarse en un nuevo script SQL de migración en `1-Aplicacion/db/`.
3. Actualizar el volcado maestro `2-Base-de-datos/base-datos-completa.sql`.
4. Documentar los nuevos campos o tablas en [`4-Documentacion/BASE_DE_DATOS.md`](4-Documentacion/BASE_DE_DATOS.md).

---
© FAINCA Group.

