CREATE DATABASE IF NOT EXISTS facturas_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE facturas_app;

CREATE TABLE IF NOT EXISTS clientes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    razon_social VARCHAR(160),
    nif VARCHAR(20) NOT NULL UNIQUE,
    direccion VARCHAR(220),
    codigo_postal VARCHAR(12),
    localidad VARCHAR(120),
    telefono VARCHAR(40),
    email VARCHAR(160)
);

CREATE TABLE IF NOT EXISTS empresa (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL,
    nif VARCHAR(20) NOT NULL,
    direccion VARCHAR(220),
    codigo_postal VARCHAR(12),
    localidad VARCHAR(120),
    telefono VARCHAR(40),
    email VARCHAR(160),
    logo_path VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS facturas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(80) NOT NULL,
    fecha DATE NOT NULL,
    cliente_id INT,
    cliente_nombre VARCHAR(160) NOT NULL,
    concepto VARCHAR(255),
    base_imponible DECIMAL(12, 2) NOT NULL,
    iva DECIMAL(12, 2) NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    pdf_path VARCHAR(600) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_facturas_numero (numero),
    INDEX idx_facturas_cliente_nombre (cliente_nombre),
    CONSTRAINT fk_facturas_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        ON DELETE SET NULL
);
