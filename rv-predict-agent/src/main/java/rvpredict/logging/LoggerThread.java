package rvpredict.logging;

import rvpredict.db.EventItem;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Created by Traian on 16.01.2015.
 */
public class LoggerThread implements Runnable {

    private final EventOutputStream loggerQueue;
    private final DataOutputStream outputStream;
    private Thread owner;

    public LoggerThread(EventOutputStream loggerQueue, DataOutputStream outputStream) {
        this.loggerQueue = loggerQueue;
        this.outputStream = outputStream;
    }

    /**
     * Writes an {@link rvpredict.db.EventItem} to the underlying output stream.
     *
     * @param      event   an {@link rvpredict.db.EventItem} to be written.
     * @exception  java.io.IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public void writeEvent(EventItem event) throws IOException {
        outputStream.writeLong(event.GID);
        outputStream.writeLong(event.TID);
        outputStream.writeInt(event.ID);
        outputStream.writeLong(event.ADDRL);
        outputStream.writeLong(event.ADDRR);
        outputStream.writeLong(event.VALUE);
        outputStream.writeByte(event.TYPE.ordinal());
    }

    @Override
    public void run() {
        owner = Thread.currentThread();
        try {
            List<EventItem> buffer;
            while (EventOutputStream.END_BUFFER != (buffer = loggerQueue.take())) {
                for (EventItem event : buffer) {
                    writeEvent(event);
                }
                outputStream.flush();
            }
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends closing signal to the event queue, then waits for the thread to flush and finish.
     */
    public void finishLogging() {
        loggerQueue.close();
        try {
            owner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
