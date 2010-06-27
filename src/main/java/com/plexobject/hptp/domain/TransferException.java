package com.plexobject.hptp.domain;

public class TransferException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final FileInfo fileInfo;

    public TransferException(final FileInfo fileInfo, Exception cause) {
        super("Failed to upload " + fileInfo + " (" + cause + ")", cause);
        this.fileInfo = fileInfo;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }
}
