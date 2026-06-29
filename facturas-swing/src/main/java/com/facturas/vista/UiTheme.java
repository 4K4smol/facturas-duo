package com.facturas.vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

final class UiTheme {
    static final Color BACKGROUND = new Color(245, 247, 250);
    static final Color PANEL = Color.WHITE;
    static final Color BORDER = new Color(218, 224, 232);
    static final Color TEXT = new Color(34, 40, 49);
    static final Color MUTED_TEXT = new Color(91, 101, 115);
    static final Color PRIMARY = new Color(25, 85, 140);
    static final Color PRIMARY_SELECTED = new Color(18, 67, 112);
    static final Color SECONDARY = new Color(232, 236, 242);
    static final Color DANGER = new Color(184, 69, 69);
    static final Color ERROR_ROW = new Color(255, 235, 235);
    static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 16);
    static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private UiTheme() {
    }

    static void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing falls back to its default look and feel.
        }
        UIManager.put("Label.font", NORMAL_FONT);
        UIManager.put("Button.font", NORMAL_FONT);
        UIManager.put("TextField.font", NORMAL_FONT);
        UIManager.put("Table.font", NORMAL_FONT);
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));
    }

    static JPanel panelContenido() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        return panel;
    }

    static JPanel panelBlanco() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(bordePanel());
        return panel;
    }

    static Border bordePanel() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
    }

    static JLabel titulo(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT);
        return label;
    }

    static JLabel seccion(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(SECTION_FONT);
        label.setForeground(TEXT);
        return label;
    }

    static JLabel ayuda(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(SUBTITLE_FONT);
        label.setForeground(MUTED_TEXT);
        return label;
    }

    static JButton botonPrimario(String texto) {
        JButton button = botonBase(texto);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        return button;
    }

    static JButton botonSecundario(String texto) {
        JButton button = botonBase(texto);
        button.setBackground(SECONDARY);
        button.setForeground(TEXT);
        return button;
    }

    static JButton botonEliminar(String texto) {
        JButton button = botonBase(texto);
        button.setBackground(DANGER);
        button.setForeground(Color.WHITE);
        return button;
    }

    static void estilizarCampo(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    static void estilizarTabla(JTable table) {
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(new Color(234, 238, 244));
        table.setSelectionBackground(new Color(215, 231, 247));
        table.setSelectionForeground(TEXT);
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(238, 242, 247));
        header.setForeground(TEXT);
        header.setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
    }

    static void fondo(JComponent component) {
        component.setBackground(BACKGROUND);
    }

    private static JButton botonBase(String texto) {
        JButton button = new JButton(texto);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
