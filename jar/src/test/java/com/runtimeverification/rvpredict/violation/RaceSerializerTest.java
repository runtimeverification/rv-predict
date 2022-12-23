package com.runtimeverification.rvpredict.violation;

import com.runtimeverification.error.data.ErrorCategory;
import com.runtimeverification.error.data.Language;
import com.runtimeverification.error.data.RawComponentField;
import com.runtimeverification.error.data.RawField;
import com.runtimeverification.error.data.RawFrame;
import com.runtimeverification.error.data.RawLock;
import com.runtimeverification.error.data.RawStackError;
import com.runtimeverification.error.data.RawStackTrace;
import com.runtimeverification.error.data.RawStackTraceComponent;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.metadata.SignatureProcessor;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isAbsent;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isPresent;
import static com.runtimeverification.rvpredict.testutils.MoreAsserts.isPresentWithValue;
import static com.runtimeverification.rvpredict.testutils.TraceUtils.extractSingleEvent;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RaceSerializerTest {
    private static final long ADDRESS_1 = 200L;
    private static final long VALUE_1 = 300L;
    private static final long BASE_ID = 0L;
    private static final long BASE_PC = 400L;
    private static final long CALL_SITE_ADDRESS_1 = 500L;
    private static final long CALL_SITE_ADDRESS_2 = 501L;
    private static final long CANONICAL_FRAME_ADDRESS_1 = 601L;
    private static final long CANONICAL_FRAME_ADDRESS_2 = 602L;
    private static final long LOCATION_1 = 701L;
    private static final long LOCK_1 = 801L;
    private static final long LOCK_2 = 802L;
    private static final long GENERATION_1 = 901L;
    private static final long SIGNAL_HANDLER_1 = 1001L;
    private static final long SIGNAL_HANDLER_2 = 1002L;
    private static final long SIGNAL_HANDLER_3 = 1003L;
    private static final long SIGNAL_HANDLER_4 = 1004L;
    private static final long THREAD_1 = 1L;
    private static final long THREAD_2 = 2L;
    private static final int NO_SIGNAL = 0;
    private static final int ONE_SIGNAL = 1;
    private static final int TWO_SIGNALS = 2;
    private static final long SIGNAL_NUMBER_1 = 1L;
    private static final long SIGNAL_NUMBER_2 = 2L;
    private static final long SIGNAL_NUMBER_3 = 3L;
    private static final long SIGNAL_NUMBER_4 = 4L;

    private int nextIdDelta = 0;

    @Mock private Configuration mockConfiguration;
    @Mock private SignatureProcessor mockSignatureProcessor;
    @Mock private MetadataInterface mockMetadata;

    @Mock private Context mockContext;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);

        when(mockConfiguration.isExcludedLibrary(anyString())).thenReturn(false);
        when(mockConfiguration.stacks()).thenReturn(true);

        when(mockMetadata.getParentOTID(anyLong())).thenReturn(OptionalLong.empty());
        when(mockMetadata.getOriginalThreadCreationLocId(anyLong())).thenReturn(OptionalLong.empty());
        when(mockMetadata.getLocationPrefix()).thenReturn("loc-prefix ");
    }

    @Test
    public void baseRaceReport() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1;
        List<SignalStackEvent> signalStack2;

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Optional<RawStackError> maybeError = serializer.generateErrorData(
                e1, e2,
                signalStack1 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace)),
                signalStack2 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace)),
                false);

        Assert.assertTrue(maybeError.isPresent());
        Assert.assertEquals("Data race on %s", maybeError.get().description_format);
        Assert.assertEquals(1, maybeError.get().description_fields.size());
        {
            RawField field = maybeError.get().description_fields.get(0);
            Assert.assertEquals("e1-e2-race-data-sig", field.address);
            Assert.assertNull(field.frame1);
            Assert.assertNull(field.frame2);
        }
        Assert.assertEquals("CEER4", maybeError.get().error_id);

        Assert.assertEquals(ErrorCategory.Tag.UNDEFINED, maybeError.get().category.tag());
        Assert.assertEquals(Language.Tag.C, maybeError.get().category.getUndefined().tag());

        Assert.assertEquals(2, maybeError.get().stack_traces.size());
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
            Assert.assertNull(stackTrace.thread_created_by);
            Assert.assertEquals("1", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Read in thread 1", stackTraceComponent.description_format);
                Assert.assertEquals(0, stackTraceComponent.description_fields.size());
                Assert.assertEquals(1, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e1-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
            Assert.assertNull(stackTrace.thread_created_at);
        }
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
            Assert.assertNull(stackTrace.thread_created_by);
            Assert.assertEquals("2", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Write in thread 2", stackTraceComponent.description_format);
                Assert.assertEquals(0, stackTraceComponent.description_fields.size());
                Assert.assertEquals(1, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
            Assert.assertNull(stackTrace.thread_created_at);
        }

        Assert.assertThat(
                generateMemReport(signalStack1, serializer),
                isPresentWithValue(
                    "    Read in thread 1\n" +
                    "      > loc-prefix e1-location-sig\n" +
                    "    Thread 1 is the main thread\n"));
        Assert.assertThat(
                generateMemReport(signalStack2, serializer),
                isPresentWithValue(
                    "    Write in thread 2\n" +
                    "      > loc-prefix e2-location-sig\n" +
                    "    Thread 2 was created by n/a\n"));
    }

    @Test
    public void addsRaceDescription() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1;
        List<SignalStackEvent> signalStack2;

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Optional<RawStackError> maybeError = serializer.generateErrorData(
                e1, e2,
                signalStack1 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace)),
                signalStack2 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace)),
                false);

        Assert.assertTrue(maybeError.isPresent());
        Assert.assertEquals("Data race on %s", maybeError.get().description_format);
        Assert.assertEquals(1, maybeError.get().description_fields.size());
        {
            RawField field = maybeError.get().description_fields.get(0);
            Assert.assertEquals("e1-e2-race-data-sig", field.address);
            Assert.assertNull(field.frame1);
            Assert.assertNull(field.frame2);
        }

        Assert.assertThat(
                generateMemReport(signalStack1, serializer),
                isPresentWithValue(
                        "    Read in thread 1\n" +
                                "      > loc-prefix e1-location-sig\n" +
                                "    Thread 1 is the main thread\n"));
        Assert.assertThat(
                generateMemReport(signalStack2, serializer),
                isPresentWithValue(
                        "    Write in thread 2\n" +
                                "      > loc-prefix e2-location-sig\n" +
                                "    Thread 2 was created by n/a\n"));
    }

    @Test
    public void doesNotReportRaceWhenAllLibrariesAreExcluded() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;
        List<ReadonlyEventInterface> f1List;
        List<ReadonlyEventInterface> f2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        f1List =tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, OptionalLong.of(CALL_SITE_ADDRESS_1)),
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        f2List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, OptionalLong.of(CALL_SITE_ADDRESS_2)),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);
        ReadonlyEventInterface f1 = extractSingleEvent(f1List);
        ReadonlyEventInterface f2 = extractSingleEvent(f2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getLocationSig(f1.getLocationId())).thenReturn("f1-location-sig");
        when(mockMetadata.getLocationSig(f2.getLocationId())).thenReturn("f2-location-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace));
        List<SignalStackEvent> signalStack2 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace));

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Supplier<Optional<RawStackError>> errorSupplier =
                () -> serializer.generateErrorData(
                    e1, e2,
                    signalStack1,
                    signalStack2,
                    false);

        when(mockConfiguration.isExcludedLibrary("e1-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("e2-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("f1-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("f2-location-sig")).thenReturn(false);
        Assert.assertThat(errorSupplier.get(), isPresent());
        Assert.assertThat(generateMemReport(signalStack1, serializer), isPresent());
        Assert.assertThat(generateMemReport(signalStack2, serializer), isPresent());

        when(mockConfiguration.isExcludedLibrary("e1-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("e2-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("f1-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("f2-location-sig")).thenReturn(false);
        Assert.assertThat(errorSupplier.get(), isPresent());
        Assert.assertThat(generateMemReport(signalStack1, serializer), isPresent());
        Assert.assertThat(generateMemReport(signalStack2, serializer), isPresent());

        when(mockConfiguration.isExcludedLibrary("e1-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("e2-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("f1-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("f2-location-sig")).thenReturn(true);
        Assert.assertThat(errorSupplier.get(), isPresent());
        Assert.assertThat(generateMemReport(signalStack1, serializer), isPresent());
        Assert.assertThat(generateMemReport(signalStack2, serializer), isPresent());

        when(mockConfiguration.isExcludedLibrary("e1-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("e2-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("f1-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("f2-location-sig")).thenReturn(false);
        Assert.assertThat(errorSupplier.get(), isPresent());
        Assert.assertThat(generateMemReport(signalStack1, serializer), isAbsent());
        Assert.assertThat(generateMemReport(signalStack2, serializer), isPresent());

        when(mockConfiguration.isExcludedLibrary("e1-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("e2-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("f1-location-sig")).thenReturn(false);
        when(mockConfiguration.isExcludedLibrary("f2-location-sig")).thenReturn(true);
        Assert.assertThat(errorSupplier.get(), isPresent());
        Assert.assertThat(generateMemReport(signalStack1, serializer), isPresent());
        Assert.assertThat(generateMemReport(signalStack2, serializer), isAbsent());

        when(mockConfiguration.isExcludedLibrary("e1-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("e2-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("f1-location-sig")).thenReturn(true);
        when(mockConfiguration.isExcludedLibrary("f2-location-sig")).thenReturn(true);
        Assert.assertThat(errorSupplier.get(), isAbsent());
        Assert.assertThat(generateMemReport(signalStack1, serializer), isAbsent());
        Assert.assertThat(generateMemReport(signalStack2, serializer), isAbsent());
    }

    @Test
    public void addsTheLowestEventStackFirst() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Supplier<Optional<RawStackError>> errorSupplier =
                () -> serializer.generateErrorData(
                        e1, e2,
                        Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace)),
                        Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace)),
                        false);

        {
            when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("a-location-sig");
            when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("b-location-sig");
            Optional<RawStackError> maybeError = errorSupplier.get();
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals(2, maybeError.get().stack_traces.size());
            Assert.assertEquals("1", maybeError.get().stack_traces.get(0).thread_id);
            Assert.assertEquals("2", maybeError.get().stack_traces.get(1).thread_id);
        }

        {
            when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("b-location-sig");
            when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("a-location-sig");
            Optional<RawStackError> maybeError = errorSupplier.get();
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals(2, maybeError.get().stack_traces.size());
            Assert.assertEquals("2", maybeError.get().stack_traces.get(0).thread_id);
            Assert.assertEquals("1", maybeError.get().stack_traces.get(1).thread_id);
        }
    }

    @Test
    public void addsRaceCategory() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Function<Boolean, Optional<RawStackError>> isSignalRaceToError =
                isSignalRace -> serializer.generateErrorData(
                        e1, e2,
                        Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace)),
                        Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace)),
                        isSignalRace);

        {
            Optional<RawStackError> maybeError = isSignalRaceToError.apply(false);
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals(ErrorCategory.Tag.UNDEFINED, maybeError.get().category.tag());
            Assert.assertEquals(Language.Tag.C, maybeError.get().category.getUndefined().tag());
        }
        {
            Optional<RawStackError> maybeError = isSignalRaceToError.apply(true);
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals(ErrorCategory.Tag.LINTERROR, maybeError.get().category.tag());
        }
    }

    @Test
    public void readWriteErrorId() throws Exception {
        TriFunctionWithException <
                Boolean,
                FunctionWithException<TraceUtils, List<ReadonlyEventInterface>>,
                FunctionWithException<TraceUtils, List<ReadonlyEventInterface>>,
                Optional<RawStackError>
                > errorBuilder =
                (isSignalRace, e1Supplier, e2Supplier) -> {
                    TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
                    TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
                    tu.setTraceState(traceState);

                    List<ReadonlyEventInterface> e1List;
                    List<ReadonlyEventInterface> e2List;

                    List<RawTrace> rawTraces = Arrays.asList(
                            tu.createRawTrace(
                                    e1List = e1Supplier.apply(tu)
                            ),
                            tu.createRawTrace(
                                    tu.switchThread(THREAD_2, NO_SIGNAL),
                                    e2List = e2Supplier.apply(tu)));

                    ReadonlyEventInterface e1 = extractSingleEvent(e1List);
                    ReadonlyEventInterface e2 = extractSingleEvent(e2List);

                    Trace trace = traceState.initNextTraceWindow(rawTraces);

                    when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
                    when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
                    when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                            .thenReturn("e1-e2-race-data-sig");

                    RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);
                    return serializer.generateErrorData(
                                    e1, e2,
                                    Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace)),
                                    Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace)),
                                    isSignalRace);
                };

        {
            Optional<RawStackError> maybeError = errorBuilder.apply(
                    false, tu -> tu.nonAtomicLoad(ADDRESS_1, VALUE_1), tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1));
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals("CEER4", maybeError.get().error_id);
        }

        {
            Optional<RawStackError> maybeError = errorBuilder.apply(
                    false, tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1), tu -> tu.nonAtomicLoad(ADDRESS_1, VALUE_1));
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals("CEER4", maybeError.get().error_id);
        }

        {
            Optional<RawStackError> maybeError = errorBuilder.apply(
                    false, tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1), tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1));
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals("CEER5", maybeError.get().error_id);
        }

        {
            Optional<RawStackError> maybeError = errorBuilder.apply(
                    true, tu -> tu.nonAtomicLoad(ADDRESS_1, VALUE_1), tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1));
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals("RVP2", maybeError.get().error_id);
        }

        {
            Optional<RawStackError> maybeError = errorBuilder.apply(
                    true, tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1), tu -> tu.nonAtomicLoad(ADDRESS_1, VALUE_1));
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals("RVP2", maybeError.get().error_id);
        }

        {
            Optional<RawStackError> maybeError = errorBuilder.apply(
                    true, tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1), tu -> tu.nonAtomicStore(ADDRESS_1, VALUE_1));
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals("RVP1", maybeError.get().error_id);
        }
    }

    @Test
    public void parentThreadReporting() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getLocationSig(LOCATION_1)).thenReturn("creation-location-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace));
        List<SignalStackEvent> signalStack2 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace));

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        {
            when(mockMetadata.getParentOTID(THREAD_2)).thenReturn(OptionalLong.empty());
            when(mockMetadata.getOriginalThreadCreationLocId(THREAD_2)).thenReturn(OptionalLong.empty());
            Optional<RawStackError> maybeError =
                    serializer.generateErrorData(e1, e2, signalStack1, signalStack2,false);
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals(2, maybeError.get().stack_traces.size());
            {
                RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
                Assert.assertNull(stackTrace.thread_created_by);
                Assert.assertEquals("1", stackTrace.thread_id);
                Assert.assertNull(stackTrace.thread_created_at);
            }
            {
                RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
                Assert.assertNull(stackTrace.thread_created_by);
                Assert.assertEquals("2", stackTrace.thread_id);
                Assert.assertNull(stackTrace.thread_created_at);
            }
            Assert.assertThat(
                    generateMemReport(signalStack1, serializer),
                    isPresentWithValue(
                            "    Read in thread 1\n" +
                                    "      > loc-prefix e1-location-sig\n" +
                                    "    Thread 1 is the main thread\n"));
            Assert.assertThat(
                    generateMemReport(signalStack2, serializer),
                    isPresentWithValue(
                            "    Write in thread 2\n" +
                                    "      > loc-prefix e2-location-sig\n" +
                                    "    Thread 2 was created by n/a\n"));
        }

        {
            when(mockMetadata.getParentOTID(THREAD_2)).thenReturn(OptionalLong.of(THREAD_1));
            when(mockMetadata.getOriginalThreadCreationLocId(THREAD_2)).thenReturn(OptionalLong.empty());
            Optional<RawStackError> maybeError =
                    serializer.generateErrorData(e1, e2, signalStack1, signalStack2,false);
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals(2, maybeError.get().stack_traces.size());
            {
                RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
                Assert.assertNull(stackTrace.thread_created_by);
                Assert.assertEquals("1", stackTrace.thread_id);
                Assert.assertNull(stackTrace.thread_created_at);
            }
            {
                RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
                Assert.assertEquals("1", stackTrace.thread_created_by);
                Assert.assertEquals("2", stackTrace.thread_id);
                Assert.assertNull(stackTrace.thread_created_at);
            }
            Assert.assertThat(
                    generateMemReport(signalStack1, serializer),
                    isPresentWithValue(
                            "    Read in thread 1\n" +
                                    "      > loc-prefix e1-location-sig\n" +
                                    "    Thread 1 is the main thread\n"));
            Assert.assertThat(
                    generateMemReport(signalStack2, serializer),
                    isPresentWithValue(
                            "    Write in thread 2\n" +
                                    "      > loc-prefix e2-location-sig\n" +
                                    "    Thread 2 was created by thread 1\n" +
                                    "        loc-prefix unknown location\n"));
        }

        {
            when(mockMetadata.getParentOTID(THREAD_2)).thenReturn(OptionalLong.of(THREAD_1));
            when(mockMetadata.getOriginalThreadCreationLocId(THREAD_2)).thenReturn(OptionalLong.of(LOCATION_1));
            Optional<RawStackError> maybeError =
                    serializer.generateErrorData(e1, e2, signalStack1, signalStack2,false);
            Assert.assertTrue(maybeError.isPresent());
            Assert.assertEquals(2, maybeError.get().stack_traces.size());
            {
                RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
                Assert.assertNull(stackTrace.thread_created_by);
                Assert.assertEquals("1", stackTrace.thread_id);
                Assert.assertNull(stackTrace.thread_created_at);
            }
            {
                RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
                Assert.assertEquals("1", stackTrace.thread_created_by);
                Assert.assertEquals("2", stackTrace.thread_id);
                Assert.assertEquals("creation-location-sig", stackTrace.thread_created_at.address);
                Assert.assertEquals(0, stackTrace.thread_created_at.locks.size());
            }
            Assert.assertThat(
                    generateMemReport(signalStack1, serializer),
                    isPresentWithValue(
                            "    Read in thread 1\n" +
                                    "      > loc-prefix e1-location-sig\n" +
                                    "    Thread 1 is the main thread\n"));
            Assert.assertThat(
                    generateMemReport(signalStack2, serializer),
                    isPresentWithValue(
                            "    Write in thread 2\n" +
                                    "      > loc-prefix e2-location-sig\n" +
                                    "    Thread 2 was created by thread 1\n" +
                                    "        loc-prefix creation-location-sig\n"));
        }
    }

    @Test
    public void generatesPrimaryStackTraceNoLocks() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;
        List<ReadonlyEventInterface> f1List;
        List<ReadonlyEventInterface> f2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        f1List =tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, OptionalLong.of(CALL_SITE_ADDRESS_1)),
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        f2List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, OptionalLong.of(CALL_SITE_ADDRESS_2)),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);
        ReadonlyEventInterface f1 = extractSingleEvent(f1List);
        ReadonlyEventInterface f2 = extractSingleEvent(f2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getLocationSig(f1.getLocationId())).thenReturn("f1-location-sig");
        when(mockMetadata.getLocationSig(f2.getLocationId())).thenReturn("f2-location-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace));
        List<SignalStackEvent> signalStack2 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace));

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Optional<RawStackError> maybeError =
                serializer.generateErrorData(e1, e2, signalStack1, signalStack2, false);

        Assert.assertTrue(maybeError.isPresent());

        Assert.assertEquals(2, maybeError.get().stack_traces.size());
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
            Assert.assertEquals("1", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Read in thread 1", stackTraceComponent.description_format);
                Assert.assertEquals(0, stackTraceComponent.description_fields.size());
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e1-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f1-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
        }
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
            Assert.assertEquals("2", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Write in thread 2", stackTraceComponent.description_format);
                Assert.assertEquals(0, stackTraceComponent.description_fields.size());
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
        }

        Assert.assertThat(
                generateMemReport(signalStack1, serializer),
                isPresentWithValue(
                        "    Read in thread 1\n" +
                                "      > loc-prefix e1-location-sig\n" +
                                "        loc-prefix f1-location-sig\n" +
                                "    Thread 1 is the main thread\n"));
        Assert.assertThat(
                generateMemReport(signalStack2, serializer),
                isPresentWithValue(
                        "    Write in thread 2\n" +
                                "      > loc-prefix e2-location-sig\n" +
                                "        loc-prefix f2-location-sig\n" +
                                "    Thread 2 was created by n/a\n"));
    }

    @Test
    public void generatesPrimaryStackTraceWithLocks() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;
        List<ReadonlyEventInterface> f1List;
        List<ReadonlyEventInterface> f2List;
        List<ReadonlyEventInterface> l1List;
        List<ReadonlyEventInterface> l2List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        f1List =tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, OptionalLong.of(CALL_SITE_ADDRESS_1)),
                        l1List = tu.lock(LOCK_1),
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        l2List = tu.lock(LOCK_2),
                        f2List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, OptionalLong.of(CALL_SITE_ADDRESS_2)),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);
        ReadonlyEventInterface f1 = extractSingleEvent(f1List);
        ReadonlyEventInterface f2 = extractSingleEvent(f2List);
        ReadonlyEventInterface l1 = extractSingleEvent(l1List);
        ReadonlyEventInterface l2 = extractSingleEvent(l2List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getLocationSig(f1.getLocationId())).thenReturn("f1-location-sig");
        when(mockMetadata.getLocationSig(f2.getLocationId())).thenReturn("f2-location-sig");
        when(mockMetadata.getLocationSig(l1.getLocationId())).thenReturn("l1-location-sig");
        when(mockMetadata.getLocationSig(l2.getLocationId())).thenReturn("l2-location-sig");
        when(mockMetadata.getLockSig(l1, trace.getStacktraceAt(l1))).thenReturn("l1-lock-sig");
        when(mockMetadata.getLockSig(l2, trace.getStacktraceAt(l2))).thenReturn("l2-lock-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace));
        List<SignalStackEvent> signalStack2 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace));

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Optional<RawStackError> maybeError =
                serializer.generateErrorData(e1, e2, signalStack1, signalStack2,  false);

        Assert.assertTrue(maybeError.isPresent());

        Assert.assertEquals(2, maybeError.get().stack_traces.size());
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
            Assert.assertEquals("1", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Read in thread 1 holding lock %s", stackTraceComponent.description_format);
                Assert.assertEquals(1, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertNull(field.getSignal());
                    Assert.assertEquals("l1-lock-sig", field.getLock().address);
                    Assert.assertNull(field.getLock().frame1);
                    Assert.assertNull(field.getLock().frame2);
                }
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e1-location-sig", frame.address);
                    Assert.assertEquals(1, frame.locks.size());
                    {
                        RawLock lock = frame.locks.get(0);
                        Assert.assertEquals("l1-location-sig", lock.locked_at);
                        Assert.assertEquals("l1-lock-sig", lock.id.address);
                        Assert.assertNull(lock.id.frame1);
                        Assert.assertNull(lock.id.frame2);
                    }
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f1-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
        }
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
            Assert.assertEquals("2", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Write in thread 2 holding lock %s", stackTraceComponent.description_format);
                Assert.assertEquals(1, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertNull(field.getSignal());
                    Assert.assertEquals("l2-lock-sig", field.getLock().address);
                    Assert.assertNull(field.getLock().frame1);
                    Assert.assertNull(field.getLock().frame2);
                }
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f2-location-sig", frame.address);
                    Assert.assertEquals(1, frame.locks.size());
                    {
                        RawLock lock = frame.locks.get(0);
                        Assert.assertEquals("l2-location-sig", lock.locked_at);
                        Assert.assertEquals("l2-lock-sig", lock.id.address);
                        Assert.assertNull(lock.id.frame1);
                        Assert.assertNull(lock.id.frame2);
                    }
                }
            }
        }

        Assert.assertThat(
                generateMemReport(signalStack1, serializer),
                isPresentWithValue(
                        "    Read in thread 1 holding lock l1-lock-sig\n" +
                                "      > loc-prefix e1-location-sig\n" +
                                "        - locked l1-lock-sig loc-prefix l1-location-sig\n" +
                                "        loc-prefix f1-location-sig\n" +
                                "    Thread 1 is the main thread\n"));
        Assert.assertThat(
                generateMemReport(signalStack2, serializer),
                isPresentWithValue(
                        "    Write in thread 2 holding lock l2-lock-sig\n" +
                                "      > loc-prefix e2-location-sig\n" +
                                "        loc-prefix f2-location-sig\n" +
                                "        - locked l2-lock-sig loc-prefix l2-location-sig\n" +
                                "    Thread 2 was created by n/a\n"));
    }

    @Test
    public void generatesPrimarySignalStackTrace() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;
        List<ReadonlyEventInterface> f1List;
        List<ReadonlyEventInterface> f2List;
        List<ReadonlyEventInterface> l1List;

        List<RawTrace> rawTraces = Arrays.asList(
                tu.createRawTrace(
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        f1List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, OptionalLong.of(CALL_SITE_ADDRESS_1)),
                        l1List = tu.lock(LOCK_1),
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        f2List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, OptionalLong.of(CALL_SITE_ADDRESS_2)),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);
        ReadonlyEventInterface f1 = extractSingleEvent(f1List);
        ReadonlyEventInterface f2 = extractSingleEvent(f2List);
        ReadonlyEventInterface l1 = extractSingleEvent(l1List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getLocationSig(f1.getLocationId())).thenReturn("f1-location-sig");
        when(mockMetadata.getLocationSig(f2.getLocationId())).thenReturn("f2-location-sig");
        when(mockMetadata.getLocationSig(l1.getLocationId())).thenReturn("l1-location-sig");
        when(mockMetadata.getLockSig(l1, trace.getStacktraceAt(l1))).thenReturn("l1-lock-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e1, trace));
        List<SignalStackEvent> signalStack2 = Collections.singletonList(SignalStackEvent.fromEventAndTrace(e2, trace));

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Optional<RawStackError> maybeError =
                serializer.generateErrorData(e1, e2, signalStack1, signalStack2, false);

        Assert.assertTrue(maybeError.isPresent());

        Assert.assertEquals(2, maybeError.get().stack_traces.size());
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
            Assert.assertEquals("1", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Read in signal %s holding lock %s", stackTraceComponent.description_format);
                Assert.assertEquals(2, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertEquals(1L, field.getSignal().longValue());
                    Assert.assertNull(field.getLock());
                }
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(1);
                    Assert.assertNull(field.getSignal());
                    Assert.assertEquals("l1-lock-sig", field.getLock().address);
                    Assert.assertNull(field.getLock().frame1);
                    Assert.assertNull(field.getLock().frame2);
                }
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e1-location-sig", frame.address);
                    Assert.assertEquals(1, frame.locks.size());
                    {
                        RawLock lock = frame.locks.get(0);
                        Assert.assertEquals("l1-location-sig", lock.locked_at);
                        Assert.assertEquals("l1-lock-sig", lock.id.address);
                        Assert.assertNull(lock.id.frame1);
                        Assert.assertNull(lock.id.frame2);
                    }
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f1-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
        }
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
            Assert.assertEquals("2", stackTrace.thread_id);
            Assert.assertEquals(1, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Write in signal %s", stackTraceComponent.description_format);
                Assert.assertEquals(1, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertEquals(2L, field.getSignal().longValue());
                    Assert.assertNull(field.getLock());
                }
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
        }

        Assert.assertThat(
                generateMemReport(signalStack1, serializer),
                isPresentWithValue(
                        "    Read in signal S1 holding lock l1-lock-sig\n" +
                                "      > loc-prefix e1-location-sig\n" +
                                "        - locked l1-lock-sig loc-prefix l1-location-sig\n" +
                                "        loc-prefix f1-location-sig\n"));
        Assert.assertThat(
                generateMemReport(signalStack2, serializer),
                isPresentWithValue(
                        "    Write in signal S2\n" +
                                "      > loc-prefix e2-location-sig\n" +
                                "        loc-prefix f2-location-sig\n"));
    }


    @Test
    public void generatesSecondaryStackTraceAfterEvent() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, BASE_PC);
        TraceState traceState = new TraceState(mockConfiguration, mockMetadata);
        tu.setTraceState(traceState);

        List<ReadonlyEventInterface> e1List;
        List<ReadonlyEventInterface> e2List;
        List<ReadonlyEventInterface> f1List;
        List<ReadonlyEventInterface> f2List;
        List<ReadonlyEventInterface> i1List;
        List<ReadonlyEventInterface> i2List;
        List<ReadonlyEventInterface> i3List;
        List<ReadonlyEventInterface> l1List;

        RawTrace rawTrace1;

        List<RawTrace> rawTraces = Arrays.asList(
                rawTrace1 = tu.createRawTrace(
                        tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, NO_SIGNAL),
                        i3List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_3, SIGNAL_HANDLER_3, GENERATION_1),
                        f1List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_1, OptionalLong.of(CALL_SITE_ADDRESS_1)),
                        l1List = tu.lock(LOCK_1),
                        i1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, ONE_SIGNAL),
                        tu.enterSignal(SIGNAL_NUMBER_4, SIGNAL_HANDLER_4, GENERATION_1),
                        f2List = tu.enterFunction(CANONICAL_FRAME_ADDRESS_2, OptionalLong.of(CALL_SITE_ADDRESS_2)),
                        i2List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_1, TWO_SIGNALS),
                        tu.enterSignal(SIGNAL_NUMBER_1, SIGNAL_HANDLER_1, GENERATION_1),
                        e1List = tu.nonAtomicLoad(ADDRESS_1, VALUE_1)
                ),
                tu.createRawTrace(
                        tu.switchThread(THREAD_2, TWO_SIGNALS),
                        tu.enterSignal(SIGNAL_NUMBER_2, SIGNAL_HANDLER_2, GENERATION_1),
                        e2List = tu.nonAtomicStore(ADDRESS_1, VALUE_1)));

        ReadonlyEventInterface e1 = extractSingleEvent(e1List);
        ReadonlyEventInterface e2 = extractSingleEvent(e2List);
        ReadonlyEventInterface f1 = extractSingleEvent(f1List);
        ReadonlyEventInterface f2 = extractSingleEvent(f2List);
        ReadonlyEventInterface i1 = extractSingleEvent(i1List);
        ReadonlyEventInterface i2 = extractSingleEvent(i2List);
        ReadonlyEventInterface i3 = extractSingleEvent(i3List);
        ReadonlyEventInterface l1 = extractSingleEvent(l1List);

        Trace trace = traceState.initNextTraceWindow(rawTraces);

        when(mockMetadata.getLocationSig(e1.getLocationId())).thenReturn("e1-location-sig");
        when(mockMetadata.getLocationSig(e2.getLocationId())).thenReturn("e2-location-sig");
        when(mockMetadata.getLocationSig(f1.getLocationId())).thenReturn("f1-location-sig");
        when(mockMetadata.getLocationSig(f2.getLocationId())).thenReturn("f2-location-sig");
        when(mockMetadata.getLocationSig(i1.getLocationId())).thenReturn("i1-location-sig");
        when(mockMetadata.getLocationSig(i2.getLocationId())).thenReturn("i2-location-sig");
        when(mockMetadata.getLocationSig(i3.getLocationId())).thenReturn("i3-location-sig");
        when(mockMetadata.getLocationSig(l1.getLocationId())).thenReturn("l1-location-sig");
        when(mockMetadata.getLockSig(l1, trace.getStacktraceAt(l1))).thenReturn("l1-lock-sig");
        when(mockMetadata.getRaceDataSig(e1, trace.getStacktraceAt(e1), trace.getStacktraceAt(e2), mockConfiguration))
                .thenReturn("e1-e2-race-data-sig");

        List<SignalStackEvent> signalStack1 = Arrays.asList(
                SignalStackEvent.fromEventAndTrace(e1, trace),
                SignalStackEvent.fromEventAndTrace(i1, trace),
                SignalStackEvent.fromBeforeFirstEvent(
                        rawTrace1.getThreadInfo().getId(),
                        Collections.emptyList(),
                        ThreadType.THREAD,
                        THREAD_1,
                        OptionalLong.empty()));
        List<SignalStackEvent> signalStack2 = Arrays.asList(
                SignalStackEvent.fromEventAndTrace(e2, trace),
                SignalStackEvent.fromEventAndTrace(i2, trace),
                SignalStackEvent.fromEventAndTrace(i3, trace));

        RaceSerializer serializer = new RaceSerializer(mockConfiguration, mockSignatureProcessor, mockMetadata);

        Optional<RawStackError> maybeError =
                serializer.generateErrorData(e1, e2, signalStack1, signalStack2, false);

        Assert.assertTrue(maybeError.isPresent());

        Assert.assertEquals(2, maybeError.get().stack_traces.size());
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(0);
            Assert.assertEquals("1", stackTrace.thread_id);
            Assert.assertEquals(3, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Read in signal %s", stackTraceComponent.description_format);
                Assert.assertEquals(1, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertEquals(1L, field.getSignal().longValue());
                    Assert.assertNull(field.getLock());
                }
                Assert.assertEquals(1, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e1-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(1);
                Assert.assertEquals("Interrupting signal %s holding lock %s", stackTraceComponent.description_format);
                Assert.assertEquals(2, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertEquals(3L, field.getSignal().longValue());
                    Assert.assertNull(field.getLock());
                }
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(1);
                    Assert.assertNull(field.getSignal());
                    Assert.assertEquals("l1-lock-sig", field.getLock().address);
                    Assert.assertNull(field.getLock().frame1);
                    Assert.assertNull(field.getLock().frame2);
                }
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("i1-location-sig", frame.address);
                    Assert.assertEquals(1, frame.locks.size());
                    {
                        RawLock lock = frame.locks.get(0);
                        Assert.assertEquals("l1-location-sig", lock.locked_at);
                        Assert.assertEquals("l1-lock-sig", lock.id.address);
                        Assert.assertNull(lock.id.frame1);
                        Assert.assertNull(lock.id.frame2);
                    }
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f1-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(2);
                Assert.assertEquals("Interrupting thread 1 before any event", stackTraceComponent.description_format);
                Assert.assertEquals(0, stackTraceComponent.description_fields.size());
                Assert.assertEquals(0, stackTraceComponent.frames.size());
            }
        }
        {
            RawStackTrace stackTrace = maybeError.get().stack_traces.get(1);
            Assert.assertEquals("2", stackTrace.thread_id);
            Assert.assertEquals(3, stackTrace.components.size());
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(0);
                Assert.assertEquals("Write in signal %s", stackTraceComponent.description_format);
                Assert.assertEquals(1, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertEquals(2L, field.getSignal().longValue());
                    Assert.assertNull(field.getLock());
                }
                Assert.assertEquals(1, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("e2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(1);
                Assert.assertEquals("Interrupting signal %s", stackTraceComponent.description_format);
                Assert.assertEquals(1, stackTraceComponent.description_fields.size());
                {
                    RawComponentField field = stackTraceComponent.description_fields.get(0);
                    Assert.assertEquals(4L, field.getSignal().longValue());
                    Assert.assertNull(field.getLock());
                }
                Assert.assertEquals(2, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("i2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
                {
                    RawFrame frame = stackTraceComponent.frames.get(1);
                    Assert.assertEquals("f2-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
            {
                RawStackTraceComponent stackTraceComponent = stackTrace.components.get(2);
                Assert.assertEquals("Interrupting thread 2", stackTraceComponent.description_format);
                Assert.assertEquals(0, stackTraceComponent.description_fields.size());
                Assert.assertEquals(1, stackTraceComponent.frames.size());
                {
                    RawFrame frame = stackTraceComponent.frames.get(0);
                    Assert.assertEquals("i3-location-sig", frame.address);
                    Assert.assertEquals(0, frame.locks.size());
                }
            }
        }


        Assert.assertThat(
                generateMemReport(signalStack1, serializer),
                isPresentWithValue(
                        "    Read in signal S1\n" +
                                "      > loc-prefix e1-location-sig\n" +
                                "    Interrupting signal S3 holding lock l1-lock-sig\n" +
                                "      > loc-prefix i1-location-sig\n" +
                                "        - locked l1-lock-sig loc-prefix l1-location-sig\n" +
                                "        loc-prefix f1-location-sig\n" +
                                "    Interrupting thread 1 before any event.\n"));
        Assert.assertThat(
                generateMemReport(signalStack2, serializer),
                isPresentWithValue(
                        "    Write in signal S2\n" +
                                "      > loc-prefix e2-location-sig\n" +
                                "    Interrupting signal S4\n" +
                                "      > loc-prefix i2-location-sig\n" +
                                "        loc-prefix f2-location-sig\n" +
                                "    Interrupting thread 2\n" +
                                "      > loc-prefix i3-location-sig\n" +
                                "    Thread 2 was created by n/a\n"));
    }

    @FunctionalInterface
    interface TriFunctionWithException<A,B,C,R> {
        R apply(A a, B b, C c) throws Exception;
    }

    @FunctionalInterface
    interface FunctionWithException<A,R> {
        R apply(A a) throws Exception;
    }

    private Optional<String> generateMemReport(List<SignalStackEvent> signalStack1, RaceSerializer serializer) {
        StringBuilder sb = new StringBuilder();
        if (serializer.generateMemAccReport(signalStack1, sb)) {
            return Optional.of(sb.toString());
        }
        return Optional.empty();
    }

}
