package rvpredict.logging;

import rvpredict.config.Configuration;

/**
 * Created by Traian on 30.01.2015.
 */
public class PredictionServer implements Runnable {
    private Thread owner;
    private final Configuration config;
    private final LoggingFactory loggingFactory;
    public PredictionServer(LoggingEngine loggingEngine) {
        config = loggingEngine.getConfig();
        loggingFactory = loggingEngine.getLoggingFactory();
    }

    @Override
    public void run() {
        owner = Thread.currentThread();
        
    }

    public void finishLogging() throws InterruptedException {
        owner.join();
    }
}
