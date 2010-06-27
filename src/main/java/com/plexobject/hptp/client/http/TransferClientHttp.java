package com.plexobject.hptp.client.http;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.validator.GenericValidator;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.plexobject.hptp.client.TransferClient;
import com.plexobject.hptp.client.io.InputStreamBodyWithLength;
import com.plexobject.hptp.client.io.ProgressInputStream;
import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.domain.TransferEvent;
import com.plexobject.hptp.domain.TransferListener;
import com.plexobject.hptp.metrics.Metric;
import com.plexobject.hptp.metrics.Timer;
import com.plexobject.hptp.service.FileSizeExceededException;
import com.plexobject.hptp.util.IOUtils;
import com.plexobject.hptp.util.Md5Utils;

public class TransferClientHttp implements TransferClient {
    private static final Logger LOGGER = Logger
            .getLogger(TransferClientHttp.class);

    private String uri;

    public TransferClientHttp() {
        this("http://localhost:" + Configuration.getInstance().getServerPort());
    }

    public TransferClientHttp(String uri) {
        this.uri = uri;
    }

    public void download(String group, String inputFile, File outputFile,
            TransferListener listener) throws IOException {
        if (GenericValidator.isBlankOrNull(group)) {
            throw new IllegalArgumentException("group not specified");
        }
        if (GenericValidator.isBlankOrNull(inputFile)) {
            throw new IllegalArgumentException("input file not specified");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("output fiel not specified");
        }
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".download");

        HttpClient httpclient = getClient();
        HttpGet method = new HttpGet(encodeUri(group, inputFile));

