package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.testutils.MoreAsserts;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ContextTest {
    private static final long MIN_DELTA_AND_EVENT_TYPE = 1;
    private static final long THREAD_ID = 2;
    private static final long SECOND_THREAD_ID = 5;
    private static final long FIRST_GENERATION = 0;
    private static final long SECOND_GENERATION = 6;
    private static final long THIRD_GENERATION = 9;
    private static final long FOURTH_GENERATION = 17;
    private static final long PROGRAM_COUNTER = 3;
    private static final long SECOND_PROGRAM_COUNTER = 8;
    private static final long THIRD_PROGRAM_COUNTER = 13;
    private static final long FOURTH_PROGRAM_COUNTER = 18;
    private static final int PROGRAM_COUNTER_DELTA = 4;
    private static final long SIGNAL_NUMBER = 10;
    private static final long SECOND_SIGNAL_NUMBER = 11;
    private static final long THIRD_SIGNAL_NUMBER = 12;
    private static final long SIGNAL_MASK_NUMBER = 14;
    private static final long SECOND_SIGNAL_MASK_NUMBER = 16;
    private static final long SIGNAL_MASK = 15;

    @Test
    public void firstThreadStart() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Constants.INVALID_PROGRAM_COUNTER, context.getPC());
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
        context.startThread(SECOND_THREAD_ID);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());

        context.beginThread(SECOND_THREAD_ID, SECOND_GENERATION);
        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(Constants.INVALID_PROGRAM_COUNTER, context.getPC());
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
        context.startThread(SECOND_THREAD_ID);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());

        context.beginThread(SECOND_THREAD_ID, SECOND_GENERATION);
        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(Constants.INVALID_PROGRAM_COUNTER, context.getPC());

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
        context.startThread(SECOND_THREAD_ID);

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
        context.startThread(SECOND_THREAD_ID);

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

        assertDistinct(
                t1_g1_op1_id, t1_g1_op2_id, t1_g1_op3_id, t1_g2_op4_id, t1_g3_op5_id,
                t2_g2_op1_id, t2_g2_op2_id, t2_g2_op3_id, t2_g3_op4_id
        );
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
    public void signalNumberGenerationPCForOneSignalLifetime() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.enterSignal(SIGNAL_NUMBER, SECOND_GENERATION);
        context.jump(SECOND_PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.exitSignal();

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());
    }

    @Test
    public void signalNumberGenerationPCWhenStartingAndEndingTwoSignals() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.enterSignal(SIGNAL_NUMBER, SECOND_GENERATION);
        context.jump(SECOND_PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.enterSignal(SECOND_SIGNAL_NUMBER, THIRD_GENERATION);
        context.jump(THIRD_PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SECOND_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(THIRD_GENERATION, context.getGeneration());
        Assert.assertEquals(THIRD_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(2, context.getSignalDepth());

        context.exitSignal();

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.exitSignal();
        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());
    }

    @Test
    public void signalNumberGenerationPCWhenStartingAndEndingSignalsOutOfOrder() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.enterSignal(SIGNAL_NUMBER, SECOND_GENERATION);
        context.jump(SECOND_PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.setSignalDepth(2);
        context.enterSignal(SECOND_SIGNAL_NUMBER, THIRD_GENERATION);
        context.jump(THIRD_PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SECOND_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(THIRD_GENERATION, context.getGeneration());
        Assert.assertEquals(THIRD_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(3, context.getSignalDepth());

        context.exitSignal();
        context.setSignalDepth(1);
        context.enterSignal(THIRD_SIGNAL_NUMBER, FOURTH_GENERATION);
        context.jump(FOURTH_PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(THIRD_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FOURTH_GENERATION, context.getGeneration());
        Assert.assertEquals(FOURTH_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(2, context.getSignalDepth());

        context.exitSignal();

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.exitSignal();

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());
    }


    @Test
    public void signalNumberGenerationPCWhenSwitchingThreads() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        context.jump(PROGRAM_COUNTER);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.startThread(SECOND_THREAD_ID);
        context.beginThread(SECOND_THREAD_ID, SECOND_GENERATION);
        context.jump(SECOND_PROGRAM_COUNTER);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.enterSignal(SIGNAL_NUMBER, THIRD_GENERATION);
        context.jump(THIRD_PROGRAM_COUNTER);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(THIRD_GENERATION, context.getGeneration());
        Assert.assertEquals(THIRD_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.switchThread(THREAD_ID);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        Assert.assertEquals(THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(FIRST_GENERATION, context.getGeneration());
        Assert.assertEquals(PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.switchThread(SECOND_THREAD_ID);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.setSignalDepth(1);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(THIRD_GENERATION, context.getGeneration());
        Assert.assertEquals(THIRD_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(THIRD_GENERATION, context.getGeneration());
        Assert.assertEquals(THIRD_PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());
        Assert.assertEquals(1, context.getSignalDepth());

        context.exitSignal();

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());

        context.updatePcWithDelta(PROGRAM_COUNTER_DELTA);

        Assert.assertEquals(SECOND_THREAD_ID, context.getThreadId());
        Assert.assertEquals(Context.INVALID_SIGNAL_NUMBER, context.getSignalNumber());
        Assert.assertEquals(SECOND_GENERATION, context.getGeneration());
        Assert.assertEquals(SECOND_PROGRAM_COUNTER + PROGRAM_COUNTER_DELTA, context.getPC());
        Assert.assertEquals(0, context.getSignalDepth());
    }

    @Test
    public void exceptionWhenUpdatingInvalidPC() throws InvalidTraceDataException {
        Context context = new Context(MIN_DELTA_AND_EVENT_TYPE);
        context.beginThread(THREAD_ID, FIRST_GENERATION);
        MoreAsserts.assertException(
                InvalidTraceDataException.class,
                "invalid program counter",
                () -> context.updatePcWithDelta(PROGRAM_COUNTER_DELTA));
    }

    private static void assertLower(long first, long second) {
        Assert.assertTrue(first + " should be lower than " + second, first < second);
    }

    private void assertDistinct(long... elements) {
        Assert.assertTrue(
                "Some of these elements are not distinct" + Arrays.toString(elements),
                Arrays.stream(elements).boxed().collect(Collectors.toSet()).size() == elements.length);
    }
}
