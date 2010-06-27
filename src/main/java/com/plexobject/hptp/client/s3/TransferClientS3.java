package com.plexobject.hptp.client.s3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.validator.GenericValidator;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.plexobject.hptp.client.TransferClient;
import com.plexobject.hptp.client.http.DisabledTrustManager;
import com.plexobject.hptp.client.io.ProgressInputStream;
import com.plexobject.hptp.client.io.ProgressOutputStream;
import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferListener;
import com.plexobject.hptp.metrics.Metric;
import com.plexobject.hptp.metrics.Timer;
import com.plexobject.hptp.util.CertUtils;
import com.plexobject.hptp.util.IOUtils;
import com.plexobject.hptp.util.Md5Utils;

public class TransferClientS3 implements TransferClient {
    private static final Logger LOGGER = Logger.getLogger(TransferClientS3.class);

    private final String bucketName;
    private AmazonS3 s3;

    static {
        System.setProperty("javax.net.ssl.trustStore", "cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        try {
            CertUtils.installCert("s3.amazonaws.com");
        } catch (Exception e) {
            LOGGER.error("Failed to install cert", e);
        }
    }

    public TransferClientS3() {
        this(Configuration.getInstance().getS3BucketName());
    }

    public TransferClientS3(final String bucketName) {
        if (Configuration.getInstance().isDisableCertVerification()) {
            disableHostVerification();
        }

        this.s3 = new AmazonS3Client(new BasicAWSCredentials(Configuration
                .getInstance().getAmazonAccessKey(), Configuration
                .getInstance().getAmazonSecretKey()));
        this.bucketName = bucketName;
        createBucketIfNonExistant(bucketName);
    }

