package com.plexobject.hptp.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.plexobject.hptp.domain.Configuration;

public class ExtensionUtils {
    public static FileFilter newFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                if (f.getName().endsWith(".md5")) {
                    return false;
                }
                if (f.length() > Configuration.getInstance().getMaxFileSize()) {
                    return false;
                }
                return true;
            }

            @Override
            public String getDescription() {
                return null;
            }
        };
    }
}
