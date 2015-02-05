package rvpredict.log;

import rvpredict.config.Configuration;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Logging server.  Makes it transparent to the Logging Engine on how
 * events are logged.  The current implementation associates to each thread
 * a queue for logging events into it and creates a thread for syncing events to disk.
 *
 * @author TraianSF
 */
public class LoggingServer implements Runnable {
    private final LoggingEngine engine;
    private Thread owner;
    private final List<LoggerThread> loggers = new LinkedList<>();
    private final BlockingQueue<EventPipe> loggersRegistry;
    private final ThreadLocalEventStream threadLocalTraceOS;
    private MetadataLoggerThread metadataLoggerThread;


    public LoggingServer(LoggingEngine engine) {
        this.engine = engine;
        loggersRegistry = new LinkedBlockingQueue<>();
        if (!Configuration.online) {
            metadataLoggerThread = new MetadataLoggerThread(engine);
        }
        threadLocalTraceOS = new ThreadLocalEventStream(engine.getLoggingFactory(), loggersRegistry);
    }

    @Override
    public void run() {
        if (!Configuration.online) {
            Thread metadataLoggingThread = new Thread(metadataLoggerThread, "Metadata logger");
            metadataLoggerThread.setOwner(metadataLoggingThread);
            metadataLoggingThread.setDaemon(true);
            metadataLoggingThread.start();
        }

        EventPipe eventOS;
        try {
            while (ThreadLocalEventStream.END_REGISTRY != (eventOS = loggersRegistry.take())) {
                final EventOutputStream outputStream = engine.getLoggingFactory().createEventOutputStream();
                final LoggerThread logger = new LoggerThread(eventOS, outputStream);
                Thread loggerThread = new Thread(logger, "Logger thread");
                logger.setOwner(loggerThread);
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

        for (LoggerThread loggerThread : loggers) {
            loggerThread.join();
        }

        if (!Configuration.online) {
            metadataLoggerThread.finishLogging();
        }
    }

    public void writeEvent(EventItem event) {
        try {
            threadLocalTraceOS.get().writeEvent(event);
        } catch (InterruptedException e) {
            System.err.println("Process being interrupted. Log data in current buffer lost.");
            e.printStackTrace();
        }
    }

    public void setOwner(Thread owner) {
        this.owner = owner;
    }
}
