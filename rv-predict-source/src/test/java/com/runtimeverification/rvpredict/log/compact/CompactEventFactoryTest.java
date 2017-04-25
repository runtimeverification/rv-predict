package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompactEventFactoryTest {
    private static final int DATA_SIZE_IN_BYTES = 2;
    private static final long ADDRESS = 1234;
    private static final long VALUE = 5678;
    private static final long OTHER_VALUE = 5679;
    private static final long NEW_ID = 100;
    private static final long THREAD_ID = 1;
    private static final long OTHER_THREAD_ID = 5;
    private static final long GENERATION = 1000;
    private static final long SIGNAL_HANDLER = 1001;
    private static final long SIGNAL_NUMBER = 1002;
    private static final long SIGNAL_MASK_NUMBER = 1003;
    private static final long SIGNAL_MASK = 1004;
    private static final int SIGNAL_DEPTH = 3;
    private static final long ORIGIN_BIT_COUNT = 4;

    private static final List<CompactEventMethod> ALL_METHODS = new ArrayList<>();

    private static final CompactEventMethod<Long> GET_ID =
            new CompactEventMethod<>(ALL_METHODS, "getEventId", CompactEvent::getEventId);
    private static final CompactEventMethod<Long> GET_THREAD_ID =
            new CompactEventMethod<>(ALL_METHODS, "getThreadId", CompactEvent::getThreadId);
    private static final CompactEventMethod<EventType> GET_COMPACT_TYPE =
            new CompactEventMethod<>(ALL_METHODS, "getType", CompactEvent::getType);
    private static final CompactEventMethod<Integer> GET_DATA_SIZE_IN_BYTES =
            new CompactEventMethod<>(ALL_METHODS, "getDataSizeInBytes", CompactEvent::getDataSizeInBytes);
    private static final CompactEventMethod<Long> GET_DATA_ADDRESS =
            new CompactEventMethod<>(ALL_METHODS, "getAddress", CompactEvent::getDataAddress);
    private static final CompactEventMethod<Long> GET_DATA_VALUE =
            new CompactEventMethod<>(ALL_METHODS, "getDataValue", CompactEvent::getDataValue);
    private static final CompactEventMethod<Long> GET_LOCK_ADDRESS =
            new CompactEventMethod<>(ALL_METHODS, "getSyncObject", CompactEvent::getSyncObject);
    private static final CompactEventMethod<Long> GET_OTHER_THREAD_ID =
            new CompactEventMethod<>(ALL_METHODS, "getSyncedThreadId", CompactEvent::getSyncedThreadId);
    private static final CompactEventMethod<Long> GET_SIGNAL_HANDLER_ADDRESS =
            new CompactEventMethod<>(ALL_METHODS, "getSignalHandlerAddress", CompactEvent::getSignalHandlerAddress);
    private static final CompactEventMethod<Long> GET_SIGNAL_MASK =
            new CompactEventMethod<>(ALL_METHODS, "getSignalMask", CompactEvent::getSignalMask);
    private static final CompactEventMethod<Long> GET_SIGNAL_NUMBER =
            new CompactEventMethod<>(ALL_METHODS, "getSignalNumber", CompactEvent::getSignalNumber);

    @Mock private Context mockContext;

    @Test
    public void readNonatomicData() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.dataManipulation(
                mockContext,
                CompactEventReader.DataManipulationType.LOAD,
                DATA_SIZE_IN_BYTES,
                ADDRESS,
                VALUE,
                CompactEventReader.Atomicity.NOT_ATOMIC);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.READ, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(DATA_SIZE_IN_BYTES, GET_DATA_SIZE_IN_BYTES),
                        new ReturnValueTest<>(ADDRESS, GET_DATA_ADDRESS),
                        new ReturnValueTest<>(VALUE, GET_DATA_VALUE),
                }
        );
    }

    @Test
    public void writeNonatomicData() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.dataManipulation(
                mockContext,
                CompactEventReader.DataManipulationType.STORE,
                DATA_SIZE_IN_BYTES,
                ADDRESS,
                VALUE,
                CompactEventReader.Atomicity.NOT_ATOMIC);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(DATA_SIZE_IN_BYTES, GET_DATA_SIZE_IN_BYTES),
                        new ReturnValueTest<>(ADDRESS, GET_DATA_ADDRESS),
                        new ReturnValueTest<>(VALUE, GET_DATA_VALUE),
                }
        );
    }

    @Test
    public void readAtomicData() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID).thenReturn(NEW_ID + 1).thenReturn(NEW_ID + 2);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.dataManipulation(
                mockContext,
                CompactEventReader.DataManipulationType.LOAD,
                DATA_SIZE_IN_BYTES,
                ADDRESS,
                VALUE,
                CompactEventReader.Atomicity.ATOMIC);

        Assert.assertEquals(3, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_LOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
        event = events.get(1);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID + 1, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.READ, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(DATA_SIZE_IN_BYTES, GET_DATA_SIZE_IN_BYTES),
                        new ReturnValueTest<>(ADDRESS, GET_DATA_ADDRESS),
                        new ReturnValueTest<>(VALUE, GET_DATA_VALUE),
                }
        );
        event = events.get(2);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID + 2, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_UNLOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
    }

    @Test
    public void writeAtomicData() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID).thenReturn(NEW_ID + 1).thenReturn(NEW_ID + 2);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.dataManipulation(
                mockContext,
                CompactEventReader.DataManipulationType.LOAD,
                DATA_SIZE_IN_BYTES,
                ADDRESS,
                VALUE,
                CompactEventReader.Atomicity.ATOMIC);

        Assert.assertEquals(3, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_LOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
        event = events.get(1);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID + 1, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.READ, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(DATA_SIZE_IN_BYTES, GET_DATA_SIZE_IN_BYTES),
                        new ReturnValueTest<>(ADDRESS, GET_DATA_ADDRESS),
                        new ReturnValueTest<>(VALUE, GET_DATA_VALUE),
                }
        );
        event = events.get(2);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID + 2, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_UNLOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
    }

    @Test
    public void atomicReadModifyWrite() throws InvalidTraceDataException {
        when(mockContext.newId())
                .thenReturn(NEW_ID).thenReturn(NEW_ID + 1).thenReturn(NEW_ID + 2).thenReturn(NEW_ID + 3);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.atomicReadModifyWrite(
                mockContext,
                DATA_SIZE_IN_BYTES,
                ADDRESS,
                VALUE,
                OTHER_VALUE);

        Assert.assertEquals(4, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_LOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
        event = events.get(1);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID + 1, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.READ, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(DATA_SIZE_IN_BYTES, GET_DATA_SIZE_IN_BYTES),
                        new ReturnValueTest<>(ADDRESS, GET_DATA_ADDRESS),
                        new ReturnValueTest<>(VALUE, GET_DATA_VALUE),
                }
        );
        event = events.get(2);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID + 2, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(DATA_SIZE_IN_BYTES, GET_DATA_SIZE_IN_BYTES),
                        new ReturnValueTest<>(ADDRESS, GET_DATA_ADDRESS),
                        new ReturnValueTest<>(OTHER_VALUE, GET_DATA_VALUE),
                }
        );
        event = events.get(3);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID + 3, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_UNLOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
    }

    @Test
    public void changeOfGeneration() throws InvalidTraceDataException {
        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.changeOfGeneration(
                mockContext,
                GENERATION);

        Assert.assertTrue(events.isEmpty());
        verify(mockContext, times(1)).changeOfGeneration(GENERATION);
    }

    @Test
    public void lockManipulation_Lock() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.lockManipulation(
                mockContext,
                CompactEventReader.LockManipulationType.LOCK,
                ADDRESS);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_LOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
    }

    @Test
    public void lockManipulation_Unlock() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.lockManipulation(
                mockContext,
                CompactEventReader.LockManipulationType.UNLOCK,
                ADDRESS);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.WRITE_UNLOCK, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(ADDRESS, GET_LOCK_ADDRESS),
                }
        );
    }

    @Test
    public void jump() throws InvalidTraceDataException {
        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.jump(
                mockContext,
                ADDRESS);

        Assert.assertTrue(events.isEmpty());
        verify(mockContext, times(1)).jump(ADDRESS);
    }

    @Test
    public void establishSignal() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);
        when(mockContext.getMemoizedSignalMask(SIGNAL_MASK_NUMBER)).thenReturn(SIGNAL_MASK);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.establishSignal(
                mockContext,
                SIGNAL_HANDLER,
                SIGNAL_NUMBER,
                SIGNAL_MASK_NUMBER);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.ESTABLISH_SIGNAL, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(SIGNAL_MASK, GET_SIGNAL_MASK),
                        new ReturnValueTest<>(SIGNAL_NUMBER, GET_SIGNAL_NUMBER),
                        new ReturnValueTest<>(SIGNAL_HANDLER, GET_SIGNAL_HANDLER_ADDRESS),
                }
        );

        verify(mockContext, times(1))
                .establishSignal(SIGNAL_HANDLER, SIGNAL_NUMBER, SIGNAL_MASK_NUMBER);
    }

    @Test
    public void disestablishSignal() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.disestablishSignal(
                mockContext,
                SIGNAL_NUMBER);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.DISESTABLISH_SIGNAL, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(SIGNAL_NUMBER, GET_SIGNAL_NUMBER),
                }
        );

        verify(mockContext, times(1)).disestablishSignal(SIGNAL_NUMBER);
    }

    @Test
    public void enterSignal() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.enterSignal(
                mockContext,
                GENERATION,
                SIGNAL_NUMBER);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.ENTER_SIGNAL, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(SIGNAL_NUMBER, GET_SIGNAL_NUMBER),
                }
        );

        verify(mockContext, times(1)).enterSignal(SIGNAL_NUMBER, GENERATION);
    }

    @Test
    public void exitSignal() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);
        when(mockContext.getSignalNumber()).thenReturn(SIGNAL_NUMBER);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.exitSignal(mockContext);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.EXIT_SIGNAL, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(SIGNAL_NUMBER, GET_SIGNAL_NUMBER),
                }
        );

        verify(mockContext, times(1)).exitSignal();
    }

    @Test
    public void signalOutstandingDepth() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.signalOutstandingDepth(
                mockContext,
                SIGNAL_DEPTH);

        Assert.assertTrue(events.isEmpty());

        verify(mockContext, times(1)).setSignalDepth(SIGNAL_DEPTH);
    }

    @Test
    public void signalMaskMemoization() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.signalMaskMemoization(
                mockContext,
                SIGNAL_MASK,
                ORIGIN_BIT_COUNT,
                SIGNAL_MASK_NUMBER);

        Assert.assertTrue(events.isEmpty());

        verify(mockContext, times(1))
                .memoizeSignalMask(SIGNAL_MASK, ORIGIN_BIT_COUNT, SIGNAL_MASK_NUMBER);
    }

    @Test
    public void signalMask() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);
        when(mockContext.getMemoizedSignalMask(SIGNAL_MASK_NUMBER)).thenReturn(SIGNAL_MASK);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.signalMask(
                mockContext,
                SIGNAL_MASK_NUMBER);

        Assert.assertTrue(events.isEmpty());

        verify(mockContext, times(1))
                .maskSignals(SIGNAL_MASK);
    }

    @Test
    public void enterFunction() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.enterFunction(mockContext);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.INVOKE_METHOD, GET_COMPACT_TYPE),
                }
        );
    }

    @Test
    public void exitFunction() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.exitFunction(mockContext);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.FINISH_METHOD, GET_COMPACT_TYPE),
                }
        );
    }

    @Test
    public void beginThread() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.beginThread(
                mockContext,
                THREAD_ID,
                GENERATION);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.BEGIN_THREAD, GET_COMPACT_TYPE),
                }
        );

        verify(mockContext, times(1)).beginThread(THREAD_ID, GENERATION);
    }

    @Test
    public void endThread() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.endThread(
                mockContext);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.END_THREAD, GET_COMPACT_TYPE),
                }
        );

        verify(mockContext, times(1)).endThread();
    }

    @Test
    public void threadSync_Switch() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.threadSync(
                mockContext,
                CompactEventReader.ThreadSyncType.SWITCH,
                THREAD_ID);

        Assert.assertTrue(events.isEmpty());

        verify(mockContext, times(1)).switchThread(THREAD_ID);
    }

    @Test
    public void threadSync_Fork() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.threadSync(
                mockContext,
                CompactEventReader.ThreadSyncType.FORK,
                OTHER_THREAD_ID);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.START_THREAD, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(OTHER_THREAD_ID, GET_OTHER_THREAD_ID),
                }
        );

        verify(mockContext, times(1)).startThread(OTHER_THREAD_ID);
    }

    @Test
    public void threadSync_Join() throws InvalidTraceDataException {
        when(mockContext.newId()).thenReturn(NEW_ID);
        when(mockContext.getThreadId()).thenReturn(THREAD_ID);

        CompactEventFactory eventFactory = new CompactEventFactory();
        List<ReadonlyEventInterface> events = eventFactory.threadSync(
                mockContext,
                CompactEventReader.ThreadSyncType.JOIN,
                OTHER_THREAD_ID);

        Assert.assertEquals(1, events.size());
        ReadonlyEventInterface event = events.get(0);
        testImplementedMethods(
                event,
                new ReturnValueTest[] {
                        new ReturnValueTest<>(NEW_ID, GET_ID),
                        new ReturnValueTest<>(THREAD_ID, GET_THREAD_ID),
                        new ReturnValueTest<>(EventType.JOIN_THREAD, GET_COMPACT_TYPE),
                        new ReturnValueTest<>(OTHER_THREAD_ID, GET_OTHER_THREAD_ID),
                }
        );

        verify(mockContext, times(1)).joinThread(OTHER_THREAD_ID);
    }

    private void testImplementedMethods(ReadonlyEventInterface event, ReturnValueTest[] tests) {
        Assert.assertTrue(
                "The event is an instance of "
                        + event.getClass().getCanonicalName()
                        + " instead of "
                        + CompactEvent.class.getCanonicalName()
                        + ".",
                event instanceof CompactEvent);
        CompactEvent compactEvent = (CompactEvent) event;
        List<CompactEventMethod> implementedMethods = new ArrayList<>();
        for (ReturnValueTest test : tests) {
            test.runTest(implementedMethods, compactEvent);
        }
        for (CompactEventMethod method : ALL_METHODS) {
            if (implementedMethods.contains(method)) {
                continue;
            }
            try {
                method.run(compactEvent);
            } catch (UnsupportedOperationException e) {
                continue;
            }
            Assert.fail(
                    "Expected UnsupportedOperationException for method "
                            + method.getName() + ", but none was thrown.");
        }
    }

    private static class ReturnValueTest<T> {
        private final T expected;
        private final CompactEventMethod<T> method;

        private ReturnValueTest(T expected, CompactEventMethod<T> method) {
            this.expected = expected;
            this.method = method;
        }

        private void runTest(List<CompactEventMethod> executedMethods, CompactEvent event) {
            executedMethods.add(method);
            Assert.assertEquals("Method: " + method.getName(), expected, method.run(event));
        }
    }

    private static class CompactEventMethod<T> {
        private final String name;
        private final Function<CompactEvent, T> method;

        private CompactEventMethod(List<CompactEventMethod> allMethods, String name, Function<CompactEvent, T> method) {
            this.name = name;
            this.method = method;
            allMethods.add(this);
        }

        private T run(CompactEvent event) {
            return method.apply(event);
        }

        private String getName() {
            return name;
        }
    }
}
