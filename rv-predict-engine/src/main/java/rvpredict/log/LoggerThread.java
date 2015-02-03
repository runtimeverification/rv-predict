package rvpredict.log;

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
        try {
            EventItem event;
            while (null != (event = eventPipe.readEvent())) {
                outputStream.writeEvent(event);
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends closing signal to the event queue.
     * @throws InterruptedException
     */
    public void finishLogging() throws InterruptedException {
        eventPipe.close();
    }

    public void setOwner(Thread owner) {
        this.owner = owner;
    }

    /**
     * Wait for the thread to flush and finish.
     * @throws InterruptedException
     */
    public void join() throws InterruptedException {
        owner.join();
    }
}
