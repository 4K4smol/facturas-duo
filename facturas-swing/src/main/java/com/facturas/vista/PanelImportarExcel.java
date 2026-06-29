package com.facturas.vista;

import com.facturas.dao.ClienteDAO;
import com.facturas.dao.EmpresaDAO;
import com.facturas.modelo.Cliente;
import com.facturas.modelo.Empresa;
import com.facturas.modelo.FacturaExcel;
import com.facturas.servicio.ExcelService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

class PanelImportarExcel extends JPanel {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final AppState appState;
    private final Consumer<String> navegar;
    private final ExcelService excelService = new ExcelService();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final JTextField archivoField = new JTextField(42);
    private final JLabel totalFilasLabel = UiTheme.ayuda("Total de filas leidas: 0");
    private final JLabel validasLabel = UiTheme.ayuda("Facturas validas: 0");
    private final JLabel erroresLabel = UiTheme.ayuda("Facturas con errores: 0");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"N factura", "Razon social", "Concepto", "Total con IVA", "Base", "IVA", "Fecha", "Estado"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabla = new JTable(tableModel);

    PanelImportarExcel(AppState appState, Consumer<String> navegar) {
        this.appState = appState;
        this.navegar = navegar;
        setLayout(new BorderLayout(16, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        archivoField.setEditable(false);
        UiTheme.estilizarCampo(archivoField);
        UiTheme.estilizarTabla(tabla);
        tabla.setDefaultRenderer(Object.class, new EstadoRenderer());

        add(crearCabecera(), BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(crearInferior(), BorderLayout.SOUTH);
    }

    private JPanel crearCabecera() {
        JPanel contenedor = new JPanel(new BorderLayout(4, 14));
        contenedor.setOpaque(false);
        JPanel titulo = new JPanel(new BorderLayout(4, 4));
        titulo.setOpaque(false);
        titulo.add(UiTheme.titulo("Importar Excel"), BorderLayout.NORTH);
        titulo.add(UiTheme.ayuda("Seleccione el archivo del mes. Se leera solo Hoja3 para cargar las facturas."), BorderLayout.CENTER);

        JPanel selector = UiTheme.panelBlanco();
        selector.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton seleccionar = UiTheme.botonSecundario("Seleccionar archivo");
        JButton leer = UiTheme.botonPrimario("Leer archivo");
        seleccionar.addActionListener(event -> seleccionarExcel());
        leer.addActionListener(event -> leerArchivo());
        selector.add(archivoField);
        selector.add(seleccionar);
        selector.add(leer);

        contenedor.add(titulo, BorderLayout.NORTH);
        contenedor.add(selector, BorderLayout.SOUTH);
        return contenedor;
    }

    private JPanel crearInferior() {
        JPanel inferior = new JPanel(new BorderLayout(10, 10));
        inferior.setOpaque(false);

        JPanel resumen = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        resumen.setOpaque(false);
        resumen.add(totalFilasLabel);
        resumen.add(validasLabel);
        resumen.add(erroresLabel);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        JButton validar = UiTheme.botonSecundario("Validar datos");
        JButton continuar = UiTheme.botonPrimario("Continuar a generacion");
        JButton cancelar = UiTheme.botonSecundario("Cancelar importacion");
        validar.addActionListener(event -> validarDatos());
        continuar.addActionListener(event -> {
            if (appState.getValidaciones().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Primero valida los datos del Excel.");
                return;
            }
            navegar.accept("Generar facturas");
        });
        cancelar.addActionListener(event -> limpiarImportacion());
        botones.add(validar);
        botones.add(continuar);
        botones.add(cancelar);

        inferior.add(resumen, BorderLayout.WEST);
        inferior.add(botones, BorderLayout.EAST);
        return inferior;
    }

    private void seleccionarExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Hojas de calculo (*.ods, *.xlsx, *.xls)", "ods", "xlsx", "xls"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();
            appState.setArchivoExcel(archivo);
            archivoField.setText(archivo.getAbsolutePath());
        }
    }

    private void leerArchivo() {
        if (appState.getArchivoExcel() == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo.");
            return;
        }
        try {
            List<FacturaExcel> filas = excelService.leerFacturas(appState.getArchivoExcel());
            appState.setFilasExcel(filas);
            appState.setValidaciones(List.of());
            cargarTablaSinValidar(filas);
            actualizarResumen(filas.size(), 0, 0);
            JOptionPane.showMessageDialog(this, "Archivo leido correctamente.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validarDatos() {
        if (appState.getFilasExcel().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero lee un archivo Excel.");
            return;
        }

        try {
            Optional<Empresa> empresa = empresaDAO.obtenerPrincipal();
            Set<String> facturasVistas = new HashSet<>();
            List<ValidacionFacturaExcel> validaciones = appState.getFilasExcel().stream()
                    .map(fila -> validarFila(fila, empresa.orElse(null), facturasVistas))
                    .toList();
            appState.setValidaciones(validaciones);
            cargarTablaValidada(validaciones);
            long validas = validaciones.stream().filter(ValidacionFacturaExcel::isCorrecta).count();
            actualizarResumen(validaciones.size(), validas, validaciones.size() - validas);
            if (validas < validaciones.size()) {
                JOptionPane.showMessageDialog(this, "Hay facturas con clientes que no existen en la base de datos o datos incompletos.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudieron validar los datos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ValidacionFacturaExcel validarFila(FacturaExcel fila, Empresa empresa, Set<String> facturasVistas) {
        if (empresa == null) {
            return new ValidacionFacturaExcel(fila, null, null, "No hay datos de empresa configurados", false);
        }
        if (vacio(fila.getNumero())) {
            return new ValidacionFacturaExcel(fila, null, empresa, "Numero de factura vacio", false);
        }
        if (!fila.getNumero().matches("\\d+")) {
            return new ValidacionFacturaExcel(fila, null, empresa, "Numero de factura invalido", false);
        }
        if (!facturasVistas.add(fila.getNumero())) {
            return new ValidacionFacturaExcel(fila, null, empresa, "Numero de factura repetido", false);
        }
        if (vacio(fila.getNombreCliente())) {
            return new ValidacionFacturaExcel(fila, null, empresa, "Razon social vacia", false);
        }
        if (fila.getTotalConIva() == null || fila.getTotalConIva().compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidacionFacturaExcel(fila, null, empresa, "Precio invalido", false);
        }
        if (fila.getBaseImponible() == null || fila.getBaseImponible().compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidacionFacturaExcel(fila, null, empresa, "Base imponible invalida", false);
        }
        if (fila.getIva() == null || fila.getIva().compareTo(BigDecimal.ZERO) < 0) {
            return new ValidacionFacturaExcel(fila, null, empresa, "IVA invalido", false);
        }
        try {
            List<Cliente> clientes = clienteDAO.buscarPorNombreFiscal(fila.getNombreCliente());
            if (clientes.isEmpty()) {
                return new ValidacionFacturaExcel(fila, null, empresa, "Cliente no encontrado", false);
            }
            if (clientes.size() > 1) {
                return new ValidacionFacturaExcel(fila, null, empresa, "Razon social duplicada", false);
            }
            Cliente encontrado = clientes.get(0);
            if (vacio(encontrado.getNif())) {
                return new ValidacionFacturaExcel(fila, encontrado, empresa, "El cliente no tiene NIF", false);
            }
            if (vacio(encontrado.getDireccion())) {
                return new ValidacionFacturaExcel(fila, encontrado, empresa, "El cliente no tiene direccion", false);
            }
            return new ValidacionFacturaExcel(fila, encontrado, empresa, "Correcto", true);
        } catch (SQLException e) {
            return new ValidacionFacturaExcel(fila, null, empresa, "No se pudo consultar el cliente", false);
        }
    }

    private void cargarTablaSinValidar(List<FacturaExcel> filas) {
        tableModel.setRowCount(0);
        for (FacturaExcel fila : filas) {
            tableModel.addRow(new Object[]{
                    valor(fila.getNumero()),
                    valor(fila.getNombreCliente()),
                    valor(fila.getConcepto()),
                    fila.getTotalConIva(),
                    base(fila),
                    iva(fila),
                    fila.getFecha() == null ? "" : fila.getFecha().format(FORMATO_FECHA),
                    "Pendiente"
            });
        }
    }

    private void cargarTablaValidada(List<ValidacionFacturaExcel> validaciones) {
        tableModel.setRowCount(0);
        for (ValidacionFacturaExcel validacion : validaciones) {
            FacturaExcel fila = validacion.getFila();
            tableModel.addRow(new Object[]{
                    valor(fila.getNumero()),
                    validacion.getCliente() == null ? valor(fila.getNombreCliente()) : nombreFiscal(validacion.getCliente()),
                    valor(fila.getConcepto()),
                    fila.getTotalConIva(),
                    base(fila),
                    iva(fila),
                    fila.getFecha() == null ? "" : fila.getFecha().format(FORMATO_FECHA),
                    validacion.getResultado()
            });
        }
    }

    private void actualizarResumen(long total, long validas, long errores) {
        totalFilasLabel.setText("Total de filas leidas: " + total);
        validasLabel.setText("Facturas validas: " + validas);
        erroresLabel.setText("Facturas con errores: " + errores);
    }

    private void limpiarImportacion() {
        appState.setArchivoExcel(null);
        appState.setFilasExcel(List.of());
        appState.setValidaciones(List.of());
        archivoField.setText("");
        tableModel.setRowCount(0);
        actualizarResumen(0, 0, 0);
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private BigDecimal base(FacturaExcel fila) {
        if (fila.getBaseImponible() != null) {
            return fila.getBaseImponible().setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = total(fila);
        return total.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : total.divide(new BigDecimal("1.21"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal iva(FacturaExcel fila) {
        if (fila.getIva() != null) {
            return fila.getIva().setScale(2, RoundingMode.HALF_UP);
        }
        return total(fila).subtract(base(fila)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal total(FacturaExcel fila) {
        return fila.getTotalConIva() == null ? BigDecimal.ZERO : fila.getTotalConIva().setScale(2, RoundingMode.HALF_UP);
    }

    private String nombreFiscal(Cliente cliente) {
        String razonSocial = cliente.getRazonSocial();
        return razonSocial == null || razonSocial.isBlank() ? valor(cliente.getNombre()) : razonSocial;
    }

    private class EstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Object estado = table.getValueAt(row, 7);
            boolean error = estado != null && !"Correcto".equals(estado.toString()) && !"Pendiente".equals(estado.toString());
            if (!isSelected) {
                component.setBackground(error ? UiTheme.ERROR_ROW : Color.WHITE);
                component.setForeground(error && column == 7 ? Color.RED.darker() : UiTheme.TEXT);
            }
            return component;
        }
    }
}
