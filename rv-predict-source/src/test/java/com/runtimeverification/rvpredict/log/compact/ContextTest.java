package com.runtimeverification.rvpredict.log.compact;

import org.junit.Assert;
import org.junit.Test;

public class ContextTest {
    private static final long MIN_DELTA_AND_EVENT_TYPE = 1;
    private static final long THREAD_ID = 2;
    private static final long SECOND_THREAD_ID = 5;
    private static final long FIRST_GENERATION = 0;
    private static final long SECOND_GENERATION = 6;
    private static final long THIRD_GENERATION = 9;
    private static final long PROGRAM_COUNTER = 3;
    private static final long SECOND_PROGRAM_COUNTER = 8;
    private static final int PROGRAM_COUNTER_DELTA = 4;
    private static final long SIGNAL_HANDLER_ADDRESS = 10;
    private static final long SECOND_SIGNAL_HANDLER_ADDRESS = 11;
    private static final long SIGNAL_NUMBER = 12;
    private static final long SECOND_SIGNAL_NUMBER = 13;
    private static final long SIGNAL_MASK_NUMBER = 14;
    private static final long SECOND_SIGNAL_MASK_NUMBER = 14;
    private static final long SIGNAL_MASK = 15;

    @Test
    public void firstThreadStart() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(MIN_DELTA_AND_EVENT_TYPE, context.getPC());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
    }

    @Test
    public void jump() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);

        long firstId = context.newId();

        context.jump(PROGRAM_COUNTER);
        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());

        long secondId = context.newId();

        assertLower(firstId, secondId);
    }

    @Test
    public void simpleInstruction() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);

        long firstId = context.newId();

        context.jump(PROGRAM_COUNTER);
        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());

        long secondId = context.newId();

        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);
        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());

        long thirdId = context.newId();

        assertLower(firstId, secondId);
        assertLower(secondId, thirdId);
    }

    @Test
    public void startSecondThread() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);

        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);
        context.forkThread(SECOND_THREAD_ID);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());

        context.beginThread(SECOND_THREAD_ID, SECOND_GENERATION);
        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(MIN_DELTA_AND_EVENT_TYPE, context.getPC());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());

        context.jump(SECOND_PROGRAM_COUNTER);
        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
    }

    @Test
    public void switchThreads() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);

        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);
        context.forkThread(SECOND_THREAD_ID);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());

        context.beginThread(SECOND_THREAD_ID, SECOND_GENERATION);
        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(MIN_DELTA_AND_EVENT_TYPE, context.getPC());

        context.jump(SECOND_PROGRAM_COUNTER);
        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());

        context.switchThread(THREAD_ID);
        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());
    }

    @Test
    public void generationsAreThreadSpecific() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);

        context.beginThread(THREAD_ID, FIRST_GENERATION);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());

        context.jump(PROGRAM_COUNTER);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);
        context.forkThread(SECOND_THREAD_ID);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());

        context.beginThread(SECOND_THREAD_ID, SECOND_GENERATION);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());

        context.jump(SECOND_PROGRAM_COUNTER);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());

        context.switchThread(THREAD_ID);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());

        context.changeOfGeneration(SECOND_GENERATION);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());

        context.changeOfGeneration(THIRD_GENERATION);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(THIRD_GENERATION, context.getGeneration());

        context.switchThread(SECOND_THREAD_ID);
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
    }

    @Test
    public void idsAreOrderedByGenerations() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);

        context.beginThread(THREAD_ID, FIRST_GENERATION);
        long t1_g1_op1_id = context.newId();

        context.jump(PROGRAM_COUNTER);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);
        context.forkThread(SECOND_THREAD_ID);

        long t1_g1_op2_id = context.newId();

        context.beginThread(SECOND_THREAD_ID, SECOND_GENERATION);

        long t2_g2_op1_id = context.newId();

        context.jump(SECOND_PROGRAM_COUNTER);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        long t2_g2_op2_id = context.newId();

        context.switchThread(THREAD_ID);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        long t1_g1_op3_id = context.newId();

        context.changeOfGeneration(SECOND_GENERATION);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        long t1_g2_op4_id = context.newId();

        context.changeOfGeneration(THIRD_GENERATION);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        long t1_g3_op5_id = context.newId();

        context.switchThread(SECOND_THREAD_ID);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        long t2_g2_op3_id = context.newId();

        context.changeOfGeneration(THIRD_GENERATION);
        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        long t2_g3_op4_id = context.newId();

        // Event ordering on the first thread.
        assertLower(t1_g1_op1_id, t1_g1_op2_id);
        assertLower(t1_g1_op2_id, t1_g1_op3_id);
        assertLower(t1_g1_op3_id, t1_g2_op4_id);
        assertLower(t1_g2_op4_id, t1_g3_op5_id);

        // Event ordering on the second thread
        assertLower(t2_g2_op1_id, t2_g2_op2_id);
        assertLower(t2_g2_op2_id, t2_g2_op3_id);
        assertLower(t2_g2_op3_id, t2_g3_op4_id);

        // Event ordering between the first and second generation.
        assertLower(t1_g1_op3_id, t2_g2_op1_id);

        // Event ordering between the second and third generation.
        assertLower(t1_g2_op4_id, t2_g3_op4_id);
        assertLower(t2_g2_op3_id, t1_g3_op5_id);
    }

    @Test
    public void signalMaskMemoization() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());

        context.memoizeSignalMask(SIGNAL_MASK, 0, SIGNAL_MASK_NUMBER);
        context.memoizeSignalMask(SIGNAL_MASK, 2, SECOND_SIGNAL_MASK_NUMBER);

        Assert.assertEquals(SIGNAL_MASK, context.getMemoizedSignalMask(SIGNAL_MASK_NUMBER));
        Assert.assertEquals(SIGNAL_MASK << 2, context.getMemoizedSignalMask(SECOND_SIGNAL_MASK_NUMBER));
    }

    @Test
    public void noSignalNumberWhenNoSignalIsRunning() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
    }

    @Test
    public void signalHandlersUseSigSetMasks() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());

        context.memoizeSignalMask(SIGNAL_MASK, 0, SIGNAL_MASK_NUMBER);
        context.memoizeSignalMask(SIGNAL_MASK, 2, SECOND_SIGNAL_MASK_NUMBER);

        context.establishSignal(SIGNAL_HANDLER_ADDRESS, SIGNAL_NUMBER, SIGNAL_MASK_NUMBER);
        context.establishSignal(SECOND_SIGNAL_HANDLER_ADDRESS, SECOND_SIGNAL_NUMBER, SECOND_SIGNAL_MASK_NUMBER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());

        context.enterSignal(SIGNAL_NUMBER, SECOND_GENERATION);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SIGNAL_MASK, context.getSignalMask());

        context.exitSignal();

        context.enterSignal(SECOND_SIGNAL_NUMBER, THIRD_GENERATION);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SIGNAL_MASK << 2, context.getSignalMask());

        context.exitSignal();
    }

    private static void assertLower(long first, long second) {
        Assert.assertTrue(first + " should be lower than " + second, first < second);
    }
}
