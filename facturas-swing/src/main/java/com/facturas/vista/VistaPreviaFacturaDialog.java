package com.facturas.vista;

import com.facturas.modelo.Cliente;
import com.facturas.modelo.Empresa;
import com.facturas.modelo.FacturaExcel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

class VistaPreviaFacturaDialog extends JDialog {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final List<ValidacionFacturaExcel> facturas;
    private final String formaPago;
    private final JPanel hojaPanel = new JPanel(new BorderLayout());
    private int indice;

    VistaPreviaFacturaDialog(Window owner, List<ValidacionFacturaExcel> validaciones, String formaPago) {
        super(owner, "Vista previa de factura", ModalityType.APPLICATION_MODAL);
        this.facturas = validaciones.stream().filter(ValidacionFacturaExcel::isCorrecta).toList();
        this.formaPago = formaPago;
        setSize(760, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(new Color(220, 224, 230));

        JScrollPane scrollPane = new JScrollPane(hojaPanel);
        scrollPane.getViewport().setBackground(new Color(220, 224, 230));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(14, 14, 0, 14));
        add(scrollPane, BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);
        actualizarFactura();
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        botones.setBackground(new Color(220, 224, 230));
        JButton anterior = UiTheme.botonSecundario("Anterior");
        JButton siguiente = UiTheme.botonSecundario("Siguiente");
        JButton generar = UiTheme.botonPrimario("Generar esta factura");
        JButton cerrar = UiTheme.botonSecundario("Cerrar vista previa");
        anterior.addActionListener(event -> {
            if (indice > 0) {
                indice--;
                actualizarFactura();
            }
        });
        siguiente.addActionListener(event -> {
            if (indice < facturas.size() - 1) {
                indice++;
                actualizarFactura();
            }
        });
        generar.addActionListener(event -> dispose());
        cerrar.addActionListener(event -> dispose());
        botones.add(anterior);
        botones.add(siguiente);
        botones.add(generar);
        botones.add(cerrar);
        return botones;
    }

    private void actualizarFactura() {
        hojaPanel.removeAll();
        hojaPanel.setBackground(new Color(220, 224, 230));
        hojaPanel.add(crearHoja(facturas.get(indice)), BorderLayout.CENTER);
        hojaPanel.revalidate();
        hojaPanel.repaint();
    }

