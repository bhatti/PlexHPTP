package com.plexobject.hptp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.Pair;
import com.plexobject.hptp.util.Md5Utils;

public abstract class BaseTransferClientTestCase {
    protected static final Logger LOGGER = Logger
            .getLogger(BaseTransferClientTestCase.class);
    protected TransferClient uploadClient;
    protected final String groupName = getClass().getSimpleName();

    @Test
    public final void testUpload() throws IOException {
        Pair<File, FileInfo> pair = upload();
        FileInfo localFileInfo = new FileInfo(pair.first);
        assertEquals(localFileInfo.getByteSize(), pair.second.getByteSize());
        assertEquals(pair.first.length(), pair.second.getBytesTransferred());
        assertEquals(localFileInfo.getMd5(), pair.second.getMd5());
        FileInfo savedFileInfo = uploadClient.info(groupName,
                pair.second.getName());
        assertFileInfo(pair.first, localFileInfo.getMd5(), savedFileInfo);
    }

    @Test
    public final void testUploadTwice() throws IOException {
        Pair<File, FileInfo> pair = upload();
        FileInfo localFileInfo = new FileInfo(pair.first);
        assertEquals(localFileInfo.getByteSize(), pair.second.getByteSize());
        assertEquals(pair.first.length(), pair.second.getBytesTransferred());
        assertEquals(localFileInfo.getMd5(), pair.second.getMd5());

        pair = upload(pair.first);
        localFileInfo = new FileInfo(pair.first);
        assertEquals(localFileInfo.getByteSize(), pair.second.getByteSize());
        assertEquals(0, pair.second.getBytesTransferred());
        assertEquals(localFileInfo.getMd5(), pair.second.getMd5());
    }

    @Test
    public final void testDownload() throws IOException {
        Pair<File, FileInfo> pair = upload();
        String md5 = Md5Utils.calculateMd5(pair.first);
        File downloadedFile = download(pair.first.getName());
        assertEquals(md5, Md5Utils.calculateMd5(downloadedFile));
    }

    @Test
    public final void testDelete() throws IOException {
        Pair<File, FileInfo> pair = upload();
        FileInfo savedFileInfo = uploadClient.info(groupName,
                pair.second.getName());
        assertNotNull(savedFileInfo);
        uploadClient.delete(groupName, pair.first.getName());
        try {
            savedFileInfo = uploadClient.info(groupName, pair.second.getName());
            fail("delete failed");
        } catch (FileNotFoundException e) {
        }
    }

    private void assertFileInfo(File file, String md5, FileInfo info) {
        assertNotNull(info);
        assertEquals(file.length(), info.getByteSize());
        assertEquals(md5, info.getMd5());

    }

    protected abstract String toFileName(String group, String name);

    protected Pair<File, FileInfo> upload() throws IOException {
        File file = TestUtils.createLocalFile();
        return upload(file);
    }

    protected Pair<File, FileInfo> upload(File file) throws IOException {
        FileInfo info = uploadClient
                .upload(groupName, new FileInfo(file), null);
        LOGGER.info("    UPLOADED " + file + " to " + info);
        return new Pair<File, FileInfo>(file, info);
    }

    private File download(String fileName) throws IOException {
        File file = File.createTempFile("plexhptp", "download");
        file.deleteOnExit();
        uploadClient.download(groupName, fileName, file, null);
        return file;
    }
}
