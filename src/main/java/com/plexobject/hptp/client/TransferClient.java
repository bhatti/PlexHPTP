package com.plexobject.hptp.client;

import java.io.File;
import java.io.IOException;

import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.TransferListener;

public interface TransferClient {
    /**
     * list files uploaded by the group
     * 
     * @param group
     * @return
     * @throws IOException
     */
    FileInfo[] list(String group) throws IOException;

    /**
     * 
     * @param group
     * @param fileName
     * @return
     * @throws IOException
     */
    FileInfo info(String group, String fileName) throws IOException;

    /**
     * upload given file for the group
     * 
     * @param group
     * @param fileInfo
     * @param listener
     *            for updates
     * @return FileInfo just created
     * @throws IOException
     * @throws HttpException
     */
    FileInfo upload(String group, FileInfo fileInfo, TransferListener listener)
            throws IOException;

    /**
     * download given file for the group and saves it locally
     * 
     * @param group
     * @param inputFile
     * @param outputFile
     * @param listener
     *            for updates
     * @throws IOException
     * @throws HttpException
     */
    void download(String group, String inputFile, File outputFile,
            TransferListener listener) throws IOException;

    /**
     * delete file
     * 
     * @param group
     * @param inputFile
     * @throws IOException
     */
    void delete(String group, String inputFile) throws IOException;
}
