package com.facturas.vista;

import com.facturas.servicio.FacturaService;
import com.facturas.servicio.GeneracionFacturaResultado;
import com.facturas.modelo.FacturaExcel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

class PanelGenerarFacturas extends JPanel {
    private final AppState appState;
    private final Consumer<String> navegar;
    private final FacturaService facturaService = new FacturaService();
    private final JTextField excelField = campo(30);
    private final JTextField cantidadField = campo(8);
    private final JTextField clientesField = campo(8);
    private final JTextField salidaField = campo(30);
    private final JTextField ivaField = campo(8);
    private final JTextField fechaField = campo(12);
    private final JComboBox<String> formaPagoCombo = new JComboBox<>(new String[]{"Metalico", "Transferencia", "Cargo a cuenta"});
    private final JProgressBar progressBar = new JProgressBar();

    PanelGenerarFacturas(AppState appState, Consumer<String> navegar) {
        this.appState = appState;
        this.navegar = navegar;
        setLayout(new BorderLayout(16, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        excelField.setEditable(false);
        cantidadField.setEditable(false);
        clientesField.setEditable(false);
        salidaField.setEditable(false);
        ivaField.setText("21%");
        fechaField.setText(LocalDate.now().toString());
        progressBar.setStringPainted(true);

        add(crearCabecera(), BorderLayout.NORTH);
        add(crearResumen(), BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (aFlag) {
            refrescarResumen();
        }
    }

    private JPanel crearCabecera() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setOpaque(false);
        panel.add(UiTheme.titulo("Generar facturas"), BorderLayout.NORTH);
        panel.add(UiTheme.ayuda("Revise el resumen final antes de crear los PDFs."), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearResumen() {
        JPanel contenedor = UiTheme.panelBlanco();
        contenedor.setLayout(new GridBagLayout());
        agregarCampo(contenedor, "Archivo", excelField, 0);
        agregarCampo(contenedor, "Facturas a generar", cantidadField, 1);
        agregarCampo(contenedor, "Clientes encontrados", clientesField, 2);
        agregarCampoConBoton(contenedor, "Carpeta de salida", salidaField, UiTheme.botonSecundario("Seleccionar carpeta"), 3);
        agregarCampo(contenedor, "IVA aplicado", ivaField, 4);
        agregarCampo(contenedor, "Fecha de factura", fechaField, 5);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 6;
        labelConstraints.insets = new Insets(8, 8, 8, 10);
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        contenedor.add(new JLabel("Forma de pago"), labelConstraints);

        GridBagConstraints comboConstraints = new GridBagConstraints();
        comboConstraints.gridx = 1;
        comboConstraints.gridy = 6;
        comboConstraints.insets = new Insets(8, 4, 8, 8);
        comboConstraints.fill = GridBagConstraints.HORIZONTAL;
        comboConstraints.weightx = 1;
        contenedor.add(formaPagoCombo, comboConstraints);

        GridBagConstraints notaConstraints = new GridBagConstraints();
        notaConstraints.gridx = 0;
        notaConstraints.gridy = 7;
        notaConstraints.gridwidth = 2;
        notaConstraints.insets = new Insets(12, 8, 8, 8);
        notaConstraints.anchor = GridBagConstraints.LINE_START;
        contenedor.add(UiTheme.ayuda("Se generara una factura por cada fila valida del archivo importado. No se aplican retenciones."), notaConstraints);
        return contenedor;
    }

    private JPanel crearBotones() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        JButton generar = UiTheme.botonPrimario("Generar PDFs");
        JButton vistaPrevia = UiTheme.botonSecundario("Vista previa");
        JButton cancelar = UiTheme.botonSecundario("Cancelar");
        generar.addActionListener(event -> generarPdfs());
        vistaPrevia.addActionListener(event -> mostrarVistaPrevia());
        cancelar.addActionListener(event -> navegar.accept("Importar Excel/CSV"));
        botones.add(vistaPrevia);
        botones.add(cancelar);
        botones.add(generar);

        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(botones, BorderLayout.EAST);
        return panel;
    }

    private void agregarCampo(JPanel panel, String etiqueta, JTextField campo, int fila) {
        agregarCampoConBoton(panel, etiqueta, campo, null, fila);
    }

    private void agregarCampoConBoton(JPanel panel, String etiqueta, JTextField campo, JButton boton, int fila) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = fila;
        labelConstraints.insets = new Insets(8, 8, 8, 10);
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(etiqueta), labelConstraints);

        JPanel fieldPanel = new JPanel(new BorderLayout(8, 0));
        fieldPanel.setOpaque(false);
        fieldPanel.add(campo, BorderLayout.CENTER);
        if (boton != null) {
            boton.addActionListener(event -> seleccionarCarpeta());
            fieldPanel.add(boton, BorderLayout.EAST);
        }

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = fila;
        fieldConstraints.insets = new Insets(8, 4, 8, 8);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        panel.add(fieldPanel, fieldConstraints);
    }

    private void refrescarResumen() {
        excelField.setText(appState.getArchivoExcel() == null ? "" : appState.getArchivoExcel().getName());
        cantidadField.setText(String.valueOf(appState.contarValidas()));
        clientesField.setText(String.valueOf(appState.contarValidas()));
        salidaField.setText(appState.getCarpetaSalida().toAbsolutePath().toString());
        progressBar.setValue(0);
    }

    private void seleccionarCarpeta() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File carpeta = chooser.getSelectedFile();
            appState.setCarpetaSalida(carpeta.toPath());
            salidaField.setText(carpeta.getAbsolutePath());
        }
    }

    private void mostrarVistaPrevia() {
        if (appState.primeraValida().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay facturas validas para previsualizar.");
            return;
        }
        VistaPreviaFacturaDialog dialog = new VistaPreviaFacturaDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                appState.getValidaciones(),
                formaPagoCombo.getSelectedItem().toString()
        );
        dialog.setVisible(true);
    }

    private void generarPdfs() {
        if (appState.getArchivoExcel() == null) {
            JOptionPane.showMessageDialog(this, "Selecciona e importa un archivo Excel/CSV.");
            return;
        }
        if (!appState.puedeGenerar()) {
            JOptionPane.showMessageDialog(this, "No hay facturas validas para generar. Revise la validacion antes de continuar.");
            return;
        }
        LocalDate fechaFactura;
        try {
            fechaFactura = LocalDate.parse(fechaField.getText().trim());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "La fecha de factura debe tener formato AAAA-MM-DD.");
            return;
        }
        List<FacturaExcel> filasValidas = appState.getValidaciones().stream()
                .filter(ValidacionFacturaExcel::isCorrecta)
                .map(ValidacionFacturaExcel::getFila)
                .toList();
        filasValidas.forEach(fila -> fila.setFecha(fechaFactura));

        progressBar.setIndeterminate(true);
        SwingWorker<List<GeneracionFacturaResultado>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<GeneracionFacturaResultado> doInBackground() throws Exception {
                Path salida = appState.getCarpetaSalida();
                return facturaService.generarFacturas(filasValidas, salida, formaPagoCombo.getSelectedItem().toString());
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                try {
                    List<GeneracionFacturaResultado> resultados = get();
                    appState.setResultados(resultados);
                    JOptionPane.showMessageDialog(PanelGenerarFacturas.this, "Facturas generadas correctamente.");
                    navegar.accept("Facturas generadas");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PanelGenerarFacturas.this, "No se pudo generar el PDF de la factura seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private static JTextField campo(int columnas) {
        JTextField field = new JTextField(columnas);
        UiTheme.estilizarCampo(field);
        return field;
    }
}
