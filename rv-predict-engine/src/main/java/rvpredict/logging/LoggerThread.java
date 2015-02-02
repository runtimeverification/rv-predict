package rvpredict.logging;

import rvpredict.db.EventItem;

import java.io.IOException;

/**
 * Class for dumping events to disk.  Reads data through the given
 * {@link BufferedEventPipe} and writes them to given {@link java.io.DataOutputStream}.
 */
public class LoggerThread implements Runnable {
    private final EventPipe eventPipe;
    private final EventOutputStream outputStream;
    private Thread owner;

    public LoggerThread(EventPipe eventPipe, EventOutputStream outputStream) {
        this.eventPipe = eventPipe;
        this.outputStream = outputStream;
    }


    @Override
    public void run() {
        owner = Thread.currentThread();
        try {
            EventItem event;
            while (null != (event = eventPipe.readEvent())) {
                outputStream.writeEvent(event);
                outputStream.flush();
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends closing signal to the event queue, then waits for the thread to flush and finish.
     */
    public void finishLogging() throws InterruptedException {
        eventPipe.close();
        owner.join();
    }
}
