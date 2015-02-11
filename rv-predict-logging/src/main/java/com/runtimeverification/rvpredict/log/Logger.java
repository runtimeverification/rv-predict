package com.runtimeverification.rvpredict.log;

import java.io.IOException;

/**
 * Class for dumping events to disk.  Reads data through an
 * {@link EventPipe} and writes them to an {@link EventOutputStream}
 */
public class Logger implements LoggingTask {
    private final EventPipe eventPipe;
    private final EventOutputStream outputStream;
    private Thread owner;

    public Logger(EventPipe eventPipe, EventOutputStream outputStream) {
        this.eventPipe = eventPipe;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        try {
            EventItem event;
            while (null != (event = eventPipe.readEvent())) {
                outputStream.writeEvent(event);
            }
            outputStream.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends closing signal to the event queue.
     * @throws InterruptedException
     */
    @Override
    public void finishLogging() throws InterruptedException {
        eventPipe.close();
    }

    @Override
    public void setOwner(Thread owner) {
        this.owner = owner;
    }

    /**
     * Wait for the thread to flush and finish.
     * @throws InterruptedException
     */
    public void awaitTermination() throws InterruptedException {
        owner.join();
    }
}
