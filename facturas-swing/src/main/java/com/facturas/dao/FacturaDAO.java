package com.facturas.dao;

import com.facturas.conexion.ConexionBD;
import com.facturas.modelo.Cliente;
import com.facturas.modelo.Factura;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FacturaDAO {
    public void guardar(Factura factura) throws SQLException {
        String sql = """
                INSERT INTO facturas (numero, fecha, cliente_id, cliente_nombre, concepto, base_imponible, iva, total, pdf_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, factura.getNumero());
            statement.setDate(2, Date.valueOf(factura.getFecha()));
            if (factura.getCliente() == null || factura.getCliente().getId() <= 0) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, factura.getCliente().getId());
            }
            statement.setString(4, nombreCliente(factura));
            statement.setString(5, factura.getConcepto());
            statement.setBigDecimal(6, factura.getBaseImponible());
            statement.setBigDecimal(7, factura.getIva());
            statement.setBigDecimal(8, factura.getTotal());
            statement.setString(9, factura.getPdfPath());
            statement.executeUpdate();

            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (claves.next()) {
                    factura.setId(claves.getInt(1));
                }
            }
        }
    }

    public List<Factura> buscar(String clienteFiltro, String numeroFiltro, String fechaFiltro) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT id, numero, fecha, cliente_id, cliente_nombre, concepto, base_imponible, iva, total, pdf_path, created_at
                FROM facturas
                WHERE 1 = 1
                """);
        List<String> parametros = new ArrayList<>();

        if (clienteFiltro != null && !clienteFiltro.isBlank()) {
            sql.append(" AND LOWER(cliente_nombre) LIKE ?");
            parametros.add("%" + clienteFiltro.trim().toLowerCase() + "%");
        }
        if (numeroFiltro != null && !numeroFiltro.isBlank()) {
            sql.append(" AND LOWER(numero) LIKE ?");
            parametros.add("%" + numeroFiltro.trim().toLowerCase() + "%");
        }
        if (fechaFiltro != null && !fechaFiltro.isBlank()) {
            sql.append(" AND (DATE_FORMAT(fecha, '%d/%m/%Y') LIKE ? OR CAST(fecha AS CHAR) LIKE ?)");
            String patron = "%" + fechaFiltro.trim().toLowerCase() + "%";
            parametros.add(patron);
            parametros.add(patron);
        }

        sql.append(" ORDER BY created_at DESC, id DESC");

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                statement.setString(i + 1, parametros.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Factura> facturas = new ArrayList<>();
                while (resultSet.next()) {
                    facturas.add(mapearFactura(resultSet));
                }
                return facturas;
            }
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM facturas WHERE id = ?";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Factura mapearFactura(ResultSet resultSet) throws SQLException {
        Factura factura = new Factura();
        factura.setId(resultSet.getInt("id"));
        factura.setNumero(resultSet.getString("numero"));
        factura.setFecha(resultSet.getDate("fecha").toLocalDate());
        factura.setConcepto(resultSet.getString("concepto"));
        factura.setBaseImponible(resultSet.getBigDecimal("base_imponible"));
        factura.setIva(resultSet.getBigDecimal("iva"));
        factura.setTotal(resultSet.getBigDecimal("total"));
        factura.setPdfPath(resultSet.getString("pdf_path"));

        Cliente cliente = new Cliente();
        cliente.setId(resultSet.getInt("cliente_id"));
        cliente.setNombre(resultSet.getString("cliente_nombre"));
        cliente.setRazonSocial(resultSet.getString("cliente_nombre"));
        factura.setCliente(cliente);

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            factura.setCreatedAt(createdAt.toLocalDateTime());
        }
        return factura;
    }

    private String nombreCliente(Factura factura) {
        if (factura.getCliente() == null) {
            return "";
        }
        String razonSocial = factura.getCliente().getRazonSocial();
        return razonSocial == null || razonSocial.isBlank() ? factura.getCliente().getNombre() : razonSocial;
    }
}
