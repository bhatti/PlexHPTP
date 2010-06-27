package com.plexobject.hptp.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.metrics.Metric;
import com.plexobject.hptp.metrics.Timer;

public class IOUtils {
    private static final int BUFFER_SIZE = Configuration.getInstance()
            .getFileBufferSize();

    public static void copy(File src, File dst, long offset) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(src));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));
        copy(dst, in, out, offset);
    }

    public static void copy(File file, OutputStream out) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                file));
        copy(file, in, out, 0);
    }

    public static void copy(File file, InputStream in, OutputStream out,
            long offset) throws IOException {
        final Timer timer = Metric.newTimer(IOUtils.class.getSimpleName()
                + ".copy");
        try {
            in.skip(offset);
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        } finally {
            timer.stop();
        }
    }

    public static void appendCopy(File src, File tgt)
            throws FileNotFoundException, IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                src));
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(tgt, true));
        copy(src, in, out, 0);
    }

    public static void appendCopy(InputStream in, File file) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(file, true));
        copy(file, in, out, 0);
    }

    public static void copy(InputStream in, File file) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(file));
        copy(file, in, out, 0);
    }
}
