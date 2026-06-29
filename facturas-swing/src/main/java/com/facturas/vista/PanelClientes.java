package com.facturas.vista;

import com.facturas.dao.ClienteDAO;
import com.facturas.modelo.Cliente;
import com.facturas.servicio.ExcelService;
import com.facturas.servicio.ImportacionClientesResultado;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class PanelClientes extends JPanel {
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ExcelService excelService = new ExcelService();
    private final JTextField busquedaField = new JTextField(32);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Nombre / razon social", "NIF", "Direccion", "Codigo postal", "Localidad", "Telefono"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaClientes = new JTable(tableModel);
    private List<Cliente> clientes = List.of();

    public PanelClientes() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        UiTheme.estilizarCampo(busquedaField);

        add(crearCabecera(), BorderLayout.NORTH);
        add(crearTabla(), BorderLayout.CENTER);
        add(crearAccionesSeleccion(), BorderLayout.SOUTH);
        cargarClientes();
    }

    private JPanel crearCabecera() {
        JPanel contenedor = new JPanel(new BorderLayout(4, 14));
        contenedor.setOpaque(false);

        JPanel titulo = new JPanel(new BorderLayout(4, 4));
        titulo.setOpaque(false);
        titulo.add(UiTheme.titulo("Clientes"), BorderLayout.NORTH);
        titulo.add(UiTheme.ayuda("Listado de clientes registrados."), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        acciones.setOpaque(false);
        JButton buscar = UiTheme.botonSecundario("Buscar");
        JButton nuevo = UiTheme.botonPrimario("Nuevo cliente");
        JButton importar = UiTheme.botonSecundario("Importar clientes Excel");
        JButton actualizar = UiTheme.botonSecundario("Actualizar");
        buscar.addActionListener(event -> buscarClientes());
        nuevo.addActionListener(event -> nuevoCliente());
        importar.addActionListener(event -> importarClientesDesdeExcel());
        actualizar.addActionListener(event -> cargarClientes());

        acciones.add(busquedaField);
        acciones.add(buscar);
        acciones.add(nuevo);
        acciones.add(importar);
        acciones.add(actualizar);

        contenedor.add(titulo, BorderLayout.NORTH);
        contenedor.add(acciones, BorderLayout.SOUTH);
        return contenedor;
    }

    private JScrollPane crearTabla() {
        tablaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiTheme.estilizarTabla(tablaClientes);
        tablaClientes.removeColumn(tablaClientes.getColumnModel().getColumn(0));
        return new JScrollPane(tablaClientes);
    }

    private JPanel crearAccionesSeleccion() {
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        acciones.setOpaque(false);
        JButton editar = UiTheme.botonSecundario("Ver / Editar");
        JButton eliminar = UiTheme.botonEliminar("Eliminar");
        JButton duplicar = UiTheme.botonSecundario("Duplicar");
        editar.addActionListener(event -> editarCliente());
        eliminar.addActionListener(event -> eliminarCliente());
        duplicar.addActionListener(event -> duplicarCliente());
        acciones.add(editar);
        acciones.add(eliminar);
        acciones.add(duplicar);
        return acciones;
    }

    private void buscarClientes() {
        try {
            cargarTabla(clienteDAO.buscar(busquedaField.getText().trim()));
        } catch (SQLException e) {
            mostrarError("No se pudo buscar clientes.");
        }
    }

    private void cargarClientes() {
        try {
            cargarTabla(clienteDAO.listar());
        } catch (SQLException e) {
            mostrarError("No se pudo cargar la lista de clientes.");
        }
    }

    private void cargarTabla(List<Cliente> clientes) {
        this.clientes = clientes;
        tableModel.setRowCount(0);
        for (Cliente cliente : clientes) {
            tableModel.addRow(new Object[]{
                    cliente.getId(),
                    valor(cliente.getNombre()),
                    valor(cliente.getNif()),
                    valor(cliente.getDireccion()),
                    valor(cliente.getCodigoPostal()),
                    valor(cliente.getLocalidad()),
                    valor(cliente.getTelefono())
            });
        }
    }

    private void nuevoCliente() {
        DialogoCliente dialogo = new DialogoCliente(frame(), null);
        dialogo.setVisible(true);
        if (!dialogo.isGuardado()) {
            return;
        }

        try {
            clienteDAO.guardar(dialogo.getCliente());
            cargarClientes();
            JOptionPane.showMessageDialog(this, "Cliente guardado correctamente.");
        } catch (SQLException e) {
            mostrarError("No se pudo guardar el cliente. Revise los datos.");
        }
    }

    private void editarCliente() {
        Cliente cliente = clienteSeleccionado();
        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla.");
            return;
        }

        DialogoCliente dialogo = new DialogoCliente(frame(), copiar(cliente));
        dialogo.setVisible(true);
        if (!dialogo.isGuardado()) {
            return;
        }

        try {
            clienteDAO.actualizar(dialogo.getCliente());
            cargarClientes();
            JOptionPane.showMessageDialog(this, "Cliente guardado correctamente.");
        } catch (SQLException e) {
            mostrarError("No se pudo guardar el cliente. Revise los datos.");
        }
    }

    private void eliminarCliente() {
        Cliente cliente = clienteSeleccionado();
        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "Seguro que quieres eliminar este cliente?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            clienteDAO.eliminar(cliente.getId());
            cargarClientes();
            JOptionPane.showMessageDialog(this, "Cliente eliminado correctamente.");
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar el cliente.");
        }
    }

    private void duplicarCliente() {
        Cliente cliente = clienteSeleccionado();
        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla.");
            return;
        }
        Cliente copia = copiar(cliente);
        copia.setId(0);
        copia.setNif("");
        DialogoCliente dialogo = new DialogoCliente(frame(), copia);
        dialogo.setVisible(true);
        if (!dialogo.isGuardado()) {
            return;
        }
        try {
            clienteDAO.guardar(dialogo.getCliente());
            cargarClientes();
            JOptionPane.showMessageDialog(this, "Cliente guardado correctamente.");
        } catch (SQLException e) {
            mostrarError("No se pudo guardar el cliente. Revise los datos.");
        }
    }

    private void importarClientesDesdeExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Excel de clientes (*.ods, *.xlsx, *.xls)", "ods", "xlsx", "xls"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            ImportacionClientesResultado resultado = importarClientes(chooser.getSelectedFile());
            cargarClientes();
            JOptionPane.showMessageDialog(
                    this,
                    "Clientes creados: " + resultado.getCreados()
                            + "\nClientes actualizados: " + resultado.getActualizados()
                            + "\nFilas omitidas: " + resultado.getOmitidos()
            );
        } catch (Exception e) {
            mostrarError("No se pudo importar el Excel de clientes. Revise que tenga las hojas comercios y sociedades.");
        }
    }

    private ImportacionClientesResultado importarClientes(File archivo) throws Exception {
        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        for (Cliente cliente : excelService.leerClientes(archivo)) {
            if (cliente.getNif() == null || cliente.getNif().isBlank()) {
                omitidos++;
                continue;
            }
            if (clienteDAO.guardarOActualizarPorNif(cliente)) {
                creados++;
            } else {
                actualizados++;
            }
        }
        return new ImportacionClientesResultado(creados, actualizados, omitidos);
    }

    private Cliente clienteSeleccionado() {
        int filaVista = tablaClientes.getSelectedRow();
        if (filaVista < 0) {
            return null;
        }
        int filaModelo = tablaClientes.convertRowIndexToModel(filaVista);
        int id = (int) tableModel.getValueAt(filaModelo, 0);
        return clientes.stream().filter(cliente -> cliente.getId() == id).findFirst().orElse(null);
    }

    private Cliente copiar(Cliente origen) {
        Cliente copia = new Cliente();
        copia.setId(origen.getId());
        copia.setNombre(origen.getNombre());
        copia.setRazonSocial(origen.getRazonSocial());
        copia.setNif(origen.getNif());
        copia.setDireccion(origen.getDireccion());
        copia.setCodigoPostal(origen.getCodigoPostal());
        copia.setLocalidad(origen.getLocalidad());
        copia.setTelefono(origen.getTelefono());
        copia.setEmail(origen.getEmail());
        return copia;
    }

    private JFrame frame() {
        return (JFrame) SwingUtilities.getWindowAncestor(this);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
