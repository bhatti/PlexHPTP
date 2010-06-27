package com.plexobject.hptp.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.plexobject.hptp.client.TransferManager;
import com.plexobject.hptp.client.TransferManagerImpl;
import com.plexobject.hptp.client.http.TransferClientHttp;
import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.MessageHandler;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferException;
import com.plexobject.hptp.domain.TransferListener;
import com.plexobject.hptp.util.MessageResourceUtils;
import com.plexobject.hptp.util.ExtensionUtils;

public class FileInfoPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final TransferManager uploader;
    private final MessageHandler messageHandler;
    private final String group;
    private JTable table;
    private FileInfoModel model;
    private JButton addButton;
    private JButton removeButton;
    private JButton transferButton;
    private JButton removeUploadedButton;

    private JButton pauseButton;

    public FileInfoPanel(final MessageHandler aMessageHandler) {
        messageHandler = aMessageHandler;
        uploader = new TransferManagerImpl(messageHandler, new TransferClientHttp());

        group = Configuration.getInstance().getGroup();

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setLayout(new BorderLayout());

                setupTable();

                setupButtons();

                setupSelection();
            }
        });
    }

    private void setupTable() {
        model = new FileInfoModel();
        model.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                resetButtons();
            }
        });
        table = new JTable(model) {
            private static final long serialVersionUID = 1L;

            // Implement table cell tool tips.
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);

                return model.getFileInfoAt(rowIndex).getDescription();
            }

            // Implement table header tool tips.
            protected JTableHeader createDefaultTableHeader() {
                final String[] columnToolTips = {
                        null,
                        null,
                        MessageResourceUtils.getInstance().getString(
                                "FileNameTip"),
                        MessageResourceUtils.getInstance().getString(
                                "FileSizeTip"),
                        MessageResourceUtils.getInstance().getString(
                                "StatusTip") };

                return new JTableHeader(columnModel) {
                    private static final long serialVersionUID = 1L;

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index)
                                .getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }
        };

        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        initColumnSizes(table);
        for (int i = 0; i < 3; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(
                    new EvenOddCellRenderer());
        }
        TableColumn pbColumn = table.getColumnModel().getColumn(3);
        pbColumn.setCellRenderer(new ProgressRenderer());
        pbColumn.setPreferredWidth(250);

        table.getSelectionModel().addListSelectionListener(new RowListener());
        table.getColumnModel().getSelectionModel().addListSelectionListener(
                new ColumnListener());

        // Add the scroll pane to this panel.
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        addButton = new JButton(MessageResourceUtils.getInstance().getString(
                "AddFiles"));
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addFileInfo();
            }

        });
        removeButton = new JButton(MessageResourceUtils.getInstance()
                .getString("RemoveFiles"));
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                removeSelectedFiles();
            }
        });
        transferButton = new JButton(MessageResourceUtils.getInstance()
                .getString("StartTransfer"));
        transferButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                transfer();
            }
        });
        pauseButton = new JButton(MessageResourceUtils.getInstance().getString(
                "Pause"));
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                cancelTransfer();
            }
        });

        removeUploadedButton = new JButton(MessageResourceUtils.getInstance()
                .getString("RemoveUploaded"));
        removeUploadedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                model.clearUploaded();
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(transferButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(removeUploadedButton);
        resetButtons();
        add(buttonPanel, BorderLayout.NORTH);
    }

    private void setupSelection() {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        ListSelectionModel rowSM = table.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                // Ignore extra messages.
                if (e.getValueIsAdjusting()) {
                    return;
                }
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (lsm.isSelectionEmpty()) {
                } else {
                    // int selectedRow = lsm.getMinSelectionIndex();
                }
            }
        });
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
        }
    }

    private class ColumnListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
        }
    }

    private void initColumnSizes(JTable table) {
        FileInfoModel model = (FileInfoModel) table.getModel();
        TableColumn column = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
        Object[] longValues = FileInfoModel.LONG_VALUES;
        TableCellRenderer headerRenderer = table.getTableHeader()
                .getDefaultRenderer();

        for (int i = 0; i < FileInfoModel.COLUMN_NAMES.length; i++) {
            column = table.getColumnModel().getColumn(i);

            comp = headerRenderer.getTableCellRendererComponent(null, column
                    .getHeaderValue(), false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;

            comp = table.getDefaultRenderer(model.getColumnClass(i))
                    .getTableCellRendererComponent(table, longValues[i], false,
                            false, 0, i);
            cellWidth = comp.getPreferredSize().width;

            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }

    private void addFileInfo() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final JFileChooser fc = new JFileChooser();
                fc.setMultiSelectionEnabled(true);
                fc.addChoosableFileFilter(ExtensionUtils.newFileFilter());
                int returnVal = fc.showOpenDialog(FileInfoPanel.this);
                // String m = FileHandler.open();
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] files = fc.getSelectedFiles();
                    model.addFiles(files);
                } else {
                    messageHandler
                            .handleMessage("Open command cancelled by user.");
                }
            }
        });

    }

    private void removeSelectedFiles() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int[] rows = table.getSelectedRows();
                model.removeRows(rows);
            }
        });
    }

    private void resetButtons() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                List<FileInfo> files = model.getIncompleteFiles();
                if (files.size() > 0 && files.size() < model.getRowCount()) {
                    transferButton.setText(MessageResourceUtils.getInstance()
                            .getString("Resume"));
                } else {
                    transferButton.setText(MessageResourceUtils.getInstance()
                            .getString("StartTransfer"));
                }
                transferButton.setEnabled(files.size() > 0);
                removeUploadedButton.setEnabled(files.size() > 0
                        && files.size() < model.getRowCount());
                pauseButton.setEnabled(false);
                addButton.setEnabled(true);
                removeButton.setEnabled(model.getRowCount() > 0);
            }
        });

    }

    private void transfer() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                List<FileInfo> files = model.getIncompleteFiles();
                uploader.upload(group, files, new TransferListener() {
                    public UploadCallbackStatus uploadStatus(TransferEvent event) {
                        model.uploadStatus(event);
                        return UploadCallbackStatus.CONTINUE_UPLOAD;
                    }

                    public void uploadFailed(TransferException error) {
                        model.uploadFailed(error);
                        messageHandler.handleException(error);
                    }

                    public void allTransferCompleted() {
                        resetButtons();
                    }

                    public void uploadCancelled(final TransferEvent event) {
                        model.uploadCancelled(event);
                        messageHandler.handleMessage(event.getFileInfo()
                                + " cancelled!");
                    }

                    public void uploadCompleted(TransferEvent event) {
                        model.uploadCompleted(event);
                        messageHandler.handleMessage(event.getFileInfo()
                                + " completed!");
                    }

                    public void alreadyUploaded(TransferEvent event) {
                        model.alreadyUploaded(event);
                        messageHandler.handleMessage(event.getFileInfo()
                                + " is already uploaded!!!");
                    }
                });
                transferButton.setEnabled(false);
                removeUploadedButton.setEnabled(false);

                pauseButton.setEnabled(true);
                addButton.setEnabled(false);
                removeButton.setEnabled(false);
            }
        });

    }

    void cancelTransfer() {
        uploader.cancel();
        resetButtons();
    }

}
