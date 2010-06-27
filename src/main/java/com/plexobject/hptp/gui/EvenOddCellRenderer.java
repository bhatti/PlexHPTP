package com.plexobject.hptp.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class EvenOddCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    public static final Color DARK_GRAY = new Color(0x66, 0x66, 0x66);
    public static final Color LIGHT_GRAY = new Color(0xCC, 0xCC, 0xCC);
    public static final Color BLUE = new Color(0x33, 0x99, 0xFF);

    public EvenOddCellRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component renderer = super.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);
        Color background;
        if (isSelected) {
            background = BLUE;
        } else {
            if (row % 2 == 0) {
                background = DARK_GRAY;
            } else {
                background = LIGHT_GRAY;
            }
        }
        renderer.setBackground(background);
        return renderer;
    }
}
