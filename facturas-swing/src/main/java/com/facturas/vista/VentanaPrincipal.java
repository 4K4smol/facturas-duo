package com.facturas.vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public class VentanaPrincipal extends JFrame {
    private final AppState appState = new AppState();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelCentral = new JPanel(cardLayout);
    private final Map<String, JButton> botonesMenu = new LinkedHashMap<>();

    public VentanaPrincipal() {
        UiTheme.aplicarLookAndFeel();
        setTitle("Gestor de Facturas");
        setSize(1000, 650);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(UiTheme.BACKGROUND);
        raiz.add(crearEncabezado(), BorderLayout.NORTH);
        raiz.add(crearMenuLateral(), BorderLayout.WEST);
        raiz.add(panelCentral, BorderLayout.CENTER);

        crearPantallas();
        setContentPane(raiz);
        mostrar("Inicio");
    }

    private JPanel crearEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout(8, 2));
        encabezado.setBackground(Color.WHITE);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(16, 22, 14, 22)
        ));

        JLabel titulo = UiTheme.titulo("Gestor de Facturas");
        JLabel subtitulo = UiTheme.ayuda("Gestion de clientes, empresa, importacion de Excel y generacion de facturas PDF");
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(subtitulo, BorderLayout.CENTER);
        return encabezado;
    }

    private JPanel crearMenuLateral() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(new Color(236, 240, 245));
        contenedor.setPreferredSize(new Dimension(220, 0));
        contenedor.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.BORDER));

        JPanel botones = new JPanel(new GridLayout(0, 1, 0, 8));
        botones.setOpaque(false);
        botones.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));

        agregarBotonMenu(botones, "Inicio");
        agregarBotonMenu(botones, "Datos de mi empresa");
        agregarBotonMenu(botones, "Clientes");
        agregarBotonMenu(botones, "Importar Excel");
        agregarBotonMenu(botones, "Generar facturas");
        agregarBotonMenu(botones, "Facturas generadas");
        agregarBotonMenu(botones, "Configuracion");

        JPanel versionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        versionPanel.setOpaque(false);
        versionPanel.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        JLabel version = UiTheme.ayuda("Version 1.0");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionPanel.add(version);

        contenedor.add(botones, BorderLayout.NORTH);
        contenedor.add(versionPanel, BorderLayout.SOUTH);
        return contenedor;
    }

    private void agregarBotonMenu(JPanel contenedor, String nombre) {
        JButton boton = UiTheme.botonSecundario(nombre);
        boton.setHorizontalAlignment(JButton.LEFT);
        boton.addActionListener(event -> mostrar(nombre));
        botonesMenu.put(nombre, boton);
        contenedor.add(boton);
    }

    private void crearPantallas() {
        panelCentral.add(new PanelInicio(appState, this::mostrar), "Inicio");
        panelCentral.add(new PanelEmpresa(), "Datos de mi empresa");
        panelCentral.add(new PanelClientes(), "Clientes");
        panelCentral.add(new PanelImportarExcel(appState, this::mostrar), "Importar Excel");
        panelCentral.add(new PanelGenerarFacturas(appState, this::mostrar), "Generar facturas");
        panelCentral.add(new PanelFacturasGeneradas(appState), "Facturas generadas");
        panelCentral.add(new PanelConfiguracion(appState), "Configuracion");
    }

    private void mostrar(String pantalla) {
        cardLayout.show(panelCentral, pantalla);
        botonesMenu.forEach((nombre, boton) -> {
            boolean seleccionado = nombre.equals(pantalla);
            boton.setBackground(seleccionado ? UiTheme.PRIMARY_SELECTED : UiTheme.SECONDARY);
            boton.setForeground(seleccionado ? Color.WHITE : UiTheme.TEXT);
        });
    }
}
