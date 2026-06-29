package com.facturas.vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

class PanelConfiguracion extends JPanel {
    private final AppState appState;
    private final JTextField carpetaField = campo(34);
    private final JTextField ivaField = campo(8);
    private final JTextField formatoField = campo(18);
    private final JComboBox<String> plantillaCombo = new JComboBox<>(new String[]{"Clasica", "Compacta", "Formal"});
    private final JCheckBox abrirPdfCheck = new JCheckBox("Abrir PDF al generar");

    PanelConfiguracion(AppState appState) {
        this.appState = appState;
        setLayout(new BorderLayout(16, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        carpetaField.setEditable(false);
        carpetaField.setText(appState.getCarpetaSalida().toAbsolutePath().toString());
        ivaField.setText("21");
        formatoField.setText("FAC-{ANO}-{NUMERO}");
        abrirPdfCheck.setOpaque(false);

        add(crearCabecera(), BorderLayout.NORTH);
        add(crearFormulario(), BorderLayout.CENTER);
    }

    private JPanel crearCabecera() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setOpaque(false);
        panel.add(UiTheme.titulo("Configuracion"), BorderLayout.NORTH);
        panel.add(UiTheme.ayuda("Ajustes generales de la aplicacion."), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearFormulario() {
        JPanel contenedor = UiTheme.panelBlanco();
        contenedor.setLayout(new BorderLayout(0, 18));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        agregarCampoConBoton(formulario, "Carpeta por defecto de facturas", carpetaField, UiTheme.botonSecundario("Seleccionar carpeta"), 0);
        agregarCampo(formulario, "IVA por defecto", ivaField, 1);
        agregarCampo(formulario, "Formato numero factura", formatoField, 2);

        GridBagConstraints plantillaLabel = etiquetaConstraints(3);
        formulario.add(new JLabel("Plantilla visual"), plantillaLabel);
        GridBagConstraints plantillaField = campoConstraints(3);
        formulario.add(plantillaCombo, plantillaField);

        GridBagConstraints checkConstraints = campoConstraints(4);
        formulario.add(abrirPdfCheck, checkConstraints);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);
        JButton guardar = UiTheme.botonPrimario("Guardar configuracion");
        guardar.addActionListener(event -> guardarConfiguracion());
        botones.add(guardar);

        contenedor.add(formulario, BorderLayout.NORTH);
        contenedor.add(botones, BorderLayout.SOUTH);
        return contenedor;
    }

    private void agregarCampo(JPanel panel, String etiqueta, JTextField campo, int fila) {
        agregarCampoConBoton(panel, etiqueta, campo, null, fila);
    }

    private void agregarCampoConBoton(JPanel panel, String etiqueta, JTextField campo, JButton boton, int fila) {
        panel.add(new JLabel(etiqueta), etiquetaConstraints(fila));
        JPanel fieldPanel = new JPanel(new BorderLayout(8, 0));
        fieldPanel.setOpaque(false);
        fieldPanel.add(campo, BorderLayout.CENTER);
        if (boton != null) {
            boton.addActionListener(event -> seleccionarCarpeta());
            fieldPanel.add(boton, BorderLayout.EAST);
        }
        panel.add(fieldPanel, campoConstraints(fila));
    }

    private void seleccionarCarpeta() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            appState.setCarpetaSalida(chooser.getSelectedFile().toPath());
            carpetaField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void guardarConfiguracion() {
        appState.setCarpetaSalida(java.nio.file.Path.of(carpetaField.getText()));
        JOptionPane.showMessageDialog(this, "Configuracion guardada correctamente.");
    }

    private GridBagConstraints etiquetaConstraints(int fila) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = fila;
        constraints.insets = new Insets(8, 8, 8, 10);
        constraints.anchor = GridBagConstraints.LINE_END;
        return constraints;
    }

    private GridBagConstraints campoConstraints(int fila) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = fila;
        constraints.insets = new Insets(8, 4, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        return constraints;
    }

    private static JTextField campo(int columnas) {
        JTextField field = new JTextField(columnas);
        UiTheme.estilizarCampo(field);
        return field;
    }
}
