package com.runtimeverification.rvpredict.smt.constraintsources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.smt.ConstraintSource;
import com.runtimeverification.rvpredict.smt.ConstraintType;
import com.runtimeverification.rvpredict.smt.ModelConstraint;
import com.runtimeverification.rvpredict.smt.VariableSource;
import com.runtimeverification.rvpredict.testutils.ModelConstraintUtils;
import com.runtimeverification.rvpredict.trace.ThreadType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.runtimeverification.rvpredict.testutils.ModelConstraintUtils.mockVariableSource;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SignalStartMaskTest {
    private static final int TTID_1 = 100;
    private static final int SIGNAL_TTID_2 = 101;
    private static final int TTID_3 = 102;

    private static final long SIGNAL_NUMBER_2 = 3;
    private static final long MASK_WITH_SIGNAL_2 = 1L << 3;
    private static final long MASK_WITHOUT_SIGNAL_2 = ~MASK_WITH_SIGNAL_2;
    private static final long MASK_WITH_ALL_SIGNALS = ~0L;

    private static final long SIGNAL_HANDLER_2 = 200;
    private static final String ENABLE_2_SIGEST_50 = "o50";
    private static final String DISABLE_ALL_SIGEST_51 = "o51";
    private static final String ENABLE_2_60 = "o60";
    private static final String DISABLE_2_70 = "o70";

    @Mock private Function<Integer, ThreadType> mockTtidToType;
    @Mock private Function<Integer, Long> mockTtidToSignalNumber;
    @Mock private Function<Integer, Long> mockTtidToSignalHandler;
    @Mock private Function<Integer, Boolean> mockSignalStartsInWindow;
    @Mock private Function<Integer, Boolean> mockSignalEndsInWindow;
    @Mock private BiFunction<Integer, Long, Optional<Boolean>> mockSignalEnabledAtStart;
    @Mock private BiFunction<Long, Long, List<ReadonlyEventInterface>> mockEstablishSignalEvents;
    @Mock private BiFunction<Long, Long, Optional<ReadonlyEventInterface>> mockPreviousWindowEstablishEvent;

    @Mock private ReadonlyEventInterface mockEvent1;
    @Mock private ReadonlyEventInterface mockEvent2;
    @Mock private ReadonlyEventInterface mockEvent3;
    @Mock private ReadonlyEventInterface mockEvent4;
    @Mock private ReadonlyEventInterface mockEvent5;
    @Mock private ReadonlyEventInterface mockEvent6;

    @Mock private ReadonlyEventInterface mockEnable2Sigest50;
    @Mock private ReadonlyEventInterface mockDisableAllSigest51;

    @Mock private ReadonlyEventInterface mockEnable2_60;

    @Mock private ReadonlyEventInterface mockDisable2_70;

    @Before
    public void setUp() {
        when(mockTtidToType.apply(TTID_1)).thenReturn(ThreadType.THREAD);
        when(mockTtidToType.apply(SIGNAL_TTID_2)).thenReturn(ThreadType.SIGNAL);

        when(mockTtidToSignalNumber.apply(SIGNAL_TTID_2)).thenReturn(SIGNAL_NUMBER_2);
        when(mockTtidToSignalHandler.apply(SIGNAL_TTID_2)).thenReturn(SIGNAL_HANDLER_2);

        when(mockEstablishSignalEvents.apply(any(), any())).thenReturn(Collections.emptyList());
        when(mockPreviousWindowEstablishEvent.apply(any(), any())).thenReturn(Optional.empty());

        when(mockSignalEnabledAtStart.apply(any(), any())).thenReturn(Optional.of(false));

        when(mockEvent1.getEventId()).thenReturn(1L);
        when(mockEvent2.getEventId()).thenReturn(2L);
        when(mockEvent3.getEventId()).thenReturn(3L);
        when(mockEvent4.getEventId()).thenReturn(4L);
        when(mockEvent5.getEventId()).thenReturn(5L);
        when(mockEvent6.getEventId()).thenReturn(6L);

        when(mockEnable2Sigest50.getEventId()).thenReturn(50L);
        when(mockDisableAllSigest51.getEventId()).thenReturn(51L);

        when(mockEnable2_60.getEventId()).thenReturn(60L);

        when(mockDisable2_70.getEventId()).thenReturn(70L);

        when(mockEvent1.getType()).thenReturn(EventType.WRITE);
        when(mockEvent2.getType()).thenReturn(EventType.WRITE);
        when(mockEvent3.getType()).thenReturn(EventType.WRITE);
        when(mockEvent4.getType()).thenReturn(EventType.WRITE);
        when(mockEvent5.getType()).thenReturn(EventType.WRITE);
        when(mockEvent6.getType()).thenReturn(EventType.WRITE);

        when(mockEnable2Sigest50.getType()).thenReturn(EventType.ESTABLISH_SIGNAL);
        when(mockDisableAllSigest51.getType()).thenReturn(EventType.ESTABLISH_SIGNAL);

        when(mockEnable2_60.getType()).thenReturn(EventType.UNBLOCK_SIGNALS);

        when(mockDisable2_70.getType()).thenReturn(EventType.BLOCK_SIGNALS);

        when(mockEnable2Sigest50.isSignalEvent()).thenReturn(Boolean.TRUE);
        when(mockDisableAllSigest51.isSignalEvent()).thenReturn(Boolean.TRUE);
        when(mockEnable2_60.isSignalEvent()).thenReturn(Boolean.TRUE);
        when(mockDisable2_70.isSignalEvent()).thenReturn(Boolean.TRUE);

        when(mockEnable2Sigest50.getFullWriteSignalMask()).thenReturn(MASK_WITHOUT_SIGNAL_2);
        when(mockDisableAllSigest51.getFullWriteSignalMask()).thenReturn(MASK_WITH_ALL_SIGNALS);

        when(mockEnable2_60.getPartialSignalMask()).thenReturn(MASK_WITH_SIGNAL_2);
        when(mockDisable2_70.getPartialSignalMask()).thenReturn(MASK_WITH_SIGNAL_2);
    }

    @Test
    public void trueWithoutEvents() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of();
        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(mockVariableSource()));
    }

    @Test
    public void trueWithoutSignals() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEvent2));
        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(mockVariableSource()));
    }

    @Test
    public void oneSignalInterruptingThread() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                "sm_101_3", "0",
                "citv101", "100",
                "cidv100", "0", "cidv101", "1")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                "sm_101_3", "1",
                "citv101", "100",
                "cidv100", "0", "cidv101", "1")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                "sm_101_3", "0",
                "citv101", "101",
                "cidv100", "0", "cidv101", "1")));
    }

    @Test
    public void oneSignalInterruptingThreadEnabledOnTheThread() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEnabledAtStart.apply(TTID_1, SIGNAL_NUMBER_2)).thenReturn(Optional.of(Boolean.TRUE));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource("citv101", "100", "cidv100", "0", "cidv101", "1")
        );
    }

    @Test
    public void oneSignalInterruptingThreadEnabledWithSigset() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEnabledAtStart.apply(TTID_1, SIGNAL_NUMBER_2)).thenReturn(Optional.of(Boolean.TRUE));

        when(mockEstablishSignalEvents.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(ImmutableList.of(mockEnable2Sigest50, mockDisableAllSigest51));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        Consumer<VariableSource> assertUnconstrained = mockVariableSource -> assertSignalCanBeEnabedOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource, "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
        Consumer<VariableSource> assertSignalDisabled = mockVariableSource -> assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource,
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );

        assertUnconstrained.accept(ModelConstraintUtils.orderedEvents(
                ENABLE_2_SIGEST_50, "o3", "o4", DISABLE_ALL_SIGEST_51));
        assertUnconstrained.accept(ModelConstraintUtils.orderedEvents(
                ENABLE_2_SIGEST_50, "o3", DISABLE_ALL_SIGEST_51, "o4"));
        assertSignalDisabled.accept(ModelConstraintUtils.orderedEvents(
                ENABLE_2_SIGEST_50, DISABLE_ALL_SIGEST_51, "o3", "o4"));
        assertSignalDisabled.accept(ModelConstraintUtils.orderedEvents(
                "o3", ENABLE_2_SIGEST_50, "o4", DISABLE_ALL_SIGEST_51));
        assertUnconstrained.accept(ModelConstraintUtils.orderedEvents(
                DISABLE_ALL_SIGEST_51, ENABLE_2_SIGEST_50, "o3", "o4"));
        assertSignalDisabled.accept(ModelConstraintUtils.orderedEvents(
                DISABLE_ALL_SIGEST_51, "o3", "o4", ENABLE_2_SIGEST_50));
    }

    @Test
    public void oneSignalInterruptingThreadEnabledOnlyWithSigset() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockEstablishSignalEvents.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(ImmutableList.of(mockEnable2Sigest50, mockDisableAllSigest51));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_SIGEST_50, "o3", "o4", DISABLE_ALL_SIGEST_51),
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
    }

    @Test
    public void oneSignalInterruptingThreadEnabledWithSigsetAndEnableEventOnThread() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEnable2_60, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockEstablishSignalEvents.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(ImmutableList.of(mockEnable2Sigest50, mockDisableAllSigest51));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_SIGEST_50, "o3", "o4", ENABLE_2_60, DISABLE_ALL_SIGEST_51),
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
        assertSignalCanBeEnabedOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_SIGEST_50, ENABLE_2_60, "o3", "o4", DISABLE_ALL_SIGEST_51),
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
    }

    @Test
    public void signalCanInterruptOnlyAfterEnablingSigset() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEnable2_60, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockEstablishSignalEvents.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(ImmutableList.of(mockEnable2Sigest50, mockDisableAllSigest51));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        Consumer<VariableSource> assertSignalDisabled = mockVariableSource -> assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource,
                        "citv101", "100")
        );
        Consumer<VariableSource> assertUnconstrained = mockVariableSource -> assertSignalCanBeEnabedOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource, "citv101", "100")
        );

        assertSignalDisabled.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, "o3", "o4", ENABLE_2_SIGEST_50, DISABLE_ALL_SIGEST_51),
                        "cidv100", "0", "cidv101", "1"));
        assertUnconstrained.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, ENABLE_2_SIGEST_50, "o3", "o4", DISABLE_ALL_SIGEST_51),
                        "cidv100", "0", "cidv101", "1"));
        assertSignalDisabled.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, ENABLE_2_SIGEST_50, DISABLE_ALL_SIGEST_51, "o3", "o4"),
                        "cidv100", "0", "cidv101", "1"));

        assertSignalDisabled.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, "o3", "o4", DISABLE_ALL_SIGEST_51, ENABLE_2_SIGEST_50),
                        "cidv100", "0", "cidv101", "1"));
        assertSignalDisabled.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, DISABLE_ALL_SIGEST_51, "o3", "o4", ENABLE_2_SIGEST_50),
                        "cidv100", "0", "cidv101", "1"));
        assertUnconstrained.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, DISABLE_ALL_SIGEST_51, ENABLE_2_SIGEST_50, "o3", "o4"),
                        "cidv100", "0", "cidv101", "1"));
    }

    @Test
    public void signalEstablishedAndEnabledInThePreviousWindow() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEnable2_60, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockEstablishSignalEvents.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(ImmutableList.of(mockDisableAllSigest51));

        when(mockPreviousWindowEstablishEvent.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(Optional.of(mockEnable2Sigest50));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        Consumer<VariableSource> assertSignalDisabled = mockVariableSource -> assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource,
                        "citv101", "100")
        );
        Consumer<VariableSource> assertUnconstrained = mockVariableSource -> assertSignalCanBeEnabedOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource, "citv101", "100")
        );

        assertUnconstrained.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, "o3", "o4", DISABLE_ALL_SIGEST_51),
                        "cidv100", "0", "cidv101", "1"));
        assertSignalDisabled.accept(
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_60, DISABLE_ALL_SIGEST_51, "o3", "o4"),
                        "cidv100", "0", "cidv101", "1"));
    }

    @Test
    public void signalEstablishedAndDisabledInThePreviousWindow() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEnable2_60, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockEstablishSignalEvents.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(ImmutableList.of(mockEnable2Sigest50));

        when(mockPreviousWindowEstablishEvent.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(Optional.of(mockDisableAllSigest51));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        Consumer<VariableSource> assertSignalDisabled = mockVariableSource -> assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource,
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
        Consumer<VariableSource> assertUnconstrained = mockVariableSource -> assertSignalCanBeEnabedOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(mockVariableSource,
                        "citv101", "100", "cidv100", "0", "cidv101", "1")
        );

        assertSignalDisabled.accept(ModelConstraintUtils.orderedEvents(
                ENABLE_2_60, "o3", "o4", ENABLE_2_SIGEST_50));
        assertUnconstrained.accept(ModelConstraintUtils.orderedEvents(
                ENABLE_2_60, ENABLE_2_SIGEST_50, "o3", "o4"));
    }

    @Test
    public void oneSignalInterruptingThreadEnabledWithSigsetAndEnableDisableEventOnThread() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEnable2_60, mockDisable2_70, mockEvent2),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockEstablishSignalEvents.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(ImmutableList.of(mockEnable2Sigest50, mockDisableAllSigest51));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);
        assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_SIGEST_50, "o3", "o4", ENABLE_2_60, DISABLE_2_70,
                                DISABLE_ALL_SIGEST_51),
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
        assertSignalCanBeEnabedOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_SIGEST_50, ENABLE_2_60, "o3", "o4", DISABLE_2_70,
                                DISABLE_ALL_SIGEST_51),
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
        assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(
                                ENABLE_2_SIGEST_50, ENABLE_2_60, DISABLE_2_70, "o3", "o4",
                                DISABLE_ALL_SIGEST_51),
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1")
        );
    }

    @Test
    public void signalEnabledOnlyOnTheThreadEnablingIt() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                TTID_1, ImmutableList.of(mockEvent1, mockEnable2_60, mockEvent2),
                TTID_3, ImmutableList.of(mockEvent5, mockEvent6),
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockPreviousWindowEstablishEvent.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(Optional.of(mockEnable2Sigest50));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(ENABLE_2_60, "o3", "o4"),
                        "citv101", "102",
                        "cidv102", "0", "cidv101", "1"));
        assertSignalCanBeEnabedOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents(ENABLE_2_60, "o3", "o4"),
                        "citv101", "100",
                        "cidv100", "0", "cidv101", "1"));
    }

    @Test
    public void signalStartedInPreviousWindowEnabled() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEnable2_60, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(false);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockSignalEnabledAtStart.apply(SIGNAL_TTID_2, SIGNAL_NUMBER_2)).thenReturn(Optional.of(true));

        when(mockPreviousWindowEstablishEvent.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(Optional.of(mockEnable2Sigest50));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        VariableSource mockVariableSource = ModelConstraintUtils.orderedEvents("o3", ENABLE_2_60, "o4");
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                mockVariableSource,
                "sm_101_3", "1")));
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                mockVariableSource,
                "sm_101_3", "0")));
    }

    @Test
    public void signalStartedInPreviousWindowDisabled() {
        Map<Integer, List<ReadonlyEventInterface>> eventsByThreadId = ImmutableMap.of(
                SIGNAL_TTID_2, ImmutableList.of(mockEvent3, mockEnable2_60, mockEvent4)
        );

        when(mockSignalStartsInWindow.apply(SIGNAL_TTID_2)).thenReturn(false);
        when(mockSignalEndsInWindow.apply(SIGNAL_TTID_2)).thenReturn(true);

        when(mockSignalEnabledAtStart.apply(SIGNAL_TTID_2, SIGNAL_NUMBER_2)).thenReturn(Optional.of(false));

        when(mockPreviousWindowEstablishEvent.apply(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2))
                .thenReturn(Optional.of(mockEnable2Sigest50));

        ConstraintSource constraintSource = new SignalStartMask(
                eventsByThreadId, mockTtidToType,
                mockTtidToSignalNumber, mockTtidToSignalHandler,
                mockSignalStartsInWindow, mockSignalEndsInWindow,
                mockSignalEnabledAtStart, mockEstablishSignalEvents, mockPreviousWindowEstablishEvent);
        ModelConstraint constraint = constraintSource.createConstraint(ConstraintType.SOUND);

        assertSignalIsDisabledOnThread(
                constraint,
                "sm_101_3",
                mockVariableSource(
                        ModelConstraintUtils.orderedEvents("o3", ENABLE_2_60, "o4")));
    }

    private void assertSignalCanBeEnabedOnThread(
            ModelConstraint constraint,
            String threadEnableRestrictName,
            VariableSource mockVariableSource) {
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                mockVariableSource,
                threadEnableRestrictName, "1")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                mockVariableSource,
                threadEnableRestrictName, "0")));
    }

    private void assertSignalIsDisabledOnThread(
            ModelConstraint constraint,
            String threadEnableRestrictName,
            VariableSource mockVariableSource) {
        Assert.assertFalse(constraint.evaluate(mockVariableSource(
                mockVariableSource,
                threadEnableRestrictName, "1")));
        Assert.assertTrue(constraint.evaluate(mockVariableSource(
                mockVariableSource,
                threadEnableRestrictName, "0")));
    }
}
