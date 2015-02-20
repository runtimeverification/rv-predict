package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.config.Configuration;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Logging server.  Makes it transparent to the Logging Engine on how
 * events are logged.  The current implementation associates to each thread
 * a queue for logging events into it and creates a thread for syncing events to disk.
 *
 * @author TraianSF
 */
public class LoggingServer implements LoggingTask {
    private final LoggingEngine engine;
    private Thread owner;
    private final List<Logger> loggers = new ArrayList<>();
    private final ThreadLocalEventStream threadLocalTraceOS;
    private final MetadataLogger metadataLogger;

    private final List<Throwable> uncaughtExceptions = new ArrayList<>();

    public LoggingServer(LoggingEngine engine) {
        this.engine = engine;
        metadataLogger = Configuration.online ? null : new MetadataLogger(engine);
        threadLocalTraceOS = new ThreadLocalEventStream(engine.getLoggingFactory());
    }

    @Override
    public void run() {
        if (!Configuration.online) {
            Thread metadataLoggerThread = new Thread(metadataLogger, "Metadata logger");
            metadataLogger.setOwner(metadataLoggerThread);
            metadataLoggerThread.setDaemon(true);
            metadataLoggerThread.start();
        }

        EventPipe eventOS;
        try {
            while (ThreadLocalEventStream.END_REGISTRY != (eventOS = threadLocalTraceOS
                    .takeEventPipe())) {
                EventOutputStream outputStream = engine.getLoggingFactory().createEventOutputStream();
                Logger logger = new Logger(eventOS, outputStream);
                Thread loggerThread = new Thread(logger);
                logger.setOwner(loggerThread);
                loggerThread.setName("Logger-" + loggerThread.getId());
                loggerThread.setDaemon(true);
                loggerThread.start();
                loggerThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        /* if e is already a StackOverflowError, trying to print
                         * it here is going to cause the StackOverflowError
                         * again */
                        uncaughtExceptions.add(e);
                    }
                });
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
    @Override
    public void finishLogging() throws InterruptedException, IOException {
        threadLocalTraceOS.close();
        owner.join();

        for (Logger logger : loggers) {
            logger.finishLogging();
        }

        for (Logger logger : loggers) {
            logger.awaitTermination();
        }

        if (!Configuration.online) {
            metadataLogger.finishLogging();
        }

        if (!uncaughtExceptions.isEmpty()) {
            for (Throwable e : uncaughtExceptions) {
                e.printStackTrace();
            }
            throw new RuntimeException(
                    "[logging] fatal error: uncaught exceptions thrown from logger threads!");
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

    @Override
    public void setOwner(Thread owner) {
        this.owner = owner;
    }
}
