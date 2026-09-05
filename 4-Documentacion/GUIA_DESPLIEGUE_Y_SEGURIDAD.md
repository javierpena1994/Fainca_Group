# Guía de Despliegue, Infraestructura y Seguridad — FAINCA Group

Este documento está dirigido al **Departamento de TI y Administradores de Sistemas**. Describe los requisitos de infraestructura, procedimientos de despliegue en producción, endurecimiento de seguridad y políticas de respaldo para el **Sistema de Inventario FAINCA**.

---

## 🖥 1. Requisitos de Infraestructura en Producción

### Hardware Recomendado
- **Procesador**: 2 núcleos CPU (x86_64 / ARM64).
- **Memoria RAM**: Mínimo 2 GB RAM (Recomendado 4 GB para JVM + MySQL).
- **Almacenamiento**: 20 GB SSD (considerando base de datos y ~1500+ fotografías de productos en alta resolución).
- **Red**: Interfaz de red cableada con IP estática fija o nombre DNS interno en la red empresarial.

### Software Base
- **Sistema Operativo**: Linux (Ubuntu Server 22.04 / 24.04 LTS, Debian 12, RHEL 9) o Windows Server 2019/2022.
- **Java Runtime**: **OpenJDK 21 LTS** (Eclipse Temurin 21 o Amazon Corretto 21).
- **Gestor de Base de Datos**: **MySQL Server 8.0+** o **8.4 LTS**.
- **Servidor de Aplicaciones**: **Apache Tomcat 10.1+** o **Eclipse Jetty 11+** *(Nota: Requiere compatibilidad con Servlet 5.0 / `jakarta.servlet`).*
- **Herramienta de Compilación**: Apache Maven 3.9+ (solo en el servidor de compilación/CI-CD).

---

## 🗄 2. Configuración y Seguridad de la Base de Datos

### 2.1 Creación de Usuario Exclusivo con Privilegios Mínimos
En lugar de conectar la aplicación con la cuenta `root`, cree un usuario dedicado para el servicio:

```sql
-- Crear base de datos con codificación UTF-8 Multibyte
CREATE DATABASE IF NOT EXISTS fainca_inventario 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_spanish_ci;

-- Crear usuario exclusivo para la aplicación
CREATE USER 'fainca_app'@'localhost' IDENTIFIED BY 'PasswordSuperSegura2026!';

-- Asignar únicamente los privilegios CRUD necesarios
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE TEMPORARY TABLES, LOCK TABLES 
  ON fainca_inventario.* TO 'fainca_app'@'localhost';

FLUSH PRIVILEGES;
```

### 2.2 Restauración de Estructura y Datos Iniciales
```bash
mysql -u fainca_app -p fainca_inventario < "/ruta/a/2-Base-de-datos/base-datos-completa.sql"
```

---

## 📦 3. Compilación y Generación del Paquete WAR

Desde el entorno de desarrollo o servidor de integración:

```bash
cd 1-Aplicacion/fainca
mvn clean package
```

El archivo compilado se genera en:
📁 **`1-Aplicacion/fainca/target/fainca.war`**

---

## 🚀 4. Métodos de Despliegue en Servidores

### Opción A: Despliegue en Apache Tomcat 10.1+ (Recomendado)
1. Instale Tomcat 10.1:
   ```bash
   sudo apt update && sudo apt install tomcat10 tomcat10-admin -y
   ```
2. Copie el archivo `.war` al directorio `webapps` como `ROOT.war` (para servir en la raíz) o `fainca.war`:
   ```bash
   sudo cp target/fainca.war /var/lib/tomcat10/webapps/ROOT.war
   ```
3. Verifique que Tomcat tenga permisos sobre la carpeta externa de imágenes.

### Opción B: Servicio Autónomo con Jetty y Systemd (Linux)
Si prefiere ejecutar con Jetty embebido como servicio del sistema operativo:

