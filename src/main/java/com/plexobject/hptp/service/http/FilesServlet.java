package com.plexobject.hptp.service.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.FileInfo;
import com.plexobject.hptp.metrics.Metric;
import com.plexobject.hptp.metrics.Timer;
import com.plexobject.hptp.service.FileSizeExceededException;

public class FilesServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(FilesServlet.class);
    private static final long serialVersionUID = 1L;
    private FilesHelper filesHelper;

    public void init(ServletConfig config) throws ServletException {
        Configuration.getInstance().update(config);
        filesHelper = new FilesHelper(Configuration.getInstance()
                .getUploadDir(), Configuration.getInstance().getMaxFileSize());
    }

    public void destroy() {
        super.destroy();
    }

    public void doHead(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".doHead");
        try {
            res.setContentType("application/json");
            final FileRequest fileRequest = FileRequest.parse(req);
            String response = null;
            if (fileRequest.file != null) {
                FileInfo fileInfo = filesHelper.list(fileRequest.group,
                        fileRequest.file.getName());
                JSONObject json = fileInfo.toJson();
                response = json.toString();
            } else {
                Collection<FileInfo> fileInfos = filesHelper
                        .list(fileRequest.group);
                JSONArray array = toJson(fileInfos);
                response = array.toString();
            }

            res.sendError(200, response);
        } catch (FileNotFoundException e) {
            res.sendError(404, e.getMessage());
        } catch (SecurityException e) {
            LOGGER.error("Failed to get file informationt", e);
            res.sendError(500, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to get file informationt", e);
            res.sendError(500, "Failed to get file information " + e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".doPost");
        try {
            res.setContentType("application/json");
            PrintWriter out = res.getWriter();
            res.setBufferSize(16 * 1024);

            Collection<FileInfo> fileInfos = filesHelper.getUploadedFiles(req);
            String response = toJson(fileInfos).toString();
            LOGGER.debug("doPost " + req.getPathInfo() + " returning "
                    + response);
            out.println(response);
        } catch (FileSizeExceededException e) {
            res.sendError(413, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to get file informationt", e);
            res.sendError(500, "Failed to get file information " + e);
        } finally {
            timer.stop();
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".doGet");
        try {
            FileInfo fileInfo = filesHelper.getFileForRead(req);
            FilesHelper.sendFile(fileInfo, req, res);
            LOGGER.debug("doGet " + req.getPathInfo() + " sent file");
        } catch (FileNotFoundException e) {
            res.sendError(404, e.getMessage());
        } catch (SecurityException e) {
            LOGGER.error("Failed to get file informationt", e);
            res.sendError(500, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to delete", e);
            res.sendError(500, "Failed to delete " + e);
        } finally {
            timer.stop();
        }

    }

    public void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        final Timer timer = Metric.newTimer(getClass().getSimpleName()
                + ".doDelete");
        try {
            filesHelper.deleteFile(req);
            LOGGER.debug("doDelete " + req.getPathInfo() + " deleted ");
            res.setStatus(200);
        } catch (FileNotFoundException e) {
            res.sendError(404, e.getMessage());
        } catch (IOException e) {
            res.sendError(400, e.getMessage());
        } catch (SecurityException e) {
            LOGGER.error("Failed to get file informationt", e);
            res.sendError(500, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to delete", e);
            res.sendError(500, "Failed to delete " + e);
        } finally {
            timer.stop();
        }
    }

    public String getServletInfo() {
        return "Upload Servlet";
    }

    static JSONArray toJson(Collection<FileInfo> fileInfos) {
        JSONArray array = new JSONArray();
        for (FileInfo fileInfo : fileInfos) {
            array.add(fileInfo.toJson());
        }
        return array;
    }

}
