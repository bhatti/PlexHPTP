package com.plexobject.hptp.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.validator.GenericValidator;
import org.apache.log4j.Logger;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.MessageHandler;
import com.plexobject.hptp.domain.Pair;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferException;
import com.plexobject.hptp.domain.TransferListener;
import com.plexobject.hptp.domain.TransferStatus;

public class TransferManagerImpl implements TransferManager {
    private static final Logger LOGGER = Logger
            .getLogger(TransferManagerImpl.class);

    private final static int MAX_THREADS = Configuration.getInstance()
            .getInteger("upload.threads", 3);
    private final MessageHandler messageHandler;
    private final static ExecutorService executorService = Executors
            .newFixedThreadPool(MAX_THREADS);
    private Map<FileInfo, Pair<Uploader, FutureTask<FileInfo>>> waitingOrInprogressTransfers = Collections
            .synchronizedMap(new HashMap<FileInfo, Pair<Uploader, FutureTask<FileInfo>>>());
    private final TransferClient uploadClient;

    final class Uploader implements Callable<FileInfo> {
        private TransferStatus status = TransferStatus.WAITING;
        private final String group;
        private final FileInfo fileInfo;
        private final TransferListener listener;

        Uploader(final String group, final FileInfo fileInfo,
                final TransferListener listener) {
            this.group = group;
            this.fileInfo = fileInfo;
            this.listener = listener;
        }

        boolean isFailedOrIncomplete() {
            return status == TransferStatus.CANCELLED
                    || status == TransferStatus.FAILED;
        }

        boolean cancel() {
            if (status == TransferStatus.WAITING
                    || status == TransferStatus.INPROGRESS) {
                status = TransferStatus.CANCELLED;
                return true;
            } else {
                return false;
            }
        }

        public FileInfo call() {
            status = TransferStatus.INPROGRESS;
            try {
                uploadClient.upload(group, fileInfo, transferListenerAdapter());
            } catch (Exception e) {
                TransferException xfrException = new TransferException(
                        fileInfo, e);
                listener.uploadFailed(xfrException);
            } catch (Throwable e) {
                LOGGER.error("Failed to transfer " + fileInfo, e);
                TransferException xfrException = new TransferException(
                        fileInfo, new RuntimeException(e));
                listener.uploadFailed(xfrException);
            } finally {
                removeFileInfo();
            }
            return fileInfo;
        }

        private void removeFileInfo() {
            synchronized (TransferManagerImpl.this) {
                Object found = waitingOrInprogressTransfers.remove(fileInfo);
                if (found == null) {
                    LOGGER.warn("####Failed to remove " + fileInfo
                            + ", remaining files "
                            + waitingOrInprogressTransfers.size());
                }
                if (waitingOrInprogressTransfers.size() == 0) {
                    listener.allTransferCompleted();
                }
            }
        }

        private TransferListener transferListenerAdapter() {
            return new TransferListener() {
                public UploadCallbackStatus uploadStatus(
                        final TransferEvent event) {
                    listener.uploadStatus(event);
                    if (status == TransferStatus.CANCELLED) {
                        return UploadCallbackStatus.CANCEL_UPLOAD;
                    } else {
                        return UploadCallbackStatus.CONTINUE_UPLOAD;
                    }
                }

                public void uploadFailed(final TransferException error) {
                    listener.uploadFailed(error);
                }

                public void allTransferCompleted() {
                    listener.allTransferCompleted();
                }

                public void uploadCancelled(final TransferEvent event) {
                    listener.uploadCancelled(event);
                }

                public void uploadCompleted(final TransferEvent event) {
                    listener.uploadCompleted(event);
                }

                public void alreadyUploaded(final TransferEvent event) {
                    listener.alreadyUploaded(event);
                }
            };
        }

        public String toString() {
            return fileInfo + " status " + status;
        }
    }

    public TransferManagerImpl(final MessageHandler messageHandler,
            final TransferClient uploadClient) {
        this.messageHandler = messageHandler;
        this.uploadClient = uploadClient;
    }

    public synchronized int cancel() {
        int cancelled = 0;

        for (Map.Entry<FileInfo, Pair<Uploader, FutureTask<FileInfo>>> e : waitingOrInprogressTransfers
                .entrySet()) {
            if (e.getValue().first.cancel() && e.getValue().second.cancel(true)) {
                cancelled++;
            }
        }
        return cancelled;
    }

    public synchronized int upload(final String group,
            final Collection<FileInfo> files, final TransferListener listener) {
        if (GenericValidator.isBlankOrNull(group)) {
            throw new IllegalArgumentException("group not specified");
        }
        if (files == null) {
            throw new IllegalArgumentException("files not specified");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener  not specified");
        }
        int submitted = 0;
        for (FileInfo file : files) {
            Pair<Uploader, FutureTask<FileInfo>> pair = waitingOrInprogressTransfers
                    .get(file);
            if (pair == null || pair.first.isFailedOrIncomplete()) {
                Uploader uploader = new Uploader(group, file, listener);
                FutureTask<FileInfo> future = new FutureTask<FileInfo>(uploader);
                waitingOrInprogressTransfers.put(file,
                        new Pair<Uploader, FutureTask<FileInfo>>(uploader,
                                future));
                executorService.submit(future);
                submitted++;
                if (LOGGER.isDebugEnabled()) {
                    messageHandler
                            .handleMessage("Submitting to upload " + file);
                }
            }
        }
        return submitted;
    }

    public Collection<FileInfo> getPendingTransfers() {
        return waitingOrInprogressTransfers.keySet();
    }

}