1. Cree el archivo de servicio `/etc/systemd/system/fainca.service`:
   ```ini
   [Unit]
   Description=Sistema de Inventario FAINCA
   After=network.target mysql.service
   Wants=mysql.service

   [Service]
   Type=simple
   User=fainca
   Group=fainca
   WorkingDirectory=/opt/fainca/1-Aplicacion/fainca
   ExecStart=/usr/bin/mvn jetty:run
   Restart=always
   RestartSec=10
   Environment="JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64"
   Environment="puerto=3000"

   [Install]
   WantedBy=multi-user.target
   ```
2. Habilite e inicie el servicio:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now fainca.service
   ```

---

## 🔒 5. Activación de HTTPS / Cifrado TLS

La aplicación ya contiene los archivos y la configuración lista para TLS en `config/`.

### 5.1 Enfoque Recomendado: Proxy Inverso Nginx con Certificado Corporativo
La mejor práctica en infraestructura empresarial es dejar que la aplicación corra en HTTP internamente (puerto local 3000 o Tomcat 8080) y delegar el cifrado SSL a un servidor web **Nginx**:

```nginx
server {
    listen 80;
    server_name inventario.fainca.local;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name inventario.fainca.local;

    ssl_certificate /etc/ssl/certs/fainca_corp.crt;
    ssl_certificate_key /etc/ssl/private/fainca_corp.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    client_max_body_size 25M;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

### 5.2 Activación Directa en Jetty
Si prefiere cifrado directo en Jetty:
1. En `pom.xml`, descomente la sección `<jettyXmls>` y comente el conector HTTP.
2. En `AuthFilter.java`, descomente la redirección HTTPS al inicio de `doFilter()`.
3. Reinicie el servidor (consultar `1-Aplicacion/fainca/config/LEEME-HTTPS.md`).

---

## 💾 6. Política de Respaldos Automatizados (Disaster Recovery)

Configure un trabajo cron diario en el servidor para respaldar tanto la base de datos como el repositorio de imágenes de productos:

### Script de Respaldo (`/opt/scripts/backup_fainca.sh`)
```bash
#!/usr/bin/env bash
set -e

BACKUP_DIR="/var/backups/fainca"
FECHA=$(date +"%Y%m%d_%H%M%S")
mkdir -p "$BACKUP_DIR"

# 1. Respaldo de Base de Datos MySQL
mysqldump -u fainca_app -p'PasswordSuperSegura2026!' \
  --single-transaction \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  fainca_inventario | gzip > "$BACKUP_DIR/db_fainca_$FECHA.sql.gz"

# 2. Respaldo de Fotografías de Productos
tar -czf "$BACKUP_DIR/imagenes_fainca_$FECHA.tar.gz" -C "/ruta/a/3-Imagenes-de-productos" .

# 3. Eliminar respaldos con más de 30 días de antigüedad
find "$BACKUP_DIR" -type f -name "*.gz" -mtime +30 -delete

echo "[$(date)] Respaldo completado exitosamente: $FECHA"
```

### Programación en Cron (`sudo crontab -e`)
```cron
# Ejecutar respaldo automático todos los días a las 23:00 hrs
0 23 * * * /opt/scripts/backup_fainca.sh >> /var/log/fainca_backup.log 2>&1
```

---

## 🛡 7. Lista de Chequeo de Seguridad para Paso a Producción

- [ ] Modificar las contraseñas temporales por defecto de todos los usuarios iniciales (`admin`, `victorm`, `javierp`, `ventas`).
- [ ] Configurar un usuario exclusivo de MySQL (`fainca_app`) con acceso limitado a `fainca_inventario`.
- [ ] Configurar la ruta absoluta de almacenamiento de imágenes en `db.properties` fuera del directorio de despliegue.
- [ ] Habilitar firewall del servidor (ej. `sudo ufw allow 80,443/tcp` y restringir el puerto MySQL 3306 a `127.0.0.1`).
- [ ] Implementar cifrado TLS / HTTPS (vía Nginx o certificado corporativo).
- [ ] Comprobar la ejecución y retención del script de respaldo automático diario.

