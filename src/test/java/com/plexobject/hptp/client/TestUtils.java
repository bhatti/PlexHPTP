package com.plexobject.hptp.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import com.plexobject.hptp.domain.FileInfo;

public class TestUtils {
    public static Collection<FileInfo> createLocalFileInfos(int max)
            throws IOException {
        Collection<FileInfo> files = new ArrayList<FileInfo>();
        for (int i = 0; i < max; i++) {
            files.add(new FileInfo(TestUtils.createLocalFile()));
        }
        return files;
    }

    public static File createLocalFile() throws IOException {
        File file = File.createTempFile("plexhptp", "upload");
        file.deleteOnExit();
        addRandomData(file, false);
        return file;
    }

    public static void addRandomData(File file, boolean append)
            throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(file, append));
        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 100; j++) {
                out.write(rand.nextInt());
            }
            out.write('\n');
        }
        out.close();
    }
}
