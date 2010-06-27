package com.plexobject.hptp.domain;

import java.util.EventListener;

public interface TransferListener extends EventListener {
    enum UploadCallbackStatus {
        CONTINUE_UPLOAD, CANCEL_UPLOAD
    }

    /**
     * updates transfer event
     * 
     * @param event
     * @return CONTINUE_UPLOAD to continue the upload process, CANCEL_UPLOAD to
     *         cancel it
     */
    UploadCallbackStatus uploadStatus(TransferEvent event);

    /**
     * 
     * @param event
     */
    void uploadCompleted(TransferEvent event);

    /**
     * 
     * @param oldInfo
     */
    void alreadyUploaded(TransferEvent event);

    /**
     * notifies that upload is cancelleds
     * 
     * @param cancel
     */
    void uploadCancelled(TransferEvent event);

    /**
     * notifies that transfer is failed
     * 
     * @param error
     */
    void uploadFailed(TransferException error);

    /**
     * notifies that all transfer is completed
     */
    void allTransferCompleted();
}
