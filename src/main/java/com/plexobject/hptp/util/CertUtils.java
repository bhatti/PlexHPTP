package com.plexobject.hptp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.plexobject.hptp.domain.Configuration;

public class CertUtils {
    private static final Logger LOGGER = Logger.getLogger(CertUtils.class);
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    private static final boolean checkCertTrusted = Configuration.getInstance()
            .getBoolean("check.cert.trusted");
    private static final String CERT_NAME = Configuration.getInstance()
            .getProperty("javax.net.ssl.trustStore", "cacerts");
    private static final String CERT_PASSWORD = Configuration.getInstance()
            .getProperty("javax.net.ssl.trustStorePassword", "changeit");
    private static final File CA_CERT = new File(System
            .getProperty("java.home")
            + File.separator + "lib" + File.separator + "security", "cacerts");

    public static boolean installCert(String hostName)
            throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        return installCert(hostName, 443);
    }

    public static boolean installCert(String hostName, int port)
            throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        File dir = new File(System.getProperty("java.home") + File.separator
                + "lib" + File.separator + "security");
        if (!dir.canWrite()) {
            dir = new File(System.getProperty("user.dir"));
        }
        File certFile = new File(dir, CERT_NAME);
        return installCert(hostName, port, certFile, CERT_PASSWORD, false);
    }

    public static boolean installCert(String hostName, int port, File certFile,
            String certPassword, boolean force) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException,
            KeyManagementException {
        hostName = hostName.toLowerCase();
        KeyStore ks = getKeyStore(certFile, certPassword);
        if (aliasExist(hostName, ks)) {
            if (verifyHost(hostName, port)) {
                return false;
            } else {
                certFile.delete();
                ks = getKeyStore(certFile, certPassword);
            }
            return false;
        }

        X509Certificate[] chain = getCertificates(hostName, port, ks);
        if (chain == null) {
            LOGGER.debug("Could not obtain server certificate chain");
            return false;
        }

        addCertificate(hostName, ks, chain);
        saveKeyStore(certFile, certPassword, ks);
        return true;
    }

    private static boolean verifyHost(String hostName, int port) {
        try {
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
                    .getDefault();
            SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(
                    hostName, port);

            InputStream in = sslsocket.getInputStream();
            OutputStream out = sslsocket.getOutputStream();

            out.write(1);

            while (in.available() > 0) {
                System.out.print(in.read());
            }
            in.close();
            out.close();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static void saveKeyStore(File certFile, String certPassword,
            KeyStore ks) throws FileNotFoundException, KeyStoreException,
            IOException, NoSuchAlgorithmException, CertificateException {
        OutputStream out = new FileOutputStream(certFile);
        ks.store(out, certPassword.toCharArray());
        out.close();
    }

    private static void addCertificate(String hostName, KeyStore ks,
            X509Certificate[] chain) throws NoSuchAlgorithmException,
            CertificateEncodingException, KeyStoreException {
        LOGGER.debug("Server sent " + chain.length + " certificate(s):");
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            LOGGER.debug(" " + (i + 1) + " Subject " + cert.getSubjectDN());
            LOGGER.debug("   Issuer  " + cert.getIssuerDN());
            sha1.update(cert.getEncoded());
            LOGGER.debug("   sha1    " + toHexString(sha1.digest()));
            md5.update(cert.getEncoded());
            LOGGER.debug("   md5     " + toHexString(md5.digest()));
        }

        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            String alias = hostName + "-" + (i + 1);
            ks.setCertificateEntry(alias, cert);
            LOGGER.debug(cert);
            LOGGER
                    .debug("Added certificate to keystore 'jssecacerts' using alias '"
                            + alias + "'");
        }
    }

    private static boolean aliasExist(String hostName, KeyStore ks)
            throws KeyStoreException {
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (alias.startsWith(hostName + "-")) {
                LOGGER.debug(hostName + " already exists");
                return true;
            }
        }
        return false;
    }

    private static X509Certificate[] getCertificates(String hostName, int port,
            KeyStore ks) throws NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, IOException, UnknownHostException,
            SocketException {
        SSLContext context = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        X509TrustManager defaultTrustManager = (X509TrustManager) tmf
                .getTrustManagers()[0];
        SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
        context.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory factory = context.getSocketFactory();

        LOGGER.debug("Opening connection to " + hostName + ":" + port + "...");
        SSLSocket socket = (SSLSocket) factory.createSocket(hostName, port);
        socket.setSoTimeout(10000);
        try {
            LOGGER.debug("Starting SSL handshake...");
            socket.startHandshake();
            socket.close();
            LOGGER.debug("No errors, certificate is already trusted");
        } catch (SSLException e) {
            LOGGER.error(e);
        }

        X509Certificate[] chain = tm.chain;
        return chain;
    }

    private static KeyStore getKeyStore(File certFile, String certPassword)
            throws IOException, KeyStoreException, FileNotFoundException,
            NoSuchAlgorithmException, CertificateException {
        if (!certFile.exists()) {
            FileUtils.copyFile(CA_CERT, certFile);
        }

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        LOGGER.debug("Loading KeyStore " + certFile + "...");
        InputStream in = new FileInputStream(certFile);
        ks.load(in, certPassword.toCharArray());
        in.close();
        return ks;
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
            sb.append(' ');
        }
        return sb.toString();
    }

    private static class SavingTrustManager implements X509TrustManager {
        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            if (checkCertTrusted) {
                tm.checkServerTrusted(chain, authType);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            installCert("s3.amazonaws.com");
        } else {
            installCert(args[0]);
        }
    }
}
