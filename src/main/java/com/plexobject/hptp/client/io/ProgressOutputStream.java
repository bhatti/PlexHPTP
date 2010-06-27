package com.plexobject.hptp.client.io;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferException;
import com.plexobject.hptp.domain.TransferListener;

public class ProgressOutputStream extends BufferedOutputStream {
    private static final Logger LOGGER = Logger
            .getLogger(ProgressInputStream.class);

    private static final int BUFFER_SIZE = Configuration.getInstance()
            .getFileBufferSize();
    private final FileInfo fileInfo;
    private final TransferListener listener;

    private long transferred;
    private boolean cancelled;

    public ProgressOutputStream(final FileInfo fileInfo,
            final TransferListener listener) throws IOException {
        super(new FileOutputStream(fileInfo.getFile()));
        this.fileInfo = fileInfo;
        this.listener = listener;
        this.fileInfo.setTransferStarted(new Date());
    }

    /**
     * @return the cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * @param cancelled
     *            the cancelled to set
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public void write(int c) throws IOException {
        try {
            if (cancelled) {
                if (listener != null) {
                    listener.uploadCancelled(new TransferEvent(this, fileInfo));
                }
                LOGGER.info("Cancelling download for " + fileInfo);
                throw new IOException("Cancelled");
            }
            write(c);
            transferred++;
            fileInfo.setBytesTransferred(transferred);
            if (transferred % BUFFER_SIZE == 0) {
                if (listener != null) {
                    if (listener
                            .uploadStatus(new TransferEvent(this, fileInfo)) == TransferListener.UploadCallbackStatus.CANCEL_UPLOAD) {
                        fileInfo.setCancelled();
                        cancelled = true;
                    }
                }
            }
            if (transferred == fileInfo.getByteSize() && listener != null) {
                fileInfo.setCompleted();
                listener.uploadCompleted(new TransferEvent(this, fileInfo));
            }
        } catch (IOException e) {
            fileInfo.setFailed();

            if (listener != null) {
                TransferException xfrException = new TransferException(
                        fileInfo, e);
                listener.uploadFailed(xfrException);
            }
            throw e;
        }
    }
}
