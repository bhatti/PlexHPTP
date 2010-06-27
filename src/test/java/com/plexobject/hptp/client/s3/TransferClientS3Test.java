package com.plexobject.hptp.client.s3;

import org.junit.After;
import org.junit.Before;

import com.plexobject.hptp.client.BaseTransferClientTestCase;
import com.plexobject.hptp.domain.FileInfo;

public class TransferClientS3Test extends BaseTransferClientTestCase {
    @Before
    public void setUp() throws Exception {
        uploadClient = new TransferClientS3();
    }

    @After
    public void tearDown() throws Exception {
        for (FileInfo file : uploadClient.list(groupName)) {
            uploadClient.delete(groupName, file.getName());
        }
    }

    @Override
    protected String toFileName(String group, String name) {
        return group + "__" + name;
    }
}
