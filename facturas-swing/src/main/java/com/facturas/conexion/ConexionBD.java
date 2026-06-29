package com.facturas.conexion;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {
    private static final String URL = obtenerConfiguracion(
            "FACTURAS_DB_URL",
            "facturas.db.url",
            "jdbc:mysql://localhost:3306/facturas_app?useSSL=false&serverTimezone=UTC"
    );
    private static final String USUARIO = obtenerConfiguracion("FACTURAS_DB_USER", "facturas.db.user", "root");
    private static final String PASSWORD = obtenerConfiguracion("FACTURAS_DB_PASSWORD", "facturas.db.password", "mysql");
    private static boolean esquemaVerificado;

    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws SQLException {
        Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
        asegurarEsquema(conexion);
        return conexion;
    }

    private static synchronized void asegurarEsquema(Connection conexion) throws SQLException {
        if (esquemaVerificado) {
            return;
        }

        crearTablasSiFaltan(conexion);
        agregarColumnasSiFaltan(conexion);
        esquemaVerificado = true;
    }

    private static void crearTablasSiFaltan(Connection conexion) throws SQLException {
        try (Statement statement = conexion.createStatement()) {
            statement.execute("""
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
                    )
                    """);
            statement.execute("""
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
                    )
                    """);
            statement.execute("""
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
                    )
                    """);
        }
    }

    private static void agregarColumnasSiFaltan(Connection conexion) throws SQLException {
        agregarColumnaSiFalta(conexion, "clientes", "razon_social", "VARCHAR(160) AFTER nombre");
        agregarColumnaSiFalta(conexion, "clientes", "direccion", "VARCHAR(220) AFTER nif");
        agregarColumnaSiFalta(conexion, "clientes", "codigo_postal", "VARCHAR(12) AFTER direccion");
        agregarColumnaSiFalta(conexion, "clientes", "localidad", "VARCHAR(120) AFTER codigo_postal");
        agregarColumnaSiFalta(conexion, "clientes", "telefono", "VARCHAR(40) AFTER localidad");
        agregarColumnaSiFalta(conexion, "clientes", "email", "VARCHAR(160) AFTER telefono");

        agregarColumnaSiFalta(conexion, "empresa", "direccion", "VARCHAR(220) AFTER nif");
        agregarColumnaSiFalta(conexion, "empresa", "codigo_postal", "VARCHAR(12) AFTER direccion");
        agregarColumnaSiFalta(conexion, "empresa", "localidad", "VARCHAR(120) AFTER codigo_postal");
        agregarColumnaSiFalta(conexion, "empresa", "telefono", "VARCHAR(40) AFTER localidad");
        agregarColumnaSiFalta(conexion, "empresa", "email", "VARCHAR(160) AFTER telefono");
        agregarColumnaSiFalta(conexion, "empresa", "logo_path", "VARCHAR(500) AFTER email");

        agregarColumnaSiFalta(conexion, "facturas", "fecha", "DATE NOT NULL AFTER numero");
        agregarColumnaSiFalta(conexion, "facturas", "cliente_id", "INT AFTER fecha");
        agregarColumnaSiFalta(conexion, "facturas", "cliente_nombre", "VARCHAR(160) NOT NULL AFTER cliente_id");
        agregarColumnaSiFalta(conexion, "facturas", "concepto", "VARCHAR(255) AFTER cliente_nombre");
        agregarColumnaSiFalta(conexion, "facturas", "base_imponible", "DECIMAL(12, 2) NOT NULL AFTER concepto");
        agregarColumnaSiFalta(conexion, "facturas", "iva", "DECIMAL(12, 2) NOT NULL AFTER base_imponible");
        agregarColumnaSiFalta(conexion, "facturas", "total", "DECIMAL(12, 2) NOT NULL AFTER iva");
        agregarColumnaSiFalta(conexion, "facturas", "pdf_path", "VARCHAR(600) NOT NULL AFTER total");
        agregarColumnaSiFalta(conexion, "facturas", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER pdf_path");
    }

    private static void agregarColumnaSiFalta(Connection conexion, String tabla, String columna, String definicion) throws SQLException {
        if (existeColumna(conexion, tabla, columna)) {
            return;
        }

        try (Statement statement = conexion.createStatement()) {
            statement.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
        }
    }

    private static boolean existeColumna(Connection conexion, String tabla, String columna) throws SQLException {
        DatabaseMetaData metaData = conexion.getMetaData();
        try (ResultSet columnas = metaData.getColumns(conexion.getCatalog(), null, tabla, columna)) {
            return columnas.next();
        }
    }

    private static String obtenerConfiguracion(String variableEntorno, String propiedadSistema, String valorPorDefecto) {
        String valor = System.getenv(variableEntorno);
        if (valor != null && !valor.isBlank()) {
            return valor;
        }

        return System.getProperty(propiedadSistema, valorPorDefecto);
    }
}
