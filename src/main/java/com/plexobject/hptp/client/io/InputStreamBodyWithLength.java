package com.plexobject.hptp.client.io;

import java.io.InputStream;

import org.apache.http.entity.mime.content.InputStreamBody;

public class InputStreamBodyWithLength extends InputStreamBody {
    private final long length;

    public InputStreamBodyWithLength(final InputStream in,
            final long length, final String mimeType, final String filename) {
        super(in, mimeType, filename);
        this.length = length;
    }

    @Override
    public long getContentLength() {
        return this.length;
    }

}
