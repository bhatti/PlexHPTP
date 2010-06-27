package com.plexobject.hptp.client;

import java.util.Collection;

import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.TransferListener;

public interface TransferManager {
    /**
     * upload files
     * 
     * @param files
     * @param listener
     */
    int upload(String group, Collection<FileInfo> files,
            TransferListener listener);

    /**
     * cancels transfer
     * 
     * @return # of files cancelled
     */
    int cancel();

    /**
     * 
     * @return files waiting or in-progress
     */
    Collection<FileInfo> getPendingTransfers();
}
