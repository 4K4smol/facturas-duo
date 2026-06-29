package com.facturas.dao;

import com.facturas.conexion.ConexionBD;
import com.facturas.modelo.Empresa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class EmpresaDAO {
    public void guardarOActualizar(Empresa empresa) throws SQLException {
        if (empresa.getId() > 0) {
            actualizar(empresa);
        } else {
            guardar(empresa);
        }
    }

    public Optional<Empresa> obtenerPrincipal() throws SQLException {
        String sql = """
                SELECT id, nombre, nif, direccion, codigo_postal, localidad, telefono, email, logo_path
                FROM empresa
                ORDER BY id
                LIMIT 1
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Optional.of(mapearEmpresa(resultSet));
            }
            return Optional.empty();
        }
    }

    private void guardar(Empresa empresa) throws SQLException {
        String sql = """
                INSERT INTO empresa (nombre, nif, direccion, codigo_postal, localidad, telefono, email, logo_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            rellenarStatement(statement, empresa);
            statement.executeUpdate();

            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (claves.next()) {
                    empresa.setId(claves.getInt(1));
                }
            }
        }
    }

    private void actualizar(Empresa empresa) throws SQLException {
        String sql = """
                UPDATE empresa
                SET nombre = ?, nif = ?, direccion = ?, codigo_postal = ?, localidad = ?, telefono = ?, email = ?, logo_path = ?
                WHERE id = ?
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
            PreparedStatement statement = conexion.prepareStatement(sql)) {
            rellenarStatement(statement, empresa);
            statement.setInt(9, empresa.getId());
            statement.executeUpdate();
        }
    }

    private void rellenarStatement(PreparedStatement statement, Empresa empresa) throws SQLException {
        statement.setString(1, empresa.getNombre());
        statement.setString(2, empresa.getNif());
        statement.setString(3, empresa.getDireccion());
        statement.setString(4, empresa.getCodigoPostal());
        statement.setString(5, empresa.getLocalidad());
        statement.setString(6, empresa.getTelefono());
        statement.setString(7, empresa.getEmail());
        statement.setString(8, empresa.getLogoPath());
    }

    private Empresa mapearEmpresa(ResultSet resultSet) throws SQLException {
        Empresa empresa = new Empresa();
        empresa.setId(resultSet.getInt("id"));
        empresa.setNombre(resultSet.getString("nombre"));
        empresa.setNif(resultSet.getString("nif"));
        empresa.setDireccion(resultSet.getString("direccion"));
        empresa.setCodigoPostal(resultSet.getString("codigo_postal"));
        empresa.setLocalidad(resultSet.getString("localidad"));
        empresa.setTelefono(resultSet.getString("telefono"));
        empresa.setEmail(resultSet.getString("email"));
        empresa.setLogoPath(resultSet.getString("logo_path"));
        return empresa;
    }
}
