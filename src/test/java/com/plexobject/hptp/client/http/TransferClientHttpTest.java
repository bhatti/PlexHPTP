package com.plexobject.hptp.client.http;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.plexobject.hptp.client.BaseTransferClientTestCase;
import com.plexobject.hptp.client.TestUtils;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.Pair;
import com.plexobject.hptp.service.FileServer;

public class TransferClientHttpTest extends BaseTransferClientTestCase {
    private FileServer server = FileServer.getInstance();

    @Before
    public void setUp() throws Exception {
        uploadClient = new TransferClientHttp();
        if (!server.isRunning()) {
            server.start();
        }
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public final void testPartialUpload() throws IOException {
        Pair<File, FileInfo> pair = upload();
        FileInfo localFileInfo = new FileInfo(pair.first);
        assertEquals(localFileInfo.getByteSize(), pair.second.getByteSize());
        assertEquals(pair.first.length(), pair.second.getBytesTransferred());
        assertEquals(localFileInfo.getMd5(), pair.second.getMd5());
        long initialLength = pair.first.length();
        TestUtils.addRandomData(pair.first, true);
        long newLength = pair.first.length() - initialLength;
        pair = upload(pair.first);
        localFileInfo = new FileInfo(pair.first);
        assertEquals(localFileInfo.getByteSize(), pair.second.getByteSize());
        assertEquals(newLength, pair.second.getBytesTransferred());
        assertEquals(localFileInfo.getMd5(), pair.second.getMd5());

        uploadClient.delete(groupName, pair.first.getName());
    }

    @Test
    public final void testUploadTwiceWithChangedData() throws IOException {
        Pair<File, FileInfo> pair = upload();
        FileInfo localFileInfo = new FileInfo(pair.first);
        assertEquals(localFileInfo.getByteSize(), pair.second.getByteSize());
        assertEquals(pair.first.length(), pair.second.getBytesTransferred());
        assertEquals(localFileInfo.getMd5(), pair.second.getMd5());
        TestUtils.addRandomData(pair.first, false);
        pair = upload(pair.first);
        localFileInfo = new FileInfo(pair.first);
        assertEquals(localFileInfo.getByteSize(), pair.second.getByteSize());
        assertEquals(localFileInfo.getByteSize(),
                pair.second.getBytesTransferred());
        assertEquals(localFileInfo.getMd5(), pair.second.getMd5());

        uploadClient.delete(groupName, pair.first.getName());
    }

    @Override
    protected String toFileName(String group, String name) {
        return name;
    }
}
