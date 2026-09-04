-- Base de datos de inventario FAINCA - Bodega #1
-- Ejecutar con: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS fainca_inventario CHARACTER SET utf8mb4;
USE fainca_inventario;

CREATE TABLE marcas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol ENUM('admin', 'ventas') NOT NULL DEFAULT 'ventas',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- El codigo es la llave unica del producto: la escriben en el
-- empaque/etiqueta fisica y es la que referencian movimientos.
CREATE TABLE productos (
    codigo VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    marca_id INT NOT NULL,
    descripcion TEXT,
    unidad_medida VARCHAR(20) NOT NULL DEFAULT 'unidad',
    stock_actual INT NOT NULL DEFAULT 0,
    ubicacion VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (marca_id) REFERENCES marcas(id),
    INDEX idx_nombre (nombre)
);

-- Historial de ingresos/egresos. stock_actual en productos SIEMPRE
-- se actualiza junto con un movimiento (ver backend), nunca a mano,
-- para no perder el rastro de quien cambio que y cuando.
CREATE TABLE movimientos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    producto_codigo VARCHAR(50) NOT NULL,
    -- 'edicion'    = cambio de datos/foto del producto (no mueve stock)
    -- 'correccion' = aclaracion sobre la observacion de un movimiento ya guardado
    --                (no altera el original; se marca aparte para el auditor)
    tipo ENUM('ingreso', 'egreso', 'ajuste', 'edicion', 'correccion') NOT NULL,
    cantidad INT NOT NULL,
    stock_resultante INT NOT NULL,
    usuario_id INT NOT NULL,
    observaciones VARCHAR(1000),
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (producto_codigo) REFERENCES productos(codigo),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_producto_fecha (producto_codigo, fecha)
);