    public void delete(String group, String inputFile) throws IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".delete");
        try {
            final String key = toKey(group, inputFile);
            LOGGER.info("Deleting " + bucketName + "/" + key);
            s3.deleteObject(bucketName, key);
        } catch (AmazonServiceException e) {
            throw new IOException("Failed to delete " + inputFile, e);
        } catch (AmazonClientException e) {
            throw new IOException("Failed to delete " + inputFile, e);
        } finally {
            timer.stop();
        }
    }

    public void download(String group, String inputFile, File outputFile,
            TransferListener listener) throws IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".download");
        try {
            final String key = toKey(group, inputFile);
            LOGGER.info("Downloading " + bucketName + "/" + key);
            S3Object object = s3
                    .getObject(new GetObjectRequest(bucketName, key));
            InputStream in = new BufferedInputStream(object.getObjectContent());
            OutputStream out = new ProgressOutputStream(
                    new FileInfo(outputFile), listener);
            IOUtils.copy(outputFile, in, out, 0);
        } catch (AmazonServiceException e) {
            throw new IOException("Failed to save " + outputFile, e);
        } catch (AmazonClientException e) {
            throw new IOException("Failed to save " + outputFile, e);
        } finally {
            timer.stop();
        }
    }

    public FileInfo info(String group, String inputFile) throws IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".info");
        try {
            final String key = toKey(group, inputFile);
            LOGGER.info("Info " + bucketName + "/" + key);
            ObjectListing objectListing = s3
                    .listObjects(new ListObjectsRequest().withBucketName(
                            bucketName).withPrefix(key));

            for (S3ObjectSummary objectSummary : objectListing
                    .getObjectSummaries()) {
                FileInfo file = new FileInfo();
                file.setPath(objectSummary.getBucketName() + File.separator
                        + objectSummary.getKey());
                file.setByteSize(objectSummary.getSize());
                file.setLastModified(objectSummary.getLastModified());
                byte[] hash = Md5Utils.fromHex(objectSummary.getETag());
                file.setMd5(Md5Utils.toBase64(hash));
                return file;
            }
            throw new FileNotFoundException("Failed to find " + inputFile);
        } catch (AmazonServiceException e) {
            throw new IOException("Failed to list " + group, e);
        } catch (AmazonClientException e) {
            throw new IOException("Failed to list " + group, e);
        } finally {
            timer.stop();
        }
    }

    public FileInfo[] list(String group) throws IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".list");
        final String key = "UploadClientImplTest"; // toKey(group, "");

        try {
            LOGGER.info("List " + bucketName + "/" + key);

            ObjectListing objectListing = s3
                    .listObjects(new ListObjectsRequest().withBucketName(
                            bucketName).withPrefix(key));

            Collection<FileInfo> files = new ArrayList<FileInfo>();

            for (S3ObjectSummary objectSummary : objectListing
                    .getObjectSummaries()) {
                FileInfo file = new FileInfo();
                file.setPath(objectSummary.getBucketName() + File.separator
                        + objectSummary.getKey());
                file.setByteSize(objectSummary.getSize());
                file.setLastModified(objectSummary.getLastModified());
                byte[] hash = Md5Utils.fromHex(objectSummary.getETag());
                file.setMd5(Md5Utils.toBase64(hash));
                files.add(file);
            }
            return files.toArray(new FileInfo[files.size()]);
        } catch (AmazonServiceException e) {
            throw new IOException("Failed to list " + key, e);
        } catch (AmazonClientException e) {
            throw new IOException("Failed to list " + key, e);
        } finally {
            timer.stop();
        }
    }

    public FileInfo upload(String group, FileInfo inputFile,
            TransferListener listener) throws IOException {
        if (GenericValidator.isBlankOrNull(group)) {
            throw new IllegalArgumentException("group not specified");
        }
        if (inputFile == null) {
            throw new IllegalArgumentException("input file not specified");
        }
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".upload");

        try {
            FileInfo oldInfo = info(group, inputFile.getName());
            if (oldInfo.getByteSize() == inputFile.getByteSize()
                    && oldInfo.getMd5().equals(inputFile.getMd5())) {
                LOGGER.info("*****" + inputFile + " for " + group
                        + " is already uploaded");
                if (listener != null) {
                    listener.alreadyUploaded(new TransferEvent(this, oldInfo));
                }
                return oldInfo;
            }
        } catch (FileNotFoundException e) {
        } finally {
            timer.stop();
        }
        return doUpload(group, inputFile, listener);
    }

    private FileInfo doUpload(String group, FileInfo fileInfo,
            TransferListener listener) throws IOException {
        final String key = toKey(group, fileInfo.getName());
        LOGGER.info("Uploading " + fileInfo + " to " + group + "/" + key);
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".upload");
        InputStream in = new ProgressInputStream(fileInfo, 0, listener);
        try {
            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileInfo.getByteSize());
            metadata.setLastModified(fileInfo.getLastModified());
            metadata.setContentMD5(fileInfo.getMd5());
            s3.putObject(bucketName, key, in, metadata);
        } catch (AmazonServiceException e) {
            throw new IOException("Failed to save " + fileInfo, e);
        } catch (AmazonClientException e) {
            throw new IOException("Failed to save " + fileInfo, e);
        } finally {
            timer.stop();
        }
        return fileInfo;
    }

    private static void disableHostVerification() {
        TrustManager[] trustAllCerts = new TrustManager[] { new DisabledTrustManager() };
        try {
            SSLContext sc = SSLContext.getInstance("SSL"); // TLS
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            LOGGER.error("Failed to disable host verification", e);
        }
    }

    private void createBucketIfNonExistant(final String bucketName) {
        boolean found = false;
        for (Bucket bucket : s3.listBuckets()) {
            if (bucket.getName().equals(bucketName)) {
                found = true;
            }
        }
        if (!found) {
            LOGGER.info("Creating bucket " + bucketName);
            s3.createBucket(bucketName);
            LOGGER.info("Created bucket " + bucketName);
        }
    }

    private String toKey(String group, String inputFile) {
        return group + "__" + inputFile;
    }
}
