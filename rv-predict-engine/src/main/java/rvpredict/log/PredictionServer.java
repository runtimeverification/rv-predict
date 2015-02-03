package rvpredict.log;

import rvpredict.config.Configuration;
import rvpredict.engine.main.RVPredict;

import java.io.IOException;

/**
 * Created by Traian on 30.01.2015.
 */
public class PredictionServer implements Runnable {
    private Thread owner;
    private final Configuration config;
    private final LoggingFactory loggingFactory;
    private final RVPredict rvPredict;
    public PredictionServer(LoggingEngine loggingEngine) {
        config = loggingEngine.getConfig();
        loggingFactory = loggingEngine.getLoggingFactory();
        RVPredict rvPredict = null;
        try {
            rvPredict = new RVPredict(config, loggingFactory);
        } catch (IOException | ClassNotFoundException e) {
            assert false : "These exceptions should only be thrown for offline prediction";
        }
        this.rvPredict = rvPredict;
    }

    @Override
    public void run() {
        rvPredict.run();
    }

    public void finishLogging() throws InterruptedException {
        loggingFactory.finishLogging();
        owner.join();
        rvPredict.report();
    }

    public void setOwner(Thread owner) {
        this.owner = owner;
    }
}
