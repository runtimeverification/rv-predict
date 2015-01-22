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
 * Logging server.  Makes it transparent to the Logging Engine on how
 * events are logged.  The current implementation associates to each thread
 * a queue for logging events into it and creates a thread for syncing events to disk.
 *
 * @author TraianSF
 */
public class LoggingServer implements Runnable {
    private static final AtomicInteger logFileId = new AtomicInteger();
    private final Configuration config;
    private Thread owner;
    private final List<LoggerThread> loggers = new LinkedList<>();
    private final BlockingQueue<EventPipe> loggersRegistry;
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
        EventPipe eventOS;
        try {
            while (ThreadLocalEventStream.END_REGISTRY != (eventOS = loggersRegistry.take())) {
                final EventOutputStream outputStream = newEventOutputStream();
                final LoggerThread logger = new LoggerThread(eventOS, outputStream);
                Thread loggerThread = new Thread(logger);
                loggerThread.setDaemon(true);
                loggerThread.start();
                loggers.add(logger);
            }
        } catch (InterruptedException e) {
            System.err.println("Process is being forcefully shut down. Log data lost.");
            System.err.println(e.getMessage());

        } catch (IOException e) {
            System.err.println("Error creating stream for logging trace.");
            System.err.println(e.getMessage());
        }
    }

    /**
     * Shuts down the logging process, signaling all threads, including the logging server
     * to finish recording and yields control.
     */
    public void finishLogging() throws InterruptedException, IOException {
        threadLocalTraceOS.close();
        owner.join();

        for (LoggerThread loggerThread : loggers) {
            loggerThread.finishLogging();
        }

        metadataLoggerThread.finishLogging();
    }


    private EventOutputStream newEventOutputStream() throws IOException {
        EventOutputStream eventOutputStream = null;
        int id = logFileId.incrementAndGet();
        OutputStream outputStream = new FileOutputStream(Paths.get(config.outdir,
                id + "_" + TraceCache.TRACE_SUFFIX
                        + (config.zip ? TraceCache.ZIP_EXTENSION : "")).toFile());
        if (config.zip) {
            outputStream = new GZIPOutputStream(outputStream,true);
        }
        eventOutputStream = new EventOutputStream(new BufferedOutputStream(
                outputStream));
        return eventOutputStream;
    }

    public EventPipe getOutputStream() {
       return threadLocalTraceOS.get();
    }
}
