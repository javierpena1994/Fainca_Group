# HTTPS — configuración lista pero DESACTIVADA

> **Dirigido al Departamento de TI.**
> Todo lo necesario para cifrar el tráfico está hecho y probado. Se dejó apagado
> por una decisión operativa, no porque falle. Reactivarlo son 2 minutos.

## Por qué está desactivado

Se implementó y se verificó funcionando: HTTPS en el puerto 3000, con detección
automática de protocolo (quien escribiera `http://` era redirigido solo a `https://`,
sin cambiar de puerto ni de dirección).

Se desactivó porque el certificado es **autofirmado**: mientras no se instale en cada
dispositivo, los navegadores muestran **"No seguro"** junto a la dirección. En una
bodega, ese aviso genera desconfianza en los usuarios, y la alternativa —instalar el
certificado a mano en cada PC y cada celular— no escala.

**El cifrado sí funcionaba.** El aviso no significaba que la conexión fuera insegura,
sino que el certificado no lo emitió una autoridad reconocida.

## Qué queda expuesto mientras siga en HTTP

Conviene tenerlo claro para dimensionar la prioridad:

- Las **contraseñas** viajan en texto claro por la red al iniciar sesión.
- La **cookie de sesión** (`JSESSIONID`) viaja en claro: quien la capture puede
  suplantar a ese usuario sin conocer su contraseña.
- Cualquier persona conectada a la misma red Wi-Fi puede capturar ambas cosas con
  herramientas de uso común.

El riesgo es proporcional a quién tenga acceso a la red interna.

## Cómo reactivarlo (2 pasos)

### 1. `pom.xml`

Comentar el conector HTTP y descomentar el bloque `<jettyXmls>`:

```xml
<!-- <httpConnector><port>${puerto}</port></httpConnector> -->

<jettyXmls>
    <jettyXml>${project.basedir}/config/jetty-ssl.xml</jettyXml>
</jettyXmls>
```

### 2. `Filtros/AuthFilter.java`

Volver a colocar, como **primera** instrucción de `doFilter`, la redirección a HTTPS
(está indicada con un comentario en el lugar exacto donde iba):

```java
private static final String PUERTO_HTTPS = System.getProperty("fainca.https.port", "3000");

// ...al inicio de doFilter:
if (!req.isSecure()) {
    String destino = "https://" + req.getServerName() + ":" + PUERTO_HTTPS
            + req.getRequestURI()
            + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
    if ("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod())) {
        res.sendRedirect(destino);
    } else {
        res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);  // 307 conserva el POST
        res.setHeader("Location", destino);
    }
    return;
}
```

Reiniciar el servidor. En el registro debe aparecer un **único** conector:

```
Started ServerConnector{[SSL], ([ssl], http/1.1)}{0.0.0.0:3000}
```

## Archivos que ya existen

| Archivo | Contenido |
|---|---|
| `config/jetty-ssl.xml` | Conector con detección de protocolo (probado) |
| `config/fainca-ssl.p12` | Almacén de claves. Contraseña: `faincaSSL2026` |
| `config/fainca-certificado.cer` | Certificado público, para instalar en los equipos |

El certificado actual está emitido para **IP 192.168.10.65**, `127.0.0.1`, `localhost` y
el nombre del equipo, y vence en **agosto de 2036**.

## La solución recomendada para producción

En lugar de reutilizar este certificado autofirmado, lo apropiado al pasar al servidor de
la empresa es emitir uno desde la **autoridad certificadora interna** (si hay Active
Directory Certificate Services, ya existe). Ventaja decisiva: todos los equipos del
dominio confían automáticamente, **sin instalar nada en cada dispositivo**, y desaparece
el aviso de "No seguro" que motivó desactivar esto.

Si el servidor tendrá otra IP o un nombre DNS, el certificado debe emitirse con esos
datos: uno emitido para `192.168.10.65` será rechazado si se accede por otra dirección.

### Regenerar el certificado autofirmado (si hiciera falta)

```
keytool -genkeypair -alias fainca -keyalg RSA -keysize 2048 -validity 3650 ^
  -keystore config/fainca-ssl.p12 -storetype PKCS12 ^
  -storepass faincaSSL2026 -keypass faincaSSL2026 ^
  -dname "CN=Inventario FAINCA, OU=Departamento de TI, O=FAINCA Group, L=Guayaquil, C=EC" ^
  -ext "SAN=IP:LA-IP-DEL-SERVIDOR,DNS:el-nombre-dns,DNS:localhost"
```
