package com.plexobject.hptp.service;

import java.io.File;
import java.io.IOException;

public class FileSizeExceededException extends IOException {
    private static final long serialVersionUID = 1L;
    private final File file;
    private final int maxSize;

    public FileSizeExceededException(final String msg, final File file,
            final int maxSize) {
        super(msg);
        this.file = file;
        this.maxSize = maxSize;
    }

    public File getFile() {
        return file;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
