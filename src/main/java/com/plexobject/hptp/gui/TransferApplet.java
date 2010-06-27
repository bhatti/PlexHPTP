package com.plexobject.hptp.gui;

import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.RepaintManager;

import org.apache.log4j.Logger;
import org.jdesktop.swinghelper.debug.CheckThreadViolationRepaintManager;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.domain.MessageHandler;

public class TransferApplet extends JApplet implements MessageHandler {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(TransferApplet.class);
    private FileInfoPanel fileInfoPanel;

    public void init() {
        Configuration.getInstance().update(this);

        RepaintManager
                .setCurrentManager(new CheckThreadViolationRepaintManager());
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fileInfoPanel = new FileInfoPanel(TransferApplet.this);
                fileInfoPanel.setOpaque(true);
                TransferApplet.this.setContentPane(fileInfoPanel);
            }
        });
    }

    public void start() {
        showStatus("starting... ");
    }

    public void stop() {
        fileInfoPanel.cancelTransfer();
    }

    public void destroy() {
        fileInfoPanel.cancelTransfer();
    }

    public void handleException(final Exception error) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, error.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        LOGGER.error(error);
    }

    public void handleMessage(final String m) {
        LOGGER.info(m);
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showStatus(m);
            }
        });
    }

    public String getAppletInfo() {
        return "Upload Applet";
    }

    public String[][] getParameterInfo() {
        return new String[][] {
                { "user.name", "String", "username" },
                { "dir.name", "String", "upload-directory" },
                { "max.file.size", "long",
                        "max size of file that can be uploaded" },
                { "max.files", "long",
                        "max number of file that can be staged in one session" } };
    }
}