    private JPanel crearHoja(ValidacionFacturaExcel validacion) {
        JPanel hoja = new JPanel();
        hoja.setLayout(new BoxLayout(hoja, BoxLayout.Y_AXIS));
        hoja.setBackground(Color.WHITE);
        hoja.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(18, 18, 18, 18),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(205, 205, 205)),
                        BorderFactory.createEmptyBorder(28, 32, 28, 32)
                )
        ));
        hoja.setPreferredSize(new Dimension(620, 850));

        hoja.add(crearBloqueSuperior(validacion.getEmpresa()));
        hoja.add(Box.createVerticalStrut(12));
        hoja.add(crearFranjaFactura(validacion.getEmpresa(), validacion.getFila()));
        hoja.add(Box.createVerticalStrut(12));
        hoja.add(crearBloqueCliente(validacion.getCliente()));
        hoja.add(Box.createVerticalStrut(12));
        hoja.add(crearBloqueConcepto(validacion.getFila()));
        hoja.add(Box.createVerticalStrut(12));
        hoja.add(crearBloqueImporte(validacion.getFila()));
        hoja.add(Box.createVerticalStrut(18));
        hoja.add(label("Tipo de pago: " + formaPago, Font.BOLD, 13));
        return hoja;
    }

    private JPanel crearBloqueSuperior(Empresa empresa) {
        JPanel panel = bloqueConBorde(new GridLayout(1, 2, 12, 0));
        JPanel datos = new JPanel(new GridLayout(0, 1, 2, 2));
        datos.setOpaque(false);
        datos.add(label(valor(empresa.getNombre()), Font.BOLD, 14));
        datos.add(label("DNI/NIF/CIF: " + valor(empresa.getNif()), Font.PLAIN, 12));
        datos.add(label(valor(empresa.getDireccion()), Font.PLAIN, 12));
        datos.add(label(valor(empresa.getCodigoPostal()) + " " + valor(empresa.getLocalidad()), Font.PLAIN, 12));
        datos.add(label("Telefono: " + valor(empresa.getTelefono()), Font.PLAIN, 12));
        datos.add(label("Email: " + valor(empresa.getEmail()), Font.PLAIN, 12));

        JPanel logo = new JPanel(new BorderLayout(4, 4));
        logo.setOpaque(false);
        JLabel logoLabel = new JLabel(valor(empresa.getNombre()), JLabel.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        if (empresa.getLogoPath() != null && !empresa.getLogoPath().isBlank()) {
            ImageIcon icon = new ImageIcon(empresa.getLogoPath());
            Image image = icon.getImage().getScaledInstance(130, 80, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(image));
            logoLabel.setText("");
        }
        logo.add(logoLabel, BorderLayout.CENTER);

        panel.add(datos);
        panel.add(logo);
        return panel;
    }

    private JPanel crearFranjaFactura(Empresa empresa, FacturaExcel fila) {
        JPanel panel = bloqueConBorde(new GridLayout(1, 2));
        panel.add(label(valor(empresa.getLocalidad()) + ", " + fila.getFecha().format(FORMATO_FECHA), Font.PLAIN, 12));
        panel.add(label("N factura: " + valor(fila.getNumero()), Font.BOLD, 12));
        return panel;
    }

    private JPanel crearBloqueCliente(Cliente cliente) {
        JPanel panel = bloqueConBorde(new GridLayout(0, 1, 2, 2));
        panel.add(label("Cliente", Font.BOLD, 13));
        panel.add(label("Nombre comercial: " + valor(cliente.getNombre()), Font.PLAIN, 12));
        panel.add(label("Nombre / razon social: " + nombreFiscal(cliente), Font.PLAIN, 12));
        panel.add(label("CIF/NIF: " + valor(cliente.getNif()), Font.PLAIN, 12));
        panel.add(label(valor(cliente.getDireccion()), Font.PLAIN, 12));
        panel.add(label(valor(cliente.getCodigoPostal()) + " " + valor(cliente.getLocalidad()), Font.PLAIN, 12));
        if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()) {
            panel.add(label("Telefono: " + cliente.getTelefono(), Font.PLAIN, 12));
        }
        return panel;
    }

    private JPanel crearBloqueConcepto(FacturaExcel fila) {
        JPanel panel = bloqueConBorde(new BorderLayout(0, 8));
        panel.add(label("CONCEPTO", Font.BOLD, 13), BorderLayout.NORTH);
        JPanel tabla = new JPanel(new GridLayout(2, 2));
        tabla.setOpaque(false);
        tabla.add(label("Trabajo", Font.BOLD, 12));
        tabla.add(label("Precio (sin IVA)", Font.BOLD, 12));
        tabla.add(label(concepto(fila), Font.PLAIN, 12));
        tabla.add(label(moneda(base(fila)), Font.PLAIN, 12));
        panel.add(tabla, BorderLayout.CENTER);
        panel.add(label("Total: " + moneda(base(fila)), Font.BOLD, 12), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearBloqueImporte(FacturaExcel fila) {
        JPanel panel = bloqueConBorde(new BorderLayout(0, 8));
        panel.add(label("IMPORTE", Font.BOLD, 13), BorderLayout.NORTH);
        JPanel tabla = new JPanel(new GridLayout(4, 2));
        tabla.setOpaque(false);
        tabla.add(label("Concepto", Font.BOLD, 12));
        tabla.add(label("Total", Font.BOLD, 12));
        tabla.add(label("Base imponible", Font.PLAIN, 12));
        tabla.add(label(moneda(base(fila)), Font.PLAIN, 12));
        tabla.add(label("IVA 21%", Font.PLAIN, 12));
        tabla.add(label(moneda(iva(fila)), Font.PLAIN, 12));
        JLabel totalTitulo = label("TOTAL A COBRAR", Font.BOLD, 13);
        JLabel totalValor = label(moneda(total(fila)), Font.BOLD, 13);
        totalTitulo.setOpaque(true);
        totalValor.setOpaque(true);
        totalTitulo.setBackground(new Color(235, 238, 242));
        totalValor.setBackground(new Color(235, 238, 242));
        tabla.add(totalTitulo);
        tabla.add(totalValor);
        panel.add(tabla, BorderLayout.CENTER);
        return panel;
    }

    private JPanel bloqueConBorde(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        return panel;
    }

    private JLabel label(String texto, int estilo, int tamano) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Segoe UI", estilo, tamano));
        label.setForeground(UiTheme.TEXT);
        return label;
    }

    private BigDecimal base(FacturaExcel fila) {
        return fila.getBaseImponible() == null ? BigDecimal.ZERO : fila.getBaseImponible();
    }

    private BigDecimal iva(FacturaExcel fila) {
        return fila.getIva() == null ? BigDecimal.ZERO : fila.getIva();
    }

    private BigDecimal total(FacturaExcel fila) {
        return fila.getTotalConIva() == null ? BigDecimal.ZERO : fila.getTotalConIva();
    }

    private String moneda(BigDecimal cantidad) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(cantidad);
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private String concepto(FacturaExcel fila) {
        String concepto = fila.getConcepto();
        return concepto == null || concepto.isBlank() ? "Limpieza de cristales" : concepto;
    }

    private String nombreFiscal(Cliente cliente) {
        String razonSocial = cliente.getRazonSocial();
        return razonSocial == null || razonSocial.isBlank() ? valor(cliente.getNombre()) : razonSocial;
    }
}
