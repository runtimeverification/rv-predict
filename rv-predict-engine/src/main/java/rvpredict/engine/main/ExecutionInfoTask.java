package rvpredict.engine.main;

import rvpredict.trace.TraceInfo;
import rvpredict.util.Logger;
import violation.Violation;

import java.util.Set;

/**
 * End of prediction hook meant to displaying information collected during the run.
 */
class ExecutionInfoTask implements Runnable {
    private final Logger logger;
    private final Set<Violation> violations;
    TraceInfo info;
    long start_time;

    ExecutionInfoTask(RVPredict rvPredict, long startTime, TraceInfo info) {
        this.info = info;
        this.start_time = startTime;
        logger = rvPredict.getLogger();
        violations = rvPredict.getViolations();
    }

    @Override
    public void run() {

        // Report statistics about the trace and race detection

        // TODO: query the following information from DB may be expensive

        int TOTAL_THREAD_NUMBER = info.getTraceThreadNumber();
        int TOTAL_SHAREDVARIABLE_NUMBER = info.getTraceSharedVariableNumber();
        int TOTAL_BRANCH_NUMBER = info.getTraceBranchNumber();
        int TOTAL_SHAREDREADWRITE_NUMBER = info.getTraceSharedReadWriteNumber();
        int TOTAL_LOCALREADWRITE_NUMBER = info.getTraceLocalReadWriteNumber();
        int TOTAL_INITWRITE_NUMBER = info.getTraceInitWriteNumber();

        int TOTAL_SYNC_NUMBER = info.getTraceSyncNumber();
        int TOTAL_PROPERTY_NUMBER = info.getTracePropertyNumber();

        if (violations.size() == 0)
            logger.report("No races found.", Logger.MSGTYPE.INFO);
        else {
            logger.report("Trace Size: " + info.getTraceLength(), Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Threads: " + TOTAL_THREAD_NUMBER, Logger.MSGTYPE.STATISTICS);
            logger.report("Total #SharedVariables: " + TOTAL_SHAREDVARIABLE_NUMBER,
                    Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Shared Read-Writes: " + TOTAL_SHAREDREADWRITE_NUMBER,
                    Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Local Read-Writes: " + TOTAL_LOCALREADWRITE_NUMBER,
                    Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Initial Writes: " + TOTAL_INITWRITE_NUMBER,
                    Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Synchronizations: " + TOTAL_SYNC_NUMBER,
                    Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Branches: " + TOTAL_BRANCH_NUMBER, Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Property Events: " + TOTAL_PROPERTY_NUMBER,
                    Logger.MSGTYPE.STATISTICS);

            logger.report("Total #Potential Violations: "
                    + (violations.size()),
                    Logger.MSGTYPE.STATISTICS);
            logger.report("Total #Real Violations: " + violations.size(),
                    Logger.MSGTYPE.STATISTICS);
            logger.report("Total Time: " + (System.currentTimeMillis() - start_time) + "ms",
                    Logger.MSGTYPE.STATISTICS);
        }

        logger.closePrinter();

    }

}
