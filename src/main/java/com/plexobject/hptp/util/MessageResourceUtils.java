package com.plexobject.hptp.util;

import java.text.Format;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import com.plexobject.hptp.domain.Configuration;

public class MessageResourceUtils {
    private static MessageResourceUtils INSTANCE;
    private ResourceBundle bundle;

    public static synchronized MessageResourceUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MessageResourceUtils();
        }
        return INSTANCE;
    }

    public String getString(String key) {
        return bundle.getString(key);
    }

    public String getString(String key, Object[] args, Format[] formats) {
        final String pattern = bundle.getString(key);
        MessageFormat messageForm = new MessageFormat("");
        messageForm.setLocale(bundle.getLocale());
        messageForm.applyPattern(pattern);
        if (formats != null) {
            messageForm.setFormats(formats);
        }
        return messageForm.format(args);
    }

    private MessageResourceUtils() {
        bundle = ResourceBundle.getBundle("MessageResources", Configuration
                .getInstance().getDefaultLocale());
    }
}
