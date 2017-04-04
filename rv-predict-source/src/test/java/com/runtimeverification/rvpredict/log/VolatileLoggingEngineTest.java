package com.runtimeverification.rvpredict.log;

import static org.mockito.Mockito.mock;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;

public class VolatileLoggingEngineTest {
    private static final int LOC_ID = 10;
    private static final int ADDR = 11;
    private static final long VALUE = 12;

    private static class VolatileLoggingEngineForTest extends VolatileLoggingEngine {
        private final ControlFlags flags;

        private VolatileLoggingEngineForTest(Configuration config, Metadata metadata, ControlFlags flags) {
            super(config, metadata);
            this.flags = flags;
        }

        @Override
        protected void runRaceDetection(int numOfEvents) {
            flags.hasStartedRaceDetection = true;
            while (!flags.canFinishRaceDetection) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
            flags.hasStartedRaceDetection = false;
        }
    }

    @Test
    public void getGidDeadlock() throws InterruptedException {
        Configuration configuration = mock(Configuration.class);
        configuration.windowSize = 1;

        Metadata metadata = mock(Metadata.class);

        ControlFlags controlFlags = new ControlFlags();
        controlFlags.canFinishRaceDetection = false;

        VolatileLoggingEngine loggingEngine = new VolatileLoggingEngineForTest(configuration, metadata, controlFlags);

        Thread t1 = new Thread(() -> {
            // This will use one GID, filling the window.
            loggingEngine.log(EventType.WRITE, LOC_ID, ADDR, ADDR, VALUE, VALUE);
            // This will try to use one GID, which does not fit in the window anymore,
            // so it will trigger race detection, cleaning the window afterwards.
            //
            // However, the control flags put the race detection on hold, so this will block.
            //
            // When it unblocks, it will use one GID, filling the window again.
            loggingEngine.log(EventType.WRITE, LOC_ID, ADDR, ADDR, VALUE, VALUE);
        });
        t1.start();
        while (!controlFlags.hasStartedRaceDetection) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        Thread t2 = new Thread(() -> {
            // This will try to use one GID, which does not fit in the window anymore.
            // However, this will not trigger race detection, since it is already running.
            // Instead, it will do a busy wait until race detection finishes.
            //
            // When race detection finishes, the GID will still not fit in the window,
            // but it will trigger a race detection on itself.
            loggingEngine.log(EventType.WRITE, LOC_ID, ADDR, ADDR, VALUE, VALUE);
        });
        t2.start();
        controlFlags.canFinishRaceDetection = true;
        t1.join();
        // One second should be enough for it to finish, but another way to detect whether
        // it deadlocked would be nice.
        t2.join(1000);
        Assert.assertTrue(!t2.isAlive());
    }

    @Test
    public void noRecursiveLogging() throws InterruptedException {
        Configuration configuration = mock(Configuration.class);
        configuration.windowSize = 1;

        Metadata metadata = mock(Metadata.class);

        VolatileLoggingEngine loggingEngine = new VolatileLoggingEngine(configuration, metadata) {
            @Override
            protected void runRaceDetection(int numOfEvents) {
                // The window is already full, so if the logging engine
                // does not reject this log, it will try to get a GID and it will
                // enter an infinite loop.
                log(EventType.WRITE, LOC_ID, ADDR, ADDR, VALUE, VALUE);
            }
        };

        Thread t1 = new Thread(() -> {
            // This will use one GID, filling the window.
            loggingEngine.log(EventType.WRITE, LOC_ID, ADDR, ADDR, VALUE, VALUE);
            // This will try to use one GID, which does not fit in the window anymore,
            // so it will trigger race detection, cleaning the window afterwards.
            loggingEngine.log(EventType.WRITE, LOC_ID, ADDR, ADDR, VALUE, VALUE);
        });
        t1.start();
        // One second should be enough for it to finish, but another way to detect whether
        // it deadlocked would be nice.
        t1.join(1000);
        Assert.assertTrue(!t1.isAlive());
    }

    private static class ControlFlags {
        private volatile boolean canFinishRaceDetection = true;
        private volatile boolean hasStartedRaceDetection = false;
    }
}
