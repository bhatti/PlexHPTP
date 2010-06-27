package com.plexobject.hptp.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.plexobject.hptp.client.TransferClient;
import com.plexobject.hptp.client.TransferManager;
import com.plexobject.hptp.client.TransferManagerImpl;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.MessageHandler;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferListener;

public class TransferManagerImplTest {
    private TransferClient uploadClient;
    private TransferListener listener;
    private MessageHandler messageHandler;
    private static final String GROUP = "UploadManagerImplTest";

    private class TransferClientImpl implements TransferClient {
        public void delete(final String group, final String inputFile)
                throws IOException {
        }

        public void download(final String group, final String inputFile,
                final File outputFile, final TransferListener listener)
                throws IOException {
            for (int i = 0; i < 1000; i++) {
                if (listener.uploadStatus(new TransferEvent(this, new FileInfo(
                        outputFile))) == TransferListener.UploadCallbackStatus.CANCEL_UPLOAD) {
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }

        public FileInfo info(final String group, final String fileName)
                throws IOException {
            return null;
        }

        public FileInfo[] list(final String group) throws IOException {
            return null;
        }

        public FileInfo upload(final String group, final FileInfo fileInfo,
                TransferListener listener) throws IOException {
            for (int i = 0; i < 1000; i++) {
                if (listener.uploadStatus(new TransferEvent(this, fileInfo)) == TransferListener.UploadCallbackStatus.CANCEL_UPLOAD) {
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testCancel() throws IOException, InterruptedException {
        uploadClient = new TransferClientImpl();
        listener = EasyMock.createMock(TransferListener.class);

        messageHandler = EasyMock.createNiceMock(MessageHandler.class);
        Collection<FileInfo> files = TestUtils.createLocalFileInfos(3);

        final TransferManager uploadManager = new TransferManagerImpl(
                messageHandler, uploadClient);
        EasyMock.replay(messageHandler);

        int submitted = uploadManager.upload(GROUP, files, listener);
        assertEquals(3, submitted);
        int cancelled = uploadManager.cancel();
        assertEquals(3, cancelled);
        Thread.sleep(5000);
        int pending = uploadManager.getPendingTransfers().size();
        assertEquals("Pending " + uploadManager.getPendingTransfers(), 0,
                pending);
    }

    @Test
    public final void testUpload() throws IOException, InterruptedException {
        uploadClient = EasyMock.createMock(TransferClient.class);
        listener = EasyMock.createMock(TransferListener.class);
        messageHandler = EasyMock.createMock(MessageHandler.class);
        final String GROUP = "test_GROUP";
        Collection<FileInfo> files = TestUtils.createLocalFileInfos(10);

        final TransferManager uploadManager = new TransferManagerImpl(
                messageHandler, uploadClient);
        for (FileInfo file : files) {
            EasyMock.expect(
                    uploadClient.upload(EasyMock.eq(GROUP), EasyMock.eq(file),
                            EasyMock.isA(TransferListener.class))).andReturn(
                    file);
        }
        EasyMock.replay(uploadClient);
        EasyMock.replay(messageHandler);

        int submitted = uploadManager.upload(GROUP, files, listener);
        assertEquals(10, submitted);
        Thread.sleep(1000);

        int pending = uploadManager.getPendingTransfers().size();
        assertEquals("Pending " + uploadManager.getPendingTransfers(), 0,
                pending);

        EasyMock.verify(uploadClient);
        EasyMock.verify(messageHandler);
    }
}
