package com.facturas.vista;

import com.facturas.dao.FacturaDAO;
import com.facturas.modelo.Factura;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

class PanelFacturasGeneradas extends JPanel {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final JTextField clienteFiltro = campo(16);
    private final JTextField facturaFiltro = campo(16);
    private final JTextField fechaFiltro = campo(12);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "N factura", "Cliente", "Fecha", "Base", "IVA", "Total", "PDF", "Ruta"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabla = new JTable(tableModel);

    PanelFacturasGeneradas(AppState appState) {
        setLayout(new BorderLayout(16, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        UiTheme.estilizarTabla(tabla);
        tabla.removeColumn(tabla.getColumnModel().getColumn(8));
        tabla.removeColumn(tabla.getColumnModel().getColumn(0));

        add(crearCabecera(), BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (aFlag) {
            cargarHistorial();
        }
    }

    private JPanel crearCabecera() {
        JPanel contenedor = new JPanel(new BorderLayout(4, 14));
        contenedor.setOpaque(false);
        JPanel titulo = new JPanel(new BorderLayout(4, 4));
        titulo.setOpaque(false);
        titulo.add(UiTheme.titulo("Facturas generadas"), BorderLayout.NORTH);
        titulo.add(UiTheme.ayuda("Historial de PDFs creados."), BorderLayout.CENTER);

        JPanel filtros = UiTheme.panelBlanco();
        filtros.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton buscar = UiTheme.botonSecundario("Buscar");
        buscar.addActionListener(event -> cargarHistorial());
        filtros.add(UiTheme.ayuda("Cliente"));
        filtros.add(clienteFiltro);
        filtros.add(UiTheme.ayuda("N factura"));
        filtros.add(facturaFiltro);
        filtros.add(UiTheme.ayuda("Fecha"));
        filtros.add(fechaFiltro);
        filtros.add(buscar);

        contenedor.add(titulo, BorderLayout.NORTH);
        contenedor.add(filtros, BorderLayout.SOUTH);
        return contenedor;
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);
        JButton abrirPdf = UiTheme.botonSecundario("Abrir PDF");
        JButton abrirCarpeta = UiTheme.botonSecundario("Abrir carpeta");
        JButton regenerar = UiTheme.botonSecundario("Regenerar");
        JButton eliminar = UiTheme.botonEliminar("Eliminar registro");
        abrirPdf.addActionListener(event -> abrirPdf());
        abrirCarpeta.addActionListener(event -> abrirCarpeta());
        regenerar.addActionListener(event -> JOptionPane.showMessageDialog(this, "Use la pantalla Generar facturas para regenerar los PDFs."));
        eliminar.addActionListener(event -> eliminarFila());
        botones.add(abrirPdf);
        botones.add(abrirCarpeta);
        botones.add(regenerar);
        botones.add(eliminar);
        return botones;
    }

    private void cargarHistorial() {
        tableModel.setRowCount(0);
        try {
            List<Factura> facturas = facturaDAO.buscar(
                    clienteFiltro.getText(),
                    facturaFiltro.getText(),
                    fechaFiltro.getText()
            );
            for (Factura factura : facturas) {
                agregarFila(factura);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el historial de facturas.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarFila(Factura factura) {
        Path pdf = Path.of(factura.getPdfPath());
        String estado = java.nio.file.Files.exists(pdf) ? "Generado" : "Archivo no encontrado";
        tableModel.addRow(new Object[]{
                factura.getId(),
                factura.getNumero(),
                factura.getCliente() == null ? "" : factura.getCliente().getNombre(),
                factura.getFecha() == null ? "" : factura.getFecha().format(FORMATO_FECHA),
                moneda(factura.getBaseImponible()),
                moneda(factura.getIva()),
                moneda(factura.getTotal()),
                estado,
                pdf.toAbsolutePath().toString()
        });
    }

    private void abrirPdf() {
        Path path = pathSeleccionado();
        if (path == null) {
            return;
        }
        if (!Files.exists(path)) {
            JOptionPane.showMessageDialog(this, "Archivo no encontrado.");
            return;
        }
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el PDF.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirCarpeta() {
        Path path = pathSeleccionado();
        if (path == null) {
            return;
        }
        try {
            Desktop.getDesktop().open(path.getParent().toFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir la carpeta.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarFila() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una factura de la tabla.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "Seguro que quieres eliminar este registro? El PDF no se borrara.",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(fila);
        int id = (int) tableModel.getValueAt(filaModelo, 0);
        try {
            facturaDAO.eliminar(id);
            cargarHistorial();
            JOptionPane.showMessageDialog(this, "Registro eliminado correctamente.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el registro.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path pathSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una factura de la tabla.");
            return null;
        }
        int filaModelo = tabla.convertRowIndexToModel(fila);
        return Path.of(tableModel.getValueAt(filaModelo, 8).toString());
    }

    private String moneda(java.math.BigDecimal cantidad) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(cantidad);
    }

    private static JTextField campo(int columnas) {
        JTextField field = new JTextField(columnas);
        UiTheme.estilizarCampo(field);
        return field;
    }
}
