package com.runtimeverification.rvpredict.progressindicator;

interface ProgressIndicatorUI {
    void reportState(
            OneItemProgress inputFile,
            OneItemProgress races,
            long racesFound,
            OneItemProgress totalTasksProgress,
            OneItemProgress smtTimeMillis);
}
