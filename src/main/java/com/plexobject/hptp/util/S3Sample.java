package com.plexobject.hptp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This sample demonstrates how to make basic requests to Amazon S3 using the
 * AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon S3. For more information on Amazon
 * S3, see http://aws.amazon.com/s3.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in the
 * AwsCredentials.properties file before you try to run this sample.
 * http://aws.amazon.com/security-credentials
 */
public class S3Sample {
    static {
        System.setProperty("javax.net.ssl.trustStore", "cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        try {
            CertUtils.installCert("s3.amazonaws.com");
        } catch (Exception e) {
        }
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(
                S3Sample.class.getClassLoader().getResourceAsStream(
                        "application.properties")));

        String bucketName = "upload-staging";
        String key = "UploadClientImplTest__plexhptp.dat";

        try {

            System.out.println("Creating bucket " + bucketName + "\n");
            s3.createBucket(bucketName);

            /*
             * List the buckets in your account
             */
            System.out.println("Listing buckets");
            for (Bucket bucket : s3.listBuckets()) {
                System.out.println(" - " + bucket.getName());
            }
            System.out.println();

            System.out.println("Uploading a new object to S3 from a file\n");
            s3.putObject(new PutObjectRequest(bucketName, key,
                    createSampleFile()));

            System.out.println("Listing objects");
            ObjectListing objectListing = s3
                    .listObjects(new ListObjectsRequest().withBucketName(
                            bucketName).withPrefix("UploadClientImplTest"));
            for (S3ObjectSummary objectSummary : objectListing
                    .getObjectSummaries()) {
                System.out.println(" - " + objectSummary.getKey() + "  "
                        + "(size = " + objectSummary.getSize() + ")");
            }
            System.out.println();

            System.out.println("Downloading an object");
            S3Object object = s3
                    .getObject(new GetObjectRequest(bucketName, key));
            System.out.println("Content-Type: "
                    + object.getObjectMetadata().getContentType());
            displayTextInputStream(object.getObjectContent());

            /*
             * Delete an object - Unless versioning has been turned on for your
             * bucket, there is no way to undelete an object, so use caution
             * when deleting objects.
             */
            System.out.println("Deleting an object\n");
            s3.deleteObject(bucketName, key);

            /*
             * Delete a bucket - A bucket must be completely empty before it can
             * be deleted, so remember to delete any objects from your buckets
             * before you try to delete them.
             */
            System.out.println("Deleting bucket " + bucketName + "\n");
            s3.deleteBucket(bucketName);
        } catch (AmazonServiceException ase) {
            System.out
                    .println("Caught an AmazonServiceException, which means your request made it "
                            + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out
                    .println("Caught an AmazonClientException, which means the client encountered "
                            + "a serious internal problem while trying to communicate with S3, "
                            + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3
     * 
     * @return A newly created temporary file with text data.
     * 
     * @throws IOException
     */
    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();

        return file;
    }

    /**
     * Displays the contents of the specified input stream as text.
     * 
     * @param input
     *            The input stream to display as text.
     * 
     * @throws IOException
     */
    private static void displayTextInputStream(InputStream input)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;

            System.out.println("    " + line);
        }
        System.out.println();
    }

}