        LOGGER.info("Downloading " + inputFile + " for " + group + " -- "
                + method + " to " + outputFile);
        try {
            HttpResponse response = httpclient.execute(method);
            if (response.getStatusLine().getStatusCode() == 404) {
                throw new FileNotFoundException(inputFile + " not found for "
                        + group + " - " + response.getStatusLine());
            } else if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Failed to download " + inputFile
                        + " for " + group + " - " + response.getStatusLine());
            }
            HttpEntity entity = response.getEntity();

            //
            InputStream in = entity.getContent();

            OutputStream out = new BufferedOutputStream(new FileOutputStream(
                    outputFile));
            IOUtils.copy(outputFile, in, out, 0); // TODO progress call back
            // listener
            in.close();
            out.close();
            Header md5Header = response.getFirstHeader("X-MD5");
            if (md5Header == null) {
                throw new RuntimeException("MD5 header not found");
            }
            String remoteMd5 = md5Header.getValue();
            String localMd5 = Md5Utils.getMd5(outputFile);
            if (!remoteMd5.equals(localMd5)) {
                throw new IOException("MD5 of remote file " + inputFile + " ["
                        + remoteMd5 + "] does not match local MD5 " + localMd5
                        + " for " + outputFile);
            }
        } finally {
            httpclient.getConnectionManager().shutdown();
            timer.stop();
        }
    }

    public void delete(String group, String inputFile) throws IOException {
        if (GenericValidator.isBlankOrNull(group)) {
            throw new IllegalArgumentException("group not specified");
        }
        if (GenericValidator.isBlankOrNull(inputFile)) {
            throw new IllegalArgumentException("input file not specified");
        }

        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".delete");
        HttpClient httpclient = getClient();
        HttpDelete method = new HttpDelete(encodeUri(group, inputFile));
        LOGGER.info("Deleting " + group + "/" + inputFile);
        try {
            HttpResponse response = httpclient.execute(method);
            if (response.getStatusLine().getStatusCode() == 404) {
                throw new FileNotFoundException(inputFile + " not found for "
                        + group + " - " + response.getStatusLine());
            } else if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Failed to delete " + inputFile + " for "
                        + group + " - " + response.getStatusLine());
            }
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                resEntity.consumeContent();
            }

        } finally {
            httpclient.getConnectionManager().shutdown();
            timer.stop();
        }
    }

    public FileInfo info(String group, String inputFile) throws IOException {
        if (GenericValidator.isBlankOrNull(group)) {
            throw new IllegalArgumentException("group not specified");
        }
        if (GenericValidator.isBlankOrNull(inputFile)) {
            throw new IllegalArgumentException("input file not specified");
        }

        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".info");
        HttpClient httpclient = getClient();
        try {
            HttpHead method = new HttpHead(encodeUri(group, inputFile));

            HttpResponse response = httpclient.execute(method);
            if (response.getStatusLine().getStatusCode() == 404) {
                throw new FileNotFoundException(inputFile + " not found for "
                        + group + " - " + response.getStatusLine());
            } else if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Failed to list " + inputFile + " for "
                        + group + " - " + response.getStatusLine());
            }

            String jsonStr = response.getStatusLine().getReasonPhrase();
            LOGGER.debug("Got response " + response + " -- for " + group
                    + " - " + inputFile);

            JSONObject json = JSONObject.fromObject(jsonStr);
            return FileInfo.parse(json);
        } finally {
            httpclient.getConnectionManager().shutdown();
            timer.stop();
        }
    }

    public FileInfo[] list(String group) throws IOException {
        if (GenericValidator.isBlankOrNull(group)) {
            throw new IllegalArgumentException("group not specified");
        }

        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".list");
        HttpClient httpclient = getClient();
        HttpHead method = new HttpHead(uri + "/" + group);
        try {
            HttpResponse response = httpclient.execute(method);
            if (response.getStatusLine().getStatusCode() == 404) {
                throw new FileNotFoundException(group + " not found  - "
                        + response.getStatusLine());
            } else if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Failed to list " + group + " - "
                        + response.getStatusLine());
            }

            String jsonStr = response.getStatusLine().getReasonPhrase();
            JSONArray array = JSONArray.fromObject(jsonStr);
            FileInfo[] fileInfos = new FileInfo[array.size()];
            for (int i = 0; i < array.size(); i++) {
                JSONObject json = array.getJSONObject(i);
                fileInfos[i] = FileInfo.parse(json);
            }
            return fileInfos;
        } finally {
            httpclient.getConnectionManager().shutdown();
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
        long offset = 0;

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
            } else if (oldInfo.getByteSize() < inputFile.getByteSize()) {
                String partialMd5 = Md5Utils.calculateMd5(inputFile.getFile(),
                        oldInfo.getByteSize());
                if (partialMd5.equals(oldInfo.getMd5())) {
                    offset = oldInfo.getByteSize();
                    LOGGER.info("*****" + inputFile + " for " + group
                            + " is partially uploaded, skipping " + offset);
                } else {
                    LOGGER.info("*****" + inputFile + " for " + group
                            + " didn't match MD5 " + partialMd5
                            + " with offset  " + offset);
                }
            } else {
                LOGGER.warn("*****" + inputFile + " for " + group
                        + " had bigger file remotely " + oldInfo);
            }
        } catch (FileNotFoundException e) {
        } finally {
            timer.stop();
        }
        inputFile.setOffset(offset);
        return doUpload(group, inputFile, listener);
    }

    private FileInfo doUpload(String group, FileInfo fileInfo,
            TransferListener listener) throws IOException {
        if (GenericValidator.isBlankOrNull(group)) {
            throw new IllegalArgumentException("group not specified");
        }
        if (fileInfo == null) {
            throw new IllegalArgumentException("input file not specified");
        }

        if (fileInfo.getByteSize() > Configuration.getInstance()
                .getMaxFileSize()) {
            throw new FileSizeExceededException("File " + fileInfo
                    + " exceeds max size", fileInfo.getFile(), Configuration
                    .getInstance().getMaxFileSize());
        }
        HttpClient httpclient = getClient();
        HttpPost method = new HttpPost(encodeUri(group, fileInfo.getName()
                + ";" + fileInfo.getOffset()));
        try {
            MultipartEntity reqEntity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);
            InputStream in = new ProgressInputStream(fileInfo,
                    fileInfo.getOffset(), listener);
            reqEntity.addPart(fileInfo.getName(),
                    new InputStreamBodyWithLength(in, fileInfo.getByteSize(),
                            fileInfo.getContentType(), fileInfo.getName()));

            method.addHeader("X-Offset", String.valueOf(fileInfo.getOffset()));

            method.setEntity(reqEntity);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Started Uploaded " + group + "/" + fileInfo
                        + " offset " + fileInfo.getOffset() + " -- "
                        + method.getRequestLine());
            }

            HttpResponse response = httpclient.execute(method);
            if (response.getStatusLine().getStatusCode() == 413) {
                throw new FileSizeExceededException("File " + fileInfo
                        + " exceeds max size", fileInfo.getFile(),
                        Configuration.getInstance().getMaxFileSize());
            } else if (response.getStatusLine().getStatusCode() != 200) {

                throw new IOException("Failed to upload " + fileInfo + "\n"
                        + response.getStatusLine());
            }
            in.close();
            HttpEntity resEntity = response.getEntity();

            String jsonStr = EntityUtils.toString(resEntity);
            JSONArray array = JSONArray.fromObject(jsonStr);
            JSONObject json = array.getJSONObject(0);
            FileInfo uploadedFileInfo = FileInfo.parse(json);

            if (fileInfo.isCompleted()) {
                if (!uploadedFileInfo.getName().equals(fileInfo.getName())) {
                    throw new IOException("Uploaded file " + uploadedFileInfo
                            + " does not match local file " + fileInfo);
                }
                if (!fileInfo.getMd5().equals(uploadedFileInfo.getMd5())) {
                    throw new IOException("Uploaded file " + uploadedFileInfo
                            + " does not match MD5 with the local file "
                            + fileInfo);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Ended Uploaded " + group + "/" + fileInfo
                            + " offset " + fileInfo.getOffset() + " -- "
                            + uploadedFileInfo + ", listener " + listener);
                }
            }
            return uploadedFileInfo;
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    private String encodeUri(String group, String inputFile) {
        try {
            return uri + "/" + group + "/"
                    + URLEncoder.encode(inputFile, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode " + inputFile);
        }
    }

    private DefaultHttpClient getClient() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        params.setBooleanParameter("http.protocol.expect-continue", false);

        HttpConnectionParams.setConnectionTimeout(params, Configuration
                .getInstance().getInteger("connection.timeout", 60000));
        HttpConnectionParams.setSoTimeout(params, Configuration.getInstance()
                .getInteger("socket.timeout", 60000));
        HttpConnectionParams.setStaleCheckingEnabled(params, true);
        HttpConnectionParams.setTcpNoDelay(params, true);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), 80));
        registry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(
                params, registry);
        DefaultHttpClient httpclient = new DefaultHttpClient(manager, params);
        httpclient
                .setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(
                        3, true));
        httpclient.setRedirectHandler(new DefaultRedirectHandler());
        return httpclient;
    }
}
