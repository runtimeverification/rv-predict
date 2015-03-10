package com.runtimeverification.rvpredict.log;

import java.io.IOException;

/**
 * Common interface for logging tasks
 * @author TraianSF
 */
public interface LoggingTask extends Runnable {
    /**
     * Signaling the current task that the logging process has concluded and thus it
     * should give up resources and finish.
     * @throws InterruptedException if the process is interrupted during the cleaning
     * @throws IOException If I/O errors occur while finishing
     */
    void finishLogging() throws InterruptedException, IOException;

    /**
     * States that the given parameter represents the thread running this task
     * @param ownerThread the thread executing this logging task.
     */
    void setOwner(Thread ownerThread);
}
