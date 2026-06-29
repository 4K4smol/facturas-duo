package com.facturas.vista;

import com.facturas.dao.EmpresaDAO;
import com.facturas.modelo.Empresa;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.sql.SQLException;

public class PanelEmpresa extends JPanel {
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final JTextField nombreField = campo(30);
    private final JTextField nifField = campo(18);
    private final JTextField direccionField = campo(30);
    private final JTextField codigoPostalField = campo(10);
    private final JTextField localidadField = campo(22);
    private final JTextField telefonoField = campo(18);
    private final JTextField emailField = campo(28);
    private final JLabel logoPreview = new JLabel("Sin logo", JLabel.CENTER);
    private int empresaId;
    private String logoPath;

    public PanelEmpresa() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        add(crearCabecera(), BorderLayout.NORTH);
        add(new JScrollPane(crearFormulario()), BorderLayout.CENTER);
        cargarEmpresa();
    }

    private JPanel crearCabecera() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setOpaque(false);
        panel.add(UiTheme.titulo("Datos de mi empresa"), BorderLayout.NORTH);
        panel.add(UiTheme.ayuda("Datos fiscales de la empresa o autonomo que emite las facturas."), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearFormulario() {
        JPanel contenedor = UiTheme.panelBlanco();
        contenedor.setLayout(new BorderLayout(16, 16));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        agregarCampo(formulario, "Nombre / razon social", nombreField, 0);
        agregarCampo(formulario, "DNI / NIF / CIF", nifField, 1);
        agregarCampo(formulario, "Direccion", direccionField, 2);
        agregarCampo(formulario, "Codigo postal", codigoPostalField, 3);
        agregarCampo(formulario, "Localidad", localidadField, 4);
        agregarCampo(formulario, "Telefono", telefonoField, 5);
        agregarCampo(formulario, "Email", emailField, 6);

        JPanel logoPanel = crearPanelLogo();
        GridBagConstraints logoConstraints = new GridBagConstraints();
        logoConstraints.gridx = 0;
        logoConstraints.gridy = 7;
        logoConstraints.gridwidth = 2;
        logoConstraints.insets = new Insets(14, 8, 8, 8);
        logoConstraints.fill = GridBagConstraints.HORIZONTAL;
        logoConstraints.weightx = 1;
        formulario.add(logoPanel, logoConstraints);

        contenedor.add(formulario, BorderLayout.NORTH);
        contenedor.add(crearBotones(), BorderLayout.SOUTH);
        return contenedor;
    }

    private JPanel crearPanelLogo() {
        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.BORDER), "Logo"));

        logoPreview.setPreferredSize(new java.awt.Dimension(180, 110));
        logoPreview.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        logoPreview.setOpaque(true);
        logoPreview.setBackground(java.awt.Color.WHITE);

        JButton seleccionar = UiTheme.botonSecundario("Seleccionar logo");
        JButton eliminar = UiTheme.botonSecundario("Eliminar logo");
        seleccionar.addActionListener(event -> seleccionarLogo());
        eliminar.addActionListener(event -> {
            logoPath = null;
            actualizarLogo();
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.setOpaque(false);
        botones.add(seleccionar);
        botones.add(eliminar);

        panel.add(logoPreview, BorderLayout.WEST);
        panel.add(botones, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);
        JButton guardar = UiTheme.botonPrimario("Guardar cambios");
        JButton cancelar = UiTheme.botonSecundario("Cancelar");
        JButton limpiar = UiTheme.botonSecundario("Limpiar formulario");

        guardar.addActionListener(event -> guardarEmpresa());
        cancelar.addActionListener(event -> cargarEmpresa());
        limpiar.addActionListener(event -> limpiarFormulario());

        botones.add(guardar);
        botones.add(cancelar);
        botones.add(limpiar);
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

    private void seleccionarLogo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Imagenes (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            logoPath = chooser.getSelectedFile().getAbsolutePath();
            actualizarLogo();
        }
    }

    private void cargarEmpresa() {
        try {
            limpiarFormulario();
            empresaDAO.obtenerPrincipal().ifPresent(empresa -> {
                empresaId = empresa.getId();
                nombreField.setText(valor(empresa.getNombre()));
                nifField.setText(valor(empresa.getNif()));
                direccionField.setText(valor(empresa.getDireccion()));
                codigoPostalField.setText(valor(empresa.getCodigoPostal()));
                localidadField.setText(valor(empresa.getLocalidad()));
                telefonoField.setText(valor(empresa.getTelefono()));
                emailField.setText(valor(empresa.getEmail()));
                logoPath = empresa.getLogoPath();
                actualizarLogo();
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los datos de empresa.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarEmpresa() {
        try {
            Empresa empresa = new Empresa();
            empresa.setId(empresaId);
            empresa.setNombre(nombreField.getText().trim());
            empresa.setNif(nifField.getText().trim());
            empresa.setDireccion(direccionField.getText().trim());
            empresa.setCodigoPostal(codigoPostalField.getText().trim());
            empresa.setLocalidad(localidadField.getText().trim());
            empresa.setTelefono(telefonoField.getText().trim());
            empresa.setEmail(emailField.getText().trim());
            empresa.setLogoPath(logoPath);

            empresaDAO.guardarOActualizar(empresa);
            empresaId = empresa.getId();
            JOptionPane.showMessageDialog(this, "Datos de empresa guardados correctamente.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron guardar los datos de empresa.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        empresaId = 0;
        nombreField.setText("");
        nifField.setText("");
        direccionField.setText("");
        codigoPostalField.setText("");
        localidadField.setText("");
        telefonoField.setText("");
        emailField.setText("");
        logoPath = null;
        actualizarLogo();
    }

    private void actualizarLogo() {
        if (logoPath == null || logoPath.isBlank() || !new File(logoPath).isFile()) {
            logoPreview.setIcon(null);
            logoPreview.setText("Sin logo");
            return;
        }

        ImageIcon icon = new ImageIcon(logoPath);
        Image image = icon.getImage().getScaledInstance(160, 90, Image.SCALE_SMOOTH);
        logoPreview.setText("");
        logoPreview.setIcon(new ImageIcon(image));
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
