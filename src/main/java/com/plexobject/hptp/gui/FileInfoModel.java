package com.plexobject.hptp.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferException;
import com.plexobject.hptp.domain.TransferListener;
import com.plexobject.hptp.util.MessageResourceUtils;

public class FileInfoModel extends AbstractTableModel implements
        TransferListener {
    private static final int BYTE_SIZE = 1;
    private static final int NAME_COLUMN = 0;
    private static final int PROGRESS_COLUMN = 3;
    private static final int MD5_COLUMN = 2;
    private static final long serialVersionUID = 1L;
    static final String[] COLUMN_NAMES = {
            MessageResourceUtils.getInstance().getString("FileName"),
            MessageResourceUtils.getInstance().getString("FileSize"),
            MessageResourceUtils.getInstance().getString("MD5"),
            MessageResourceUtils.getInstance().getString("Status"), };
    static final Object[] LONG_VALUES = { "XXXXXXXXXXXXXXXXXXXXXXXXXX",
            "XXXXXXXXXX", "XXXXXXXXXXX",
            "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" };
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    final List<FileInfo> fileInfos = Collections
            .synchronizedList(new ArrayList<FileInfo>());

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public int getRowCount() {
        return fileInfos.size();
    }

    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

    public FileInfo getFileInfoAt(int row) {
        return fileInfos.get(row);
    }

    public List<FileInfo> getIncompleteFiles() {
        List<FileInfo> list = new ArrayList<FileInfo>();
        for (FileInfo info : fileInfos) {
            if (!info.isCompleted()) {
                list.add(info);
            }
        }
        return list;
    }

    public List<FileInfo> getFileInfosAt(int[] rows) {
        List<FileInfo> list = new ArrayList<FileInfo>();
        for (int row : rows) {
            list.add(fileInfos.get(row));
        }
        return list;
    }

    public void addFiles(File[] files) {
        int added = 0;
        final List<FileInfo> oldFileInfos = new ArrayList<FileInfo>(fileInfos);

        for (File file : files) {
            FileInfo info = new FileInfo(file);
            if (!fileInfos.contains(info)) {
                fileInfos.add(info);
                info.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent event) {
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                FileInfo info = (FileInfo) event.getSource();
                                int index = getIndex(info);
                                if (index != -1) {
                                    fireTableRowsUpdated(index, index);
                                    fireTableCellUpdated(index, MD5_COLUMN);
                                }
                            }
                        });
                    }
                });
                added++;
            }
        }
        if (added > 0) {
            fireTableDataChanged();
            this.pcs.firePropertyChange("files", oldFileInfos, fileInfos);
        }
    }

    public void clearUploaded() {
        final List<FileInfo> oldFileInfos = new ArrayList<FileInfo>(fileInfos);

        Iterator<FileInfo> it = fileInfos.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            FileInfo file = it.next();
            if (file.isCompleted()) {
                it.remove();
                changed = true;
            }
        }

        if (changed) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fireTableDataChanged();
                }
            });
            this.pcs.firePropertyChange("files", oldFileInfos, fileInfos);
        }
    }

    public void removeRows(int[] rows) {
        if (rows.length > 0) {
            final List<FileInfo> oldFileInfos = new ArrayList<FileInfo>(
                    fileInfos);

            List<FileInfo> toRemove = getFileInfosAt(rows);

            fileInfos.removeAll(toRemove);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fireTableDataChanged();
                }
            });
            this.pcs.firePropertyChange("files", oldFileInfos, fileInfos);
        }
    }

    public Object getValueAt(int row, int col) {
        FileInfo info = fileInfos.get(row);
        switch (col) {
        case NAME_COLUMN:
            return info.getName();
        case BYTE_SIZE:
            return info.getByteSize();
        case MD5_COLUMN:
            return info.getMd5();
        case PROGRESS_COLUMN:
            return info;
        }
        throw new RuntimeException("internal error " + row + "/" + col);
    }

    /*
     * JTable uses this method to determine the default renderer/ editor for
     * each cell. If we didn't implement this method, then the last column would
     * contain text ("true"/"false"), rather than a check box.
     */
    public Class<?> getColumnClass(int c) {
        return String.class;
    }

    /*
     * Don't need to implement this method unless your table's editable.
     */
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    public UploadCallbackStatus uploadStatus(final TransferEvent event) {
        updateProgressbar(event);
        return UploadCallbackStatus.CONTINUE_UPLOAD;
    }

    public void uploadFailed(final TransferException error) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FileInfo info = lookup(error.getFileInfo());
                info.setFailed();
                int index = getIndex(info);
                fireTableRowsUpdated(index, index);
                fireTableCellUpdated(index, PROGRESS_COLUMN);
                pcs.firePropertyChange("files", fileInfos, fileInfos);
            }
        });
    }

    public void allTransferCompleted() {
        pcs.firePropertyChange("files", fileInfos, fileInfos);
    }

    public void uploadCancelled(final TransferEvent event) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FileInfo info = lookup(event.getFileInfo());
                info.setCancelled();
                int index = getIndex(info);
                fireTableRowsUpdated(index, index);
                fireTableCellUpdated(index, PROGRESS_COLUMN);
                pcs.firePropertyChange("files", fileInfos, fileInfos);
            }
        });
    }

    public void uploadCompleted(TransferEvent event) {
        updateProgressbar(event);
    }

    public void alreadyUploaded(final TransferEvent event) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FileInfo info = lookup(event.getFileInfo());
                info.setAlreadyCompleted();
                int index = getIndex(info);
                fireTableRowsUpdated(index, index);
                fireTableCellUpdated(index, PROGRESS_COLUMN);
                pcs.firePropertyChange("files", fileInfos, fileInfos);
            }
        });
    }

    private void updateProgressbar(final TransferEvent event) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FileInfo info = lookup(event.getFileInfo());
                info.setBytesTransferred(event.getFileInfo()
                        .getBytesTransferred());

                int index = getIndex(info);
                fireTableRowsUpdated(index, index);
                fireTableCellUpdated(index, PROGRESS_COLUMN);
                // fireTableDataChanged();
                if (event.getFileInfo().isCompleted()) {
                    info.setCompleted();
                    pcs.firePropertyChange("files", fileInfos, fileInfos);
                }
            }
        });
    }

    private FileInfo lookup(FileInfo fileInfo) {
        for (FileInfo info : fileInfos) {
            if (fileInfo.equals(info)) {
                return info;
            }
        }
        for (FileInfo info : fileInfos) {
            if (fileInfo.getName().equals(info.getName())) {
                return info;
            }
        }
        throw new RuntimeException("Failed to find file for " + fileInfo
                + " in \n" + fileInfos);
    }

    private int getIndex(FileInfo info) {
        for (int i = 0; i < fileInfos.size(); i++) {
            if (info == fileInfos.get(i)) {
                return i;
            }
        }
        return -1;
    }

}
