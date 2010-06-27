package com.plexobject.hptp.service.http;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.service.FileSizeExceededException;
import com.plexobject.hptp.util.IOUtils;

public class FilesHelper {
    private static final Logger LOGGER = Logger.getLogger(FilesHelper.class);
    protected static final String MD5_SUFFIX = Configuration.getInstance()
            .getMd5Suffix();

    private final File uploadDir;
    private final int maxSize;

    public FilesHelper(final File uploadDir, final int maxSize) {
        if (uploadDir == null) {
            throw new IllegalArgumentException("upload-dir not specified");
        }
        if (maxSize == 0) {
            throw new IllegalArgumentException("max-size not specified");
        }
        this.uploadDir = uploadDir;
        this.maxSize = maxSize;
        LOGGER.info("Will store all uploads in " + uploadDir);
    }

    FileInfo list(String group, String fileName) throws FileNotFoundException,
            SecurityException {
        final File dir = new File(uploadDir, group);
        if (dir.exists()) {
            if (!dir.canRead()) {
                throw new SecurityException("Not authorized to read directory "
                        + dir);
            } else {
                File file = new File(dir, fileName);
                if (file.exists()) {
                    if (!file.canRead()) {
                        throw new SecurityException("Not authorized to read "
                                + file);
                    }
                    return new FileInfo(file);
                } else {
                    throw new FileNotFoundException("Failed to find "
                            + file.getAbsolutePath());
                }
            }
        } else {
            throw new FileNotFoundException("Failed to find directory  "
                    + dir.getAbsolutePath());
        }
    }

    Collection<FileInfo> list(String group) throws FileNotFoundException,
            SecurityException {
        final File dir = new File(uploadDir, group);
        if (dir.exists()) {
            if (!dir.canRead()) {
                throw new SecurityException("Not authorized to read " + dir);
            } else {
                File[] files = dir.listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        return !file.getName().endsWith(MD5_SUFFIX);
                    }
                });
                Collection<FileInfo> fileInfos = new ArrayList<FileInfo>();
                for (File file : files) {
                    fileInfos.add(new FileInfo(file));
                }
                return fileInfos;
            }
        } else {
            throw new FileNotFoundException("Failed to find "
                    + dir.getAbsolutePath());
        }
    }

    FileInfo getFileForRead(HttpServletRequest req)
            throws FileNotFoundException {
        FileRequest fileDetails = FileRequest.parse(req);
        if (fileDetails.file.exists()) {
            if (!fileDetails.file.canRead()) {
                throw new SecurityException("Not authorized to read "
                        + fileDetails.file.getName() + " for group "
                        + fileDetails.group);
            } else {
                return new FileInfo(fileDetails.file);
            }
        } else {
            throw new FileNotFoundException("Failed to find "
                    + fileDetails.file.getAbsolutePath());
        }
    }

    void deleteFile(final HttpServletRequest req) throws IOException {
        final FileRequest fileDetails = FileRequest.parse(req);
        if (fileDetails.file.exists()) {
            if (!fileDetails.file.canWrite()) {
                throw new SecurityException("Not authorized to delete "
                        + fileDetails.file.getName() + " for group "
                        + fileDetails.group);
            } else if (fileDetails.file.delete()) {
                new File(fileDetails.file.getAbsolutePath() + MD5_SUFFIX)
                        .delete();
                return;
            } else {
                throw new IOException("Failed to delete "
                        + fileDetails.file.getAbsolutePath());
            }
        } else {
            throw new FileNotFoundException("Failed to find "
                    + fileDetails.file.getAbsolutePath());
        }
    }

    static void sendFile(FileInfo fileInfo, HttpServletRequest req,
            HttpServletResponse res) throws IOException {
        OutputStream out = null;
        final String encodings = req.getHeader("Accept-Encoding");
        if (encodings != null && encodings.indexOf("gzip") != -1) {
            // Go with GZIP
            if (encodings.indexOf("x-gzip") != -1) {
                res.setHeader("Content-Encoding", "x-gzip");
            } else {
                res.setHeader("Content-Encoding", "gzip");
            }
            out = new GZIPOutputStream(res.getOutputStream());
        } else if (encodings != null && encodings.indexOf("compress") != -1) {
            // Go with ZIP
            if (encodings.indexOf("x-compress") != -1) {
                res.setHeader("Content-Encoding", "x-compress");
            } else {
                res.setHeader("Content-Encoding", "compress");
            }
            out = new ZipOutputStream(res.getOutputStream());
            ((ZipOutputStream) out).putNextEntry(new ZipEntry("dummy name"));
        } else {
            // No compression
            out = res.getOutputStream();
        }
        res.setHeader("X-MD5", fileInfo.getMd5());
        res.setHeader("X-Modified-Date", String.valueOf(fileInfo
                .getLastModified().getTime()));
        res.setHeader("X-FileSize", String.valueOf(fileInfo.getByteSize()));

        res.setHeader("Vary", "Accept-Encoding");
        res.setContentType(fileInfo.getContentType());

        IOUtils.copy(fileInfo.getFile(), out);
    }

    @SuppressWarnings("unchecked")
    Collection<FileInfo> getUploadedFiles(final HttpServletRequest req)
            throws IOException {
        Date started = new Date();
        final FileRequest fileRequest = FileRequest.parse(req);
        final File dir = new File(uploadDir, fileRequest.group);

        dir.mkdirs();
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        Collection<FileInfo> fileInfos = new ArrayList<FileInfo>();

        if (isMultipart) {
            DiskFileItemFactory factory = new DiskFileItemFactory();

            // factory.setSizeThreshold(yourMaxMemorySize);
            // factory.setRepository(dir);
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(maxSize);

            try {
                List<FileItem> items = upload.parseRequest(req);
                for (FileItem item : items) {
                    final String name = item.getFieldName();
                    final long byteTransferred = item.getSize();
                    final File file = new File(dir, name);

                    if (fileRequest.offset > 0 && file.exists()) {
                        IOUtils.appendCopy(item.getInputStream(), file);
                        LOGGER.info("**** Appended " + file + " at offset "
                                + fileRequest.offset);
                    } else {
                        item.write(file);
                    }

                    final FileInfo fileInfo = new FileInfo(file);
                    fileInfo.setTransferStarted(started);

                    fileInfo.setBytesTransferred(byteTransferred);
                    fileInfo.setCompleted();
                    fileInfos.add(fileInfo);
                    LOGGER.info("Multipart Saved " + fileInfo);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            } catch (FileUploadBase.SizeLimitExceededException e) {
                throw new FileSizeExceededException("File size for "
                        + fileRequest + " exceeded", fileRequest.file, maxSize);
            } catch (Exception e) {
                throw new IOException("Failed to upload " + fileRequest, e);
            }
        } else {
            final File file = fileRequest.file;
            IOUtils.copy(req.getInputStream(), file);
            final FileInfo fileInfo = new FileInfo(file);
            fileInfo.setTransferStarted(started);

            fileInfo.setBytesTransferred(file.length());
            fileInfo.setCompleted();
            fileInfos.add(fileInfo);
            LOGGER.info("Simple Post Saved " + fileInfo);
        }
        return fileInfos;
    }
}
