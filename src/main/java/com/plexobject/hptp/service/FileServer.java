package com.plexobject.hptp.service;

import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.thread.BoundedThreadPool;

import com.plexobject.hptp.domain.Configuration;
import com.plexobject.hptp.service.http.FilesServlet;

public class FileServer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(FileServer.class);

    private Server server;
    private static final FileServer INSTANCE = new FileServer();

    public static FileServer getInstance() {
        return INSTANCE;
    }

    public synchronized void run() {
        try {
            LOGGER.debug("Starting Upload Server");
            server = new Server();
            server.setStopAtShutdown(true);
            BoundedThreadPool pool = new BoundedThreadPool();
            pool.setMinThreads(10);
            pool.setMaxThreads(250);
            pool.setLowThreads(25);

            SelectChannelConnector connector = new SelectChannelConnector();
            connector.setPort(Configuration.getInstance().getServerPort());
            connector.setMaxIdleTime(240000);
            connector.setAcceptors(8);
            connector.setAcceptQueueSize(1000);
            connector.setLowResourceMaxIdleTime(1000);
            connector.setLowResourcesConnections(11000);
            connector.setThreadPool(pool);

            server.setConnectors(new Connector[] { connector });

            Context root = new Context(server, "/", Context.SESSIONS);
            root.addServlet(new ServletHolder(new FilesServlet()), "/*");
            server.start();
            LOGGER.debug("Started Upload Server");
        } catch (Exception e) {
            LOGGER.error("Failed to start server", e);
        }
    }

    public Thread start() {
        Thread t = new Thread(INSTANCE);
        t.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        return t;
    }

    public synchronized void join() {
        try {
            server.join();
        } catch (Exception e) {
            LOGGER.error("Failed to join server", e);
        }
    }

    public synchronized void stop() {
        try {
            LOGGER.debug("Stopping Upload Server");
            server.stop();
            server.destroy();
            server = null;
        } catch (NullPointerException e) {
        } catch (Exception e) {
            LOGGER.error("Failed to stop server", e);
        } finally {
            LOGGER.debug("Stopped Upload Server");
        }
    }

    public static void main(String[] args) {
        Thread t = FileServer.getInstance().start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
