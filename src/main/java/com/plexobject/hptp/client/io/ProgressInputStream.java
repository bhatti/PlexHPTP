package com.plexobject.hptp.client.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferException;
import com.plexobject.hptp.domain.TransferListener;

public class ProgressInputStream extends BufferedInputStream {
    private static final Logger LOGGER = Logger
            .getLogger(ProgressInputStream.class);

    private static final int BUFFER_SIZE = Configuration.getInstance()
            .getFileBufferSize();
    private final FileInfo fileInfo;
    private final TransferListener listener;

    private long transferred;
    private boolean cancelled;

    public ProgressInputStream(final FileInfo fileInfo, long offset,
            final TransferListener listener) throws IOException {
        super(new FileInputStream(fileInfo.getFile()));
        this.fileInfo = fileInfo;
        this.listener = listener;
        fileInfo.setTransferStarted(new Date());
        if (offset > 0) {
            super.skip(offset);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            if (cancelled) {
                return -1;
            }
            int count = super.read(b, off, len);
            if (count > 0) {
                transferred += count;
                fileInfo.setBytesTransferred(transferred);

                if (listener != null) {
                    if (listener
                            .uploadStatus(new TransferEvent(this, fileInfo)) == TransferListener.UploadCallbackStatus.CANCEL_UPLOAD) {
                        cancelled = true;
                        fileInfo.setCancelled();
                        listener.uploadCancelled(new TransferEvent(this,
                                fileInfo));

                        LOGGER.info("Cancelling upload for " + fileInfo);
                        return -1;
                    }
                }
            } else {
                fileInfo.setCompleted();
                if (listener != null && !cancelled) {
                    listener.uploadCompleted(new TransferEvent(this, fileInfo));
                }
            }
            return count;
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

    @Override
    public int read() throws IOException {
        try {
            if (cancelled) {
                return -1;
            }
            int c = super.read();
            if (c != -1) {
                transferred++;
                fileInfo.setBytesTransferred(transferred);
                if (transferred % BUFFER_SIZE == 0) {
                    if (listener != null) {
                        if (listener.uploadStatus(new TransferEvent(this,
                                fileInfo)) == TransferListener.UploadCallbackStatus.CANCEL_UPLOAD) {
                            cancelled = true;
                            listener.uploadCancelled(new TransferEvent(this,
                                    fileInfo));

                            LOGGER.info("Cancelling upload for " + fileInfo);
                            return -1;
                        }
                    }
                }
            } else {
                fileInfo.setCompleted();
                if (listener != null && !cancelled) {
                    listener.uploadCompleted(new TransferEvent(this, fileInfo));
                }
            }
            return c;
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
