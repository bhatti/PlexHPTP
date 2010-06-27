package com.plexobject.hptp.service.http;

import java.io.File;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.plexobject.hptp.domain.Configuration;

public class FileRequest {
    public final String group;
    public final File file;
    public final long offset;

    public FileRequest(String group, File file, long offset) {
        this.group = group;
        this.file = file;
        this.offset = offset;
    }

    public static FileRequest parse(HttpServletRequest req) {
        long offset = 0;

        String offsetHeader = req.getHeader("X-Offset");
        if (offsetHeader != null) {
            offset = Long.parseLong(offsetHeader);
        }
        String path = req.getPathInfo();
        int semiPosition = path.lastIndexOf(";");

        if (semiPosition != -1) {
            if (offset == 0) {
                offset = Long.parseLong(path.substring(semiPosition + 1));
            }
            path = path.substring(0, semiPosition);
        }

        final File uploadDir = Configuration.getInstance().getUploadDir();

        String[] tokens = StringUtils.split(path, "/");
        if (tokens.length == 0 || tokens.length > 2) {
            throw new RuntimeException("[" + tokens.length + "/"
                    + Arrays.asList(tokens) + "] Invalid path " + path);
        }
        String group = tokens[0].trim();
        String fileName = null;
        File file = null;
        if (tokens.length == 2) {
            fileName = tokens[1].trim();
            file = new File(uploadDir, group + File.separator + fileName);
            new File(uploadDir, group).mkdirs();
        }
        return new FileRequest(group, file, offset);
    }

    public String toString() {
        return group + "__" + file + ";" + offset;
    }
}
