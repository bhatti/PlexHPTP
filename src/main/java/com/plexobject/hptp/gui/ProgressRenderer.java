package com.plexobject.hptp.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.plaf.metal.MetalProgressBarUI;
import javax.swing.table.DefaultTableCellRenderer;

import com.plexobject.hptp.domain.FileInfo;

public class ProgressRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel messageL = new JLabel("");
    private final JPanel container = new JPanel();
    public static final Color GREEN = new Color(0x33, 0x66, 0x33);
    public static final Color RED = new Color(0xB2, 0x00, 0x00);
    public static final Color GRAY = new Color(0x33, 0x33, 0x33);

    public ProgressRenderer() {
        setOpaque(true);
        progressBar.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        progressBar.setStringPainted(true);
        progressBar.setOpaque(true);
        progressBar.setIndeterminate(false);
        progressBar.setBorderPainted(true);
        progressBar.setUI(new MetalProgressBarUI());
        progressBar.setForeground(GREEN);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(progressBar);
        container.add(messageL);
        container.setBorder(BorderFactory.createEtchedBorder());
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        FileInfo info = (FileInfo) value;
        messageL.setText(info.getDescription());
        int percent = info.getPercentCompleted();
        progressBar.setValue(percent);
        progressBar.setString(percent + "%");

        if (info.isCancelled()) {
            progressBar.setBackground(RED);
            progressBar.setString("");
        } else if (info.isFailed()) {
            progressBar.setBackground(RED);
        } else if (info.isCompleted()) {
            progressBar.setBackground(GREEN);
            progressBar.setValue(100);
            progressBar.setString("");
        } else {
            if (percent < 100) {
                progressBar.setBackground(GRAY);
            } else {
                progressBar.setBackground(GREEN);
            }
        }
        Dimension pbDim = progressBar.getPreferredSize();
        Dimension mDim = messageL.getPreferredSize();
        table.setRowHeight(row, pbDim.height + mDim.height + 5);
        return container;
    }

}
