package com.facturas.vista;

import com.facturas.modelo.Cliente;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

class DialogoCliente extends JDialog {
    private final JTextField nombreField = campo(28);
    private final JTextField nifField = campo(18);
    private final JTextField direccionField = campo(28);
    private final JTextField codigoPostalField = campo(10);
    private final JTextField localidadField = campo(20);
    private final JTextField telefonoField = campo(16);
    private final JTextField emailField = campo(24);
    private Cliente cliente;
    private boolean guardado;

    DialogoCliente(Frame owner, Cliente cliente) {
        super(owner, cliente == null ? "Nuevo cliente" : "Editar cliente", true);
        this.cliente = cliente == null ? new Cliente() : cliente;
        setSize(520, 430);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(14, 14));
        getContentPane().setBackground(UiTheme.BACKGROUND);

        add(crearFormulario(), BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);
        cargarCliente();
    }

    boolean isGuardado() {
        return guardado;
    }

    Cliente getCliente() {
        return cliente;
    }

    private JPanel crearFormulario() {
        JPanel panel = UiTheme.panelBlanco();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(16, 16, 0, 16),
                UiTheme.bordePanel()
        ));

        agregarCampo(panel, "Nombre / razon social *", nombreField, 0);
        agregarCampo(panel, "NIF *", nifField, 1);
        agregarCampo(panel, "Direccion *", direccionField, 2);
        agregarCampo(panel, "Codigo postal *", codigoPostalField, 3);
        agregarCampo(panel, "Localidad *", localidadField, 4);
        agregarCampo(panel, "Telefono", telefonoField, 5);
        agregarCampo(panel, "Email", emailField, 6);
        return panel;
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        botones.setBackground(UiTheme.BACKGROUND);
        JButton guardar = UiTheme.botonPrimario("Guardar");
        JButton cancelar = UiTheme.botonSecundario("Cancelar");
        guardar.addActionListener(event -> guardar());
        cancelar.addActionListener(event -> dispose());
        botones.add(guardar);
        botones.add(cancelar);
        return botones;
    }

    private void agregarCampo(JPanel panel, String etiqueta, JTextField campo, int fila) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = fila;
        labelConstraints.insets = new Insets(8, 8, 8, 10);
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(etiqueta), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = fila;
        fieldConstraints.insets = new Insets(8, 4, 8, 8);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        panel.add(campo, fieldConstraints);
    }

    private void cargarCliente() {
        nombreField.setText(valor(cliente.getNombre()));
        nifField.setText(valor(cliente.getNif()));
        direccionField.setText(valor(cliente.getDireccion()));
        codigoPostalField.setText(valor(cliente.getCodigoPostal()));
        localidadField.setText(valor(cliente.getLocalidad()));
        telefonoField.setText(valor(cliente.getTelefono()));
        emailField.setText(valor(cliente.getEmail()));
    }

    private void guardar() {
        String error = validar();
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cliente.setNombre(nombreField.getText().trim());
        cliente.setRazonSocial(nombreField.getText().trim());
        cliente.setNif(nifField.getText().trim());
        cliente.setDireccion(direccionField.getText().trim());
        cliente.setCodigoPostal(codigoPostalField.getText().trim());
        cliente.setLocalidad(localidadField.getText().trim());
        cliente.setTelefono(telefonoField.getText().trim());
        cliente.setEmail(emailField.getText().trim());
        guardado = true;
        dispose();
    }

    private String validar() {
        if (nombreField.getText().trim().isEmpty()) {
            return "El campo Nombre / razon social es obligatorio.";
        }
        if (nifField.getText().trim().isEmpty()) {
            return "El campo NIF es obligatorio.";
        }
        if (direccionField.getText().trim().isEmpty()) {
            return "El campo Direccion es obligatorio.";
        }
        if (codigoPostalField.getText().trim().isEmpty()) {
            return "El campo Codigo postal es obligatorio.";
        }
        if (localidadField.getText().trim().isEmpty()) {
            return "El campo Localidad es obligatorio.";
        }
        return null;
    }

    private static JTextField campo(int columnas) {
        JTextField field = new JTextField(columnas);
        UiTheme.estilizarCampo(field);
        return field;
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
