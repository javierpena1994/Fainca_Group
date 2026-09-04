-- Usuarios iniciales para una instalacion nueva.
-- Contrasena temporal de ambos: CambiarClave123 (cambiarla tras el primer login).
USE fainca_inventario;

INSERT INTO usuarios (nombre, usuario, password_hash, rol) VALUES
('Administrador', 'victor', '$2a$10$nFYX1t3qnVtB3anIPitgJOwRQjvuV8yrgtVBpMkbv9WpW3zPwkzH2', 'admin'),
('Ventas Demo', 'ventas1', '$2a$10$AGnyFlCtkui2pCxvoy/5K.Yg.GqHmMDPVuv.h6J4pep0jmtu8Iw5e', 'ventas')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
