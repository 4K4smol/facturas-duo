package com.facturas.vista;

import com.facturas.dao.ClienteDAO;
import com.facturas.dao.EmpresaDAO;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

class PanelInicio extends JPanel {
    private final AppState appState;
    private final Consumer<String> navegar;
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final JLabel clientesValor = valorGrande("0");
    private final JLabel empresaValor = valorGrande("No");
    private final JLabel excelValor = valorGrande("-");
    private final JLabel facturasValor = valorGrande("0");

    PanelInicio(AppState appState, Consumer<String> navegar) {
        this.appState = appState;
        this.navegar = navegar;
        setLayout(new BorderLayout(16, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 24, 22, 24));

        add(crearCabecera(), BorderLayout.NORTH);
        add(crearContenido(), BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                actualizarResumen();
            }
        });
        actualizarResumen();
    }

    private JPanel crearCabecera() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setOpaque(false);
        panel.add(UiTheme.titulo("Inicio"), BorderLayout.NORTH);
        panel.add(UiTheme.ayuda("Resumen general del flujo de facturacion."), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearContenido() {
        JPanel contenido = new JPanel(new BorderLayout(16, 16));
        contenido.setOpaque(false);
        contenido.add(crearTarjetas(), BorderLayout.NORTH);
        contenido.add(crearFlujo(), BorderLayout.CENTER);
        contenido.add(crearAccesosRapidos(), BorderLayout.SOUTH);
        return contenido;
    }

    private JPanel crearTarjetas() {
        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 14, 14));
        tarjetas.setOpaque(false);
        tarjetas.add(tarjeta("Clientes registrados", clientesValor));
        tarjetas.add(tarjeta("Empresa configurada", empresaValor));
        tarjetas.add(tarjeta("Ultimo archivo importado", excelValor));
        tarjetas.add(tarjeta("Facturas generadas", facturasValor));
        return tarjetas;
    }

    private JPanel tarjeta(String titulo, JLabel valor) {
        JPanel panel = UiTheme.panelBlanco();
        panel.setLayout(new BorderLayout(4, 10));
        panel.add(UiTheme.ayuda(titulo), BorderLayout.NORTH);
        panel.add(valor, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearFlujo() {
        JPanel panel = UiTheme.panelBlanco();
        panel.setLayout(new GridLayout(0, 1, 8, 8));
        panel.add(UiTheme.seccion("Flujo de trabajo"));
        panel.add(UiTheme.ayuda("1. Configurar los datos de mi empresa."));
        panel.add(UiTheme.ayuda("2. Registrar clientes."));
        panel.add(UiTheme.ayuda("3. Importar Excel/CSV mensual."));
        panel.add(UiTheme.ayuda("4. Generar facturas PDF."));
        return panel;
    }

    private JPanel crearAccesosRapidos() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);

        JButton cliente = UiTheme.botonPrimario("Anadir cliente");
        JButton importar = UiTheme.botonSecundario("Importar Excel/CSV");
        JButton generar = UiTheme.botonSecundario("Generar facturas");

        cliente.addActionListener(event -> navegar.accept("Clientes"));
        importar.addActionListener(event -> navegar.accept("Importar Excel/CSV"));
        generar.addActionListener(event -> navegar.accept("Generar facturas"));

        panel.add(cliente);
        panel.add(importar);
        panel.add(generar);
        return panel;
    }

    private void actualizarResumen() {
        try {
            clientesValor.setText(String.valueOf(clienteDAO.contar()));
            empresaValor.setText(empresaDAO.obtenerPrincipal().isPresent() ? "Si" : "No");
            excelValor.setText(appState.getArchivoExcel() == null ? "-" : appState.getArchivoExcel().getName());
            facturasValor.setText(String.valueOf(contarFacturas()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el resumen.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private long contarFacturas() throws IOException {
        if (!Files.isDirectory(appState.getCarpetaSalida())) {
            return 0;
        }
        try (var paths = Files.list(appState.getCarpetaSalida())) {
            return paths.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf")).count();
        }
    }

    private static JLabel valorGrande(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        label.setForeground(UiTheme.TEXT);
        return label;
    }
}
