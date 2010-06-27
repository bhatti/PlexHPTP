package com.plexobject.hptp.domain;

import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.ServletConfig;

import org.apache.commons.validator.GenericValidator;
import org.apache.log4j.Logger;

public class Configuration {
    private static final Logger LOGGER = Logger.getLogger(Configuration.class);
    private static final File TEMP_DIR = new File(
            System.getProperty("java.io.tmpdir"));
    private static final String RESOURCE_NAME = "application.properties";
    private static final String GROUP_NAME = "group";
    private static final String USER_NAME = "user.name";
    private static final String DIR_NAME = "dir.name";
    private static final String MAX_FILE_SIZE = "max.file.size";
    private static final String MAX_FILES = "max.files";
    private static final int DEFAULT_MAX_FILES = 100;
    private static final String MD5_SUFFIX = "md5.suffix";
    private static final String DEFAULT_MD5_SUFFIX = ".md5";
    private static final int DEFAULT_MAX_SIZE = 1024 * 1000 * 100; // 100 MB
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final String SERVER_PORT = "server.port";
    private static final String AWS_ACCESS_KEY = "accessKey";
    private static final String AWS_SECRET_KEY = "secretKey";
    private static final String S3_BUCKET = "bucket";
    private static final String DISABLE_CERT_VERIFICATION = "disable.cert.verification";
    private static final boolean DEFAULT_DISABLE_CERT_VERIFICATION = true;
    private static final String FILE_BUFFER_SIZE = "file.buffer.size";
    private static final int DEFAULT_FILE_BUFFER_SIZE = 1024; // TODO 8192

    private static Configuration instance = new Configuration();
    private final Properties properties = new Properties();
    private Locale locale;

    Configuration() {
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream(
                    RESOURCE_NAME);
            properties.load(in);

            properties.putAll(System.getProperties());

        } catch (IOException e) {
            LOGGER.error("Failed to load " + RESOURCE_NAME, e);
        }
        properties.putAll(System.getProperties());
    }

    public static Configuration getInstance() {
        return instance;
    }

    public String getProperty(final String key) {
        return getProperty(key, null);
    }

    public String getProperty(final String key, final String def) {
        return properties.getProperty(key, def);
    }

    public int getInteger(final String key) {
        return getInteger(key, 0);
    }

    public int getInteger(final String key, final int def) {
        return Integer.parseInt(getProperty(key, String.valueOf(def)));
    }

    public double getDouble(final String key) {
        return getDouble(key, 0);
    }

    public double getDouble(final String key, final double def) {
        return Double.valueOf(getProperty(key, String.valueOf(def)))
                .doubleValue();
    }

    public boolean getBoolean(final String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(final String key, final boolean def) {
        return Boolean.valueOf(getProperty(key, String.valueOf(def)))
                .booleanValue();
    }

    public long getLong(final String key) {
        return getLong(key, 0);
    }

    public long getLong(final String key, long def) {
        return Long.valueOf(getProperty(key, String.valueOf(def)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Enumeration<?> e = properties.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = properties.getProperty(name);
            sb.append(name + "=" + value + "\n");
        }
        return sb.toString();
    }

    public void setDirectory(String dir) {
        if (dir != null) {
            properties.setProperty(DIR_NAME, dir);
        }
    }

    public String getDirectory() {
        return properties.getProperty(DIR_NAME, System.getProperty(USER_NAME));
    }

    public String getGroup() {
        String group = properties.getProperty(GROUP_NAME,
                System.getProperty(USER_NAME));
        if (GenericValidator.isBlankOrNull(group)
                || group.equalsIgnoreCase("null")) {
            group = "group" + System.currentTimeMillis();
        }
        return group.replaceAll("@.*", "");
    }

    public File getUploadDir() {
        String dir = properties.getProperty(DIR_NAME);
        if (dir != null) {
            return new File(dir);
        } else {
            return Configuration.getTempFile("plexhptp", "upload");
        }
    }

    public int getMaxFileSize() {
        return getInteger(MAX_FILE_SIZE, DEFAULT_MAX_SIZE);
    }

    public int getMaxFiles() {
        return getInteger(MAX_FILES, DEFAULT_MAX_FILES);
    }

    public int getFileBufferSize() {
        return getInteger(FILE_BUFFER_SIZE, DEFAULT_FILE_BUFFER_SIZE);
    }

    public void update(ServletConfig config) {
        Enumeration<?> e = config.getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = config.getInitParameter(name);
            properties.setProperty(name, value);
        }
    }

    public void update(Applet applet) {
        for (String[] p : applet.getParameterInfo()) {
            String value = applet.getParameter(p[0]);
            if (value != null) {
                properties.setProperty(p[0], value);
                LOGGER.info("Adding " + p[0] + ": " + value + "----- "
                        + getGroup());
            }
        }
    }

    public boolean isDisableCertVerification() {
        return getBoolean(DISABLE_CERT_VERIFICATION,
                DEFAULT_DISABLE_CERT_VERIFICATION);
    }

    public int getServerPort() {
        return getInteger(SERVER_PORT, DEFAULT_SERVER_PORT);

    }

    public String getAmazonAccessKey() {
        return properties.getProperty(AWS_ACCESS_KEY);
    }

    public String getAmazonSecretKey() {
        return properties.getProperty(AWS_SECRET_KEY);
    }

    public String getS3BucketName() {
        return properties.getProperty(S3_BUCKET);
    }

    public static File getTempFile(final String group, String name) {
        File dir = new File(TEMP_DIR, group);
        dir.mkdirs();
        File file = new File(dir, name);
        file.deleteOnExit();
        return file;
    }

    public String getMd5Suffix() {
        return properties.getProperty(MD5_SUFFIX, DEFAULT_MD5_SUFFIX);
    }

    public Locale getDefaultLocale() {
        if (locale == null) {
            String country = getProperty("locale.country2", "US"); // uppercase
            // two-letter
            // ISO-3166
            // code.
            String language = getProperty("locale.lang2", "en"); // lowercase
            // two-letter
            // ISO-639
            // code.
            locale = new Locale(language, country);
            Locale.setDefault(locale);
        }
        return locale;
    }
}
