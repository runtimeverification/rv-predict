package com.runtimeverification.rvpredict.progressindicator;

import java.util.concurrent.TimeUnit;

public class ConsoleOneLineProgressIndicatorUI implements ProgressIndicatorUI {
    @Override
    public void reportState(
            OneItemProgress inputFile,
            OneItemProgress races,
            long racesFound,
            OneItemProgress totalTasksProgress,
            OneItemProgress smtTimeMillis) {
        System.out.print(String.format(
                "Input: %1$3d%%. Races found: %2$3d. Current input tasks: %3$3d%%. Time left: %4$ss.\r",
                inputFile.intPercentageDone(),
                racesFound,
                totalTasksProgress.intPercentageDone(),
                TimeUnit.MILLISECONDS.toSeconds(smtTimeMillis.getTotal() - smtTimeMillis.getDone())));
        System.out.flush();
    }
}
