package com.facturas.vista;

import com.facturas.servicio.FacturaService;
import com.facturas.servicio.GeneracionFacturaResultado;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class PanelFacturas extends JPanel {
    private final FacturaService facturaService = new FacturaService();
    private final JTextField excelSeleccionadoField = new JTextField(42);
    private final JButton seleccionarExcelButton = new JButton("Seleccionar archivo");
    private final JButton generarFacturasButton = new JButton("Generar facturas PDF");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Factura", "Cliente", "Estado", "Archivo / mensaje"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaResultados = new JTable(tableModel);
    private File archivoExcel;

    public PanelFacturas() {
        setLayout(new BorderLayout(12, 12));
        excelSeleccionadoField.setEditable(false);

        seleccionarExcelButton.addActionListener(event -> seleccionarExcel());
        generarFacturasButton.addActionListener(event -> generarFacturas());

        add(crearControles(), BorderLayout.NORTH);
        add(new JScrollPane(tablaResultados), BorderLayout.CENTER);
    }

    private JPanel crearControles() {
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controles.add(new JLabel("Archivo"));
        controles.add(excelSeleccionadoField);
        controles.add(seleccionarExcelButton);
        controles.add(generarFacturasButton);
        return controles;
    }

    private void seleccionarExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Hojas de calculo o CSV (*.ods, *.xlsx, *.xls, *.csv)", "ods", "xlsx", "xls", "csv"));
        int resultado = fileChooser.showOpenDialog(this);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            archivoExcel = fileChooser.getSelectedFile();
            excelSeleccionadoField.setText(archivoExcel.getAbsolutePath());
        }
    }

    private void generarFacturas() {
        if (archivoExcel == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo.");
            return;
        }

        cambiarEstadoBotones(false);
        tableModel.setRowCount(0);

        SwingWorker<List<GeneracionFacturaResultado>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<GeneracionFacturaResultado> doInBackground() throws Exception {
                return facturaService.generarFacturasDesdeExcel(archivoExcel, Path.of("facturas_generadas"));
            }

            @Override
            protected void done() {
                try {
                    for (GeneracionFacturaResultado resultado : get()) {
                        tableModel.addRow(new Object[]{
                                resultado.getNumeroFactura(),
                                resultado.getReferenciaCliente(),
                                resultado.isCorrecta() ? "OK" : "Error",
                                resultado.isCorrecta() ? resultado.getArchivoPdf().toString() : resultado.getMensaje()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PanelFacturas.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    cambiarEstadoBotones(true);
                }
            }
        };

        worker.execute();
    }

    private void cambiarEstadoBotones(boolean activo) {
        seleccionarExcelButton.setEnabled(activo);
        generarFacturasButton.setEnabled(activo);
    }
}
