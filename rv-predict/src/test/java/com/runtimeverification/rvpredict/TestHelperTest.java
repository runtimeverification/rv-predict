package com.runtimeverification.rvpredict;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestHelperTest {
    @Mock private TestHelper.Task mockTask;

    @Test
    public void testMatchesOutput() throws Exception {
        String taskOutput =
                "----------------Instrumented execution to record the trace-----------------\n" +
                "[RV-Predict] Log directory: /tmp/rv-predict8619430094651400241\n" +
                "[RV-Predict] Finished retransforming preloaded classes.\n" +
                "Data race on field jtsan.ExecutorTests.sharedVar: \n" +
                "    Concurrent read in thread T24\n" +
                "      > at jtsan.ExecutorTests.lambda$shutdownWrong$3(ExecutorTests.java:44)\n" +
                "        at jtsan.ExecutorTests$$Lambda$26.run(Unknown:n/a)\n" +
                "    T24 is created by T1\n" +
                "        at java.util.concurrent.ThreadPoolExecutor.addWorker(Unknown Source)\n" +
                "\n" +
                "    Concurrent write in thread T25\n" +
                "      > at jtsan.ExecutorTests.lambda$shutdownWrong$3(ExecutorTests.java:44)\n" +
                "        at jtsan.ExecutorTests$$Lambda$26.run(Unknown:n/a)\n" +
                "    T25 is created by T1\n" +
                "        at java.util.concurrent.ThreadPoolExecutor.addWorker(Unknown Source)\n" +
                "\n" +
                "\n" +
                "Data race on field jtsan.ExecutorTests.sharedVar: \n" +
                "    Concurrent write in thread T20\n" +
                "      > at jtsan.ExecutorTests.lambda$cachedThreadPoolRaceyTasks$2(ExecutorTests.java:33)\n" +
                "        at jtsan.ExecutorTests$$Lambda$25.run(Unknown:n/a)\n" +
                "    T20 is created by T1\n" +
                "        at java.util.concurrent.ThreadPoolExecutor.addWorker(Unknown Source)\n" +
                "\n" +
                "    Concurrent read in thread T19\n" +
                "      > at jtsan.ExecutorTests.lambda$cachedThreadPoolRaceyTasks$2(ExecutorTests.java:33)\n" +
                "        at jtsan.ExecutorTests$$Lambda$25.run(Unknown:n/a)\n" +
                "    T19 is created by T1\n" +
                "        at java.util.concurrent.ThreadPoolExecutor.addWorker(Unknown Source)\n" +
                "\n" +
                "\n" +
                "Data race on field jtsan.ExecutorTests.sharedVar: \n" +
                "    Concurrent write in thread T14\n" +
                "      > at jtsan.ExecutorTests.lambda$fixedThreadPoolRaceyTasks$1(ExecutorTests.java:23)\n" +
                "        at jtsan.ExecutorTests$$Lambda$22.run(Unknown:n/a)\n" +
                "    T14 is created by T1\n" +
                "        at java.util.concurrent.ThreadPoolExecutor.addWorker(Unknown Source)\n" +
                "\n" +
                "    Concurrent read in thread T15\n" +
                "      > at jtsan.ExecutorTests.lambda$fixedThreadPoolRaceyTasks$1(ExecutorTests.java:23)\n" +
                "        at jtsan.ExecutorTests$$Lambda$22.run(Unknown:n/a)\n" +
                "    T15 is created by T1\n" +
                "        at java.util.concurrent.ThreadPoolExecutor.addWorker(Unknown Source)\n" +
                "\n";
        when(mockTask.call()).thenReturn(0);
        when(mockTask.output()).thenReturn(taskOutput);
        TestHelper.testCommand(
                new String[]{
                        "Data race on field jtsan.ExecutorTests.sharedVar: (.*)"
                                + "> at jtsan.ExecutorTests.lambda\\$cachedThreadPoolRaceyTasks\\$.\\(ExecutorTests.java:33\\)(.*)"
                                + "> at jtsan.ExecutorTests.lambda\\$cachedThreadPoolRaceyTasks\\$.\\(ExecutorTests.java:33\\)(.*)"
                },
                Collections.singletonList(mockTask),
                "testCommand"
                );
    }
}
