package rvpredict.logging;

import rvpredict.config.Configuration;
import rvpredict.db.TraceCache;

import java.io.*;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Traian on 16.01.2015.
 */
public class LoggingServer implements Runnable {
    private static final AtomicInteger threadId = new AtomicInteger();
    private final Configuration config;
    private Thread owner;
    private List<LoggerThread> loggers = new LinkedList<>();
    BlockingQueue<EventOutputStream> loggersRegistry;
    private final ThreadLocalEventStream threadLocalTraceOS;
    private final MetadataLoggerThread metadataLoggerThread;


    public LoggingServer(LoggingEngine engine) {
        loggersRegistry = new LinkedBlockingQueue<>();
        threadLocalTraceOS = new ThreadLocalEventStream(loggersRegistry);
        this.config = engine.getConfig();
        metadataLoggerThread = new MetadataLoggerThread(engine);
    }

    @Override
    public void run() {
        Thread metadataLoggingThread = new Thread(metadataLoggerThread);
        metadataLoggingThread.setDaemon(true);
        metadataLoggingThread.start();

        owner = Thread.currentThread();
        EventOutputStream eventOS;
        try {
            while (ThreadLocalEventStream.END_REGISTRY != (eventOS = loggersRegistry.take())) {
                final DataOutputStream outputStream = newDataOutputStream();
                final LoggerThread logger = new LoggerThread(eventOS, outputStream);
                Thread loggerThread = new Thread(logger);
                loggerThread.setDaemon(true);
                loggerThread.start();
                loggers.add(logger);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void finishLogging() {
        threadLocalTraceOS.close();
        try {
            owner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (LoggerThread loggerThread : loggers) {
            loggerThread.finishLogging();
        }

        metadataLoggerThread.finishLogging();
    }






    private DataOutputStream newDataOutputStream() {
        DataOutputStream dataOutputStream = null;
        try {
            int id = threadId.incrementAndGet();
            OutputStream outputStream = new FileOutputStream(Paths.get(config.outdir,
                    id + "_" + TraceCache.TRACE_SUFFIX
                            + (config.zip ? TraceCache.ZIP_EXTENSION : "")).toFile());
            if (config.zip) {
                outputStream = new GZIPOutputStream(outputStream,true);
            }
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(
                    outputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) { // GZIPOutputStream exception
            e.printStackTrace();
        }
        return dataOutputStream;
    }

    public EventOutputStream getOutputStream() {
       return threadLocalTraceOS.get();
    }
}
