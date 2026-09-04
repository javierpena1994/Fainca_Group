#!/usr/bin/env bash
set -e

# Ubicarse en el directorio del script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "=================================================="
echo "  SERVIDOR DE INVENTARIO FAINCA (macOS)"
echo "=================================================="
echo ""

# 1. Configurar Java 21
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
    elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)"
    fi
fi

if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# 2. Configurar Maven
if ! command -v mvn >/dev/null 2>&1; then
    if [ -f "/Users/javierpena/.m2/wrapper/dists/apache-maven-3.9.16/56ba1f9f/bin/mvn" ]; then
        export PATH="/Users/javierpena/.m2/wrapper/dists/apache-maven-3.9.16/56ba1f9f/bin:$PATH"
    fi
fi

# 3. Comprobar e iniciar Base de Datos Local FAINCA (Puerto 3307)
DB_DIR="$WORKSPACE_ROOT/.db_data"
MY_CNF="$DB_DIR/my.cnf"

if ! nc -z 127.0.0.1 3307 2>/dev/null; then
    echo "[MySQL] Iniciando base de datos local en puerto 3307..."
    
    mkdir -p "$DB_DIR/data"
    cat << EOF > "$MY_CNF"
[mysqld]
basedir=/usr/local/mysql
datadir=$DB_DIR/data
port=3307
bind-address=127.0.0.1
socket=/tmp/fainca_mysql.sock
mysqlx=0
innodb_undo_directory=$DB_DIR/data
innodb_data_home_dir=$DB_DIR/data
innodb_log_group_home_dir=$DB_DIR/data
EOF

    if [ ! -d "$DB_DIR/data/mysql" ]; then
        echo "[MySQL] Inicializando almacenamiento de base de datos..."
        /usr/local/mysql/bin/mysqld --defaults-file="$MY_CNF" --initialize-insecure
        
        cd "$DB_DIR/data"
        /usr/local/mysql/bin/mysqld --defaults-file="$MY_CNF" &
        cd "$SCRIPT_DIR"
        
        for i in {1..30}; do
            if nc -z 127.0.0.1 3307 2>/dev/null; then break; fi
            sleep 0.5
        done
        
        echo "[MySQL] Cargando datos iniciales de base-datos-completa.sql..."
        /usr/local/mysql/bin/mysql -h 127.0.0.1 -P 3307 -u root < "$WORKSPACE_ROOT/2-Base-de-datos/base-datos-completa.sql"
        /usr/local/mysql/bin/mysql -h 127.0.0.1 -P 3307 -u root -e "USE fainca_inventario; UPDATE usuarios SET password_hash = '\$2a\$10\$nFYX1t3qnVtB3anIPitgJOwRQjvuV8yrgtVBpMkbv9WpW3zPwkzH2', activo = 1 WHERE usuario IN ('admin', 'victorm', 'javierp', 'ventas');"
    else
        cd "$DB_DIR/data"
        /usr/local/mysql/bin/mysqld --defaults-file="$MY_CNF" &
        cd "$SCRIPT_DIR"
        for i in {1..30}; do
            if nc -z 127.0.0.1 3307 2>/dev/null; then break; fi
            sleep 0.5
        done
    fi
    echo "[MySQL] Base de datos conectada correctamente (puerto 3307)."
else
    echo "[MySQL] Base de datos activa y lista en puerto 3307."
fi

# 4. Liberar puerto 3000 si quedó algún proceso colgado
if nc -z 127.0.0.1 3000 2>/dev/null; then
    echo "[Puerto 3000] Liberando puerto..."
    lsof -ti :3000 | xargs kill -9 2>/dev/null || true
    sleep 1
fi

echo ""
echo "Java runtime:  $(java -version 2>&1 | head -n 1)"
echo "Maven version: $(mvn -v 2>&1 | head -n 1)"
echo ""
echo "Iniciando servidor web FAINCA..."
echo "Estará listo cuando veas: 'Started ServerConnector ... 0.0.0.0:3000'"
echo ""
echo "Acceso web:       http://localhost:3000/"
echo "Usuario admin:    admin    (o 'victorm' / 'javierp')"
echo "Contraseña:       CambiarClave123"
echo ""
echo "Para detenerlo:   Presiona Ctrl + C"
echo "--------------------------------------------------"
echo ""

exec mvn jetty:run
