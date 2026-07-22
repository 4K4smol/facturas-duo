package com.facturas.dao;

import com.facturas.conexion.ConexionBD;
import com.facturas.modelo.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ClienteDAO {
    public void guardar(Cliente cliente) throws SQLException {
        String sql = """
                INSERT INTO clientes (nombre, razon_social, nif, direccion, codigo_postal, localidad, telefono, email)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            rellenarStatement(statement, cliente);
            statement.executeUpdate();

            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (claves.next()) {
                    cliente.setId(claves.getInt(1));
                }
            }
        }
    }

    public void actualizar(Cliente cliente) throws SQLException {
        String sql = """
                UPDATE clientes
                SET nombre = ?, razon_social = ?, nif = ?, direccion = ?, codigo_postal = ?, localidad = ?, telefono = ?, email = ?
                WHERE id = ?
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
            PreparedStatement statement = conexion.prepareStatement(sql)) {
            rellenarStatement(statement, cliente);
            statement.setInt(9, cliente.getId());
            statement.executeUpdate();
        }
    }

    public boolean guardarOActualizarPorNif(Cliente cliente) throws SQLException {
        String nif = normalizarNif(cliente.getNif());
        if (nif.isBlank()) {
            return false;
        }

        cliente.setNif(nif);
        Optional<Cliente> existente = buscarPorNif(nif);
        if (existente.isEmpty()) {
            guardar(cliente);
            return true;
        }

        cliente.setId(existente.get().getId());
        actualizar(cliente);
        return false;
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM clientes WHERE id = ?";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<Cliente> listar() throws SQLException {
        String sql = """
                SELECT id, nombre, razon_social, nif, direccion, codigo_postal, localidad, telefono, email
                FROM clientes
                ORDER BY nombre
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Cliente> clientes = new ArrayList<>();
            while (resultSet.next()) {
                clientes.add(mapearCliente(resultSet));
            }
            return clientes;
        }
    }

    public Optional<Cliente> buscarPorNif(String nif) throws SQLException {
        String sql = """
                SELECT id, nombre, razon_social, nif, direccion, codigo_postal, localidad, telefono, email
                FROM clientes
                WHERE UPPER(REPLACE(REPLACE(nif, '-', ''), ' ', '')) = ?
                """;

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, normalizarNif(nif));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearCliente(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public List<Cliente> buscarPorNombreFiscal(String nombreFiscal) throws SQLException {
        if (nombreFiscal == null || nombreFiscal.isBlank()) {
            return List.of();
        }

        String normalizado = normalizar(nombreFiscal);
        List<Cliente> coincidencias = new ArrayList<>();
        for (Cliente cliente : listar()) {
            if (coincide(normalizado, cliente.getNombre()) || coincide(normalizado, cliente.getRazonSocial())) {
                coincidencias.add(cliente);
            }
        }
        return coincidencias;
    }

    public int contar() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    public List<Cliente> buscar(String filtro) throws SQLException {
        if (filtro == null || filtro.isBlank()) {
            return listar();
        }

        String sql = """
                SELECT id, nombre, razon_social, nif, direccion, codigo_postal, localidad, telefono, email
                FROM clientes
                WHERE LOWER(nombre) LIKE ?
                   OR LOWER(COALESCE(razon_social, '')) LIKE ?
                   OR LOWER(nif) LIKE ?
                   OR LOWER(COALESCE(localidad, '')) LIKE ?
                ORDER BY nombre
                """;
        String patron = "%" + filtro.toLowerCase() + "%";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                statement.setString(i, patron);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Cliente> clientes = new ArrayList<>();
                while (resultSet.next()) {
                    clientes.add(mapearCliente(resultSet));
                }
                return clientes;
            }
        }
    }

    private void rellenarStatement(PreparedStatement statement, Cliente cliente) throws SQLException {
        statement.setString(1, cliente.getNombre());
        statement.setString(2, cliente.getRazonSocial());
        statement.setString(3, cliente.getNif());
        statement.setString(4, cliente.getDireccion());
        statement.setString(5, cliente.getCodigoPostal());
        statement.setString(6, cliente.getLocalidad());
        statement.setString(7, cliente.getTelefono());
        statement.setString(8, cliente.getEmail());
    }

    private Cliente mapearCliente(ResultSet resultSet) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(resultSet.getInt("id"));
        cliente.setNombre(resultSet.getString("nombre"));
        cliente.setRazonSocial(resultSet.getString("razon_social"));
        cliente.setNif(resultSet.getString("nif"));
        cliente.setDireccion(resultSet.getString("direccion"));
        cliente.setCodigoPostal(resultSet.getString("codigo_postal"));
        cliente.setLocalidad(resultSet.getString("localidad"));
        cliente.setTelefono(resultSet.getString("telefono"));
        cliente.setEmail(resultSet.getString("email"));
        return cliente;
    }

    boolean coincide(String buscado, String candidato) {
        String normalizado = normalizar(candidato);
        if (buscado.isBlank() || normalizado.isBlank()) {
            return false;
        }
        return normalizado.equals(buscado);
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        String sinAcentos = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = sinAcentos.replaceAll("[^a-z0-9]+", " ").trim();
        StringBuilder resultado = new StringBuilder();
        for (String palabra : limpio.split("\\s+")) {
            if (palabra.isBlank() || esPalabraIgnorada(palabra)) {
                continue;
            }
            if (resultado.length() > 0) {
                resultado.append(' ');
            }
            resultado.append(palabra);
        }
        return resultado.toString();
    }

    private boolean esPalabraIgnorada(String palabra) {
        return palabra.equals("el")
                || palabra.equals("la")
                || palabra.equals("los")
                || palabra.equals("las")
                || palabra.equals("de")
                || palabra.equals("del");
    }

    private String normalizarNif(String nif) {
        return nif == null ? "" : nif.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
