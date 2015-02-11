package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.config.Configuration;

import java.io.*;
import java.util.LinkedList;
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
    private final List<Logger> loggers = new LinkedList<>();
    private final ThreadLocalEventStream threadLocalTraceOS;
    private MetadataLogger metadataLogger;


    public LoggingServer(LoggingEngine engine) {
        this.engine = engine;
        if (!Configuration.online) {
            metadataLogger = new MetadataLogger(engine);
        }
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
