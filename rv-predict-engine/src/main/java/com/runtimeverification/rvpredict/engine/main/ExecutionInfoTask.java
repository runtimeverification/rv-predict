package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.util.Logger;
import violation.Violation;

import java.util.Set;

/**
 * End of prediction hook meant to displaying information collected during the run.
 */
class ExecutionInfoTask implements Runnable {
    private final Logger logger;
    private final Set<Violation> violations;
    long start_time;

    ExecutionInfoTask(RVPredict rvPredict, long startTime) {
        this.start_time = startTime;
        logger = rvPredict.getLogger();
        violations = rvPredict.getViolations();
    }

    @Override
    public void run() {
        if (violations.size() == 0) {
            logger.report("No races found.", Logger.MSGTYPE.INFO);
        }
        logger.closePrinter();
    }

}
