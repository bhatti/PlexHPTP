package com.plexobject.hptp.domain;

public interface MessageHandler {
    void handleMessage(String m);

    void handleException(Exception e);
}
