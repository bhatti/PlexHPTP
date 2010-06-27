package com.plexobject.hptp.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;

public class Md5Utils {
    private static final Logger LOGGER = Logger.getLogger(Md5Utils.class);
    protected static final String MD5_SUFFIX = Configuration.getInstance()
            .getMd5Suffix();
    private final static int MAX_THREADS = Configuration.getInstance()
            .getInteger("md5.threads", 2);
    private final static ExecutorService executorService = Executors
            .newFixedThreadPool(MAX_THREADS);
    private static final long M5 = UnitUtils.M * 5;

    public static Future<FileInfo> calculateMd5Async(final FileInfo fileInfo) {
        Future<FileInfo> future = null;
        final File file = fileInfo.getFile();
        if (file.exists() && file.isFile()) {
            File md5File = getMd5File(file);
            if (file.length() <= M5
                    || (md5File.exists() && md5File.lastModified() > file
                            .lastModified())) {
                fileInfo.setMd5(getMd5(file));
            } else {
                future = executorService.submit(new Callable<FileInfo>() {
                    public FileInfo call() throws Exception {
                        fileInfo.setMd5(getMd5(file));
                        return fileInfo;
                    }
                });
            }
        }
        return future;
    }

    String s = "This is a test";

    public static String getMd5(File file) {
        String md5 = null;
        File md5File = getMd5File(file);
        if (md5File.exists() && md5File.lastModified() > file.lastModified()) {
            try {
                md5 = FileUtils.readFileToString(md5File).trim();
            } catch (NumberFormatException e) {
            } catch (IOException e) {
            }
        }
        if (md5 == null) {
            md5 = Md5Utils.calculateMd5(file);
            try {
                FileUtils.writeStringToFile(md5File, md5);
            } catch (IOException e) {
                LOGGER.error("Failed to save " + md5File, e);
            }
        }
        return md5;
    }

    private static File getMd5File(File file) {
        File md5File = new File(file.getAbsolutePath() + MD5_SUFFIX);
        return md5File;
    }

    public static String calculateMd5(File file) {
        return calculateMd5(file, file.length());
    }

    public static String calculateMd5(File file, long length) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");

            BufferedInputStream in = new BufferedInputStream(
                    new FileInputStream(file));
            int c;
            for (long i = 0; i < length && (c = in.read()) != -1; i++) {
                m.update((byte) c);
            }
            in.close();
            byte[] digest = m.digest();
            return toBase64(digest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate md5 for " + file, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate md5 for " + file, e);
        }
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1) {
                // Append leading zero.
                sb.append("0");
            } else if (hex.length() == 8) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.getDefault());
    }

    public static byte[] fromHex(String hexData) {
        byte[] result = new byte[(hexData.length() + 1) / 2];
        String hexNumber = null;
        int stringOffset = 0;
        int byteOffset = 0;
        while (stringOffset < hexData.length()) {
            hexNumber = hexData.substring(stringOffset, stringOffset + 2);
            stringOffset += 2;
            result[byteOffset++] = (byte) Integer.parseInt(hexNumber, 16);
        }
        return result;
    }

    public static String toBase64(byte[] data) {
        byte[] b64 = Base64.encodeBase64(data);
        return new String(b64);
    }

    /**
     * Converts a Base64-encoded string to the original byte data.
     * 
     * @param b64Data
     *            a Base64-encoded string to decode.
     * 
     * @return bytes decoded from a Base64 string.
     */
    public static byte[] fromBase64(String b64Data) {
        byte[] decoded;
        try {
            decoded = Base64.decodeBase64(b64Data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            decoded = Base64.decodeBase64(b64Data.getBytes());
        }
        return decoded;
    }
}
