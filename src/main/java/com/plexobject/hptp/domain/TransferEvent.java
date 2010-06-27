package com.plexobject.hptp.domain;

import java.util.EventObject;

public class TransferEvent extends EventObject {
    private static final long serialVersionUID = 1L;
    private final FileInfo fileInfo;

    public TransferEvent(final Object source, final FileInfo fileInfo) {
        super(source);
        this.fileInfo = fileInfo;
    }

    /**
     * @return the file
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String toString() {
        return fileInfo.toString();
    }

}
