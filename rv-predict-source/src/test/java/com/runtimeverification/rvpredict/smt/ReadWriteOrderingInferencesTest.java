package com.runtimeverification.rvpredict.smt;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.Context;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.testutils.TraceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReadWriteOrderingInferencesTest {
    private static final int NO_SIGNAL = 0;
    private static final long THREAD_1 = 101L;
    private static final long BASE_ID = 201L;
    private static final long PC_BASE = 301L;
    private static final int TTID_1 = 401;
    private static final int TTID_2 = 402;
    private static final int TTID_3 = 403;
    private static final int TTID_4 = 404;
    private static final int TTID_5 = 405;
    private static final long ADDRESS_1 = 501L;
    private static final long VALUE_1 = 601L;
    private static final long VALUE_2 = 602L;

    @Mock private TransitiveClosure.Builder mockTransitiveClosure;
    @Mock private Context mockContext;

    private int nextIdDelta = 0;

    @Before
    public void setUp() {
        nextIdDelta = 0;
        when(mockContext.newId()).then(invocation -> BASE_ID + nextIdDelta++);
    }

    @Test
    public void doesNotAddAnyRelationForEmptyInput() {
        ReadWriteOrderingInferences inferences = new ReadWriteOrderingInferences(Collections.emptyMap());
        inferences.addToMhb(mockTransitiveClosure, Optional.empty());

        verify(mockTransitiveClosure, never())
                .addNotRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, never())
                .addRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
    }

    @Test
    public void doesNotAddAnyRelationForReadAndWriteOnSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> thread = TraceUtils.flatten(
                tu.nonAtomicStore(ADDRESS_1, VALUE_1),
                tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        ReadWriteOrderingInferences inferences = new ReadWriteOrderingInferences(ImmutableMap.of(TTID_1, thread));
        inferences.addToMhb(mockTransitiveClosure, Optional.empty());

        verify(mockTransitiveClosure, never())
                .addNotRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, never())
                .addRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
    }

    @Test
    public void addsRelationForReadAndWriteOnDifferentThreads() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> thread1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1);

        ReadWriteOrderingInferences inferences = new ReadWriteOrderingInferences(ImmutableMap.of(
                TTID_1, thread1,
                TTID_2, thread2));
        inferences.addToMhb(mockTransitiveClosure, Optional.empty());

        verify(mockTransitiveClosure, times(1))
                .addNotRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, times(1))
                .addNotRelation(TraceUtils.extractSingleEvent(thread2), TraceUtils.extractSingleEvent(thread1));
        verify(mockTransitiveClosure, never())
                .addRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
    }

    @Test
    public void addsHBRelationForSecondReadOnSameThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> e1;
        List<ReadonlyEventInterface> e2;

        List<ReadonlyEventInterface> thread1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread2 = TraceUtils.flatten(
                e1 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1),
                e2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1));

        ReadWriteOrderingInferences inferences = new ReadWriteOrderingInferences(ImmutableMap.of(
                TTID_1, thread1,
                TTID_2, thread2));
        inferences.addToMhb(mockTransitiveClosure, Optional.empty());

        verify(mockTransitiveClosure, times(1))
                .addNotRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, times(1))
                .addNotRelation(TraceUtils.extractSingleEvent(e1), TraceUtils.extractSingleEvent(thread1));
        verify(mockTransitiveClosure, times(1))
                .addRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, times(1))
                .addRelation(TraceUtils.extractSingleEvent(thread1), TraceUtils.extractSingleEvent(e2));
    }

    @Test
    public void addsNHBRelationForReadOnDifferentThread() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> thread1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread3 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1);

        ReadWriteOrderingInferences inferences = new ReadWriteOrderingInferences(ImmutableMap.of(
                TTID_1, thread1,
                TTID_2, thread2,
                TTID_3, thread3));
        inferences.addToMhb(mockTransitiveClosure, Optional.empty());

        verify(mockTransitiveClosure, times(2))
                .addNotRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, times(1))
                .addNotRelation(TraceUtils.extractSingleEvent(thread2), TraceUtils.extractSingleEvent(thread1));
        verify(mockTransitiveClosure, times(1))
                .addNotRelation(TraceUtils.extractSingleEvent(thread3), TraceUtils.extractSingleEvent(thread1));
        verify(mockTransitiveClosure, never())
                .addRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
    }

    @Test
    public void doesNotAddAnyRelationIfValueWrittenOnTwoThreads() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> thread1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1);

        ReadWriteOrderingInferences inferences = new ReadWriteOrderingInferences(ImmutableMap.of(
                TTID_1, thread1,
                TTID_2, thread2,
                TTID_3, thread3));
        inferences.addToMhb(mockTransitiveClosure, Optional.empty());

        verify(mockTransitiveClosure, never())
                .addNotRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, never())
                .addRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
    }

    @Test
    public void addsRelationOnlyForUniquelyWrittenValues() throws InvalidTraceDataException {
        TraceUtils tu = new TraceUtils(mockContext, THREAD_1, NO_SIGNAL, PC_BASE);

        List<ReadonlyEventInterface> thread1 = tu.nonAtomicStore(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread2 = tu.nonAtomicLoad(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread3 = tu.nonAtomicStore(ADDRESS_1, VALUE_1);
        List<ReadonlyEventInterface> thread4 = tu.nonAtomicLoad(ADDRESS_1, VALUE_2);
        List<ReadonlyEventInterface> thread5 = tu.nonAtomicStore(ADDRESS_1, VALUE_2);

        ReadWriteOrderingInferences inferences = new ReadWriteOrderingInferences(ImmutableMap.of(
                TTID_1, thread1,
                TTID_2, thread2,
                TTID_3, thread3,
                TTID_4, thread4,
                TTID_5, thread5));
        inferences.addToMhb(mockTransitiveClosure, Optional.empty());


        verify(mockTransitiveClosure, times(1))
                .addNotRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
        verify(mockTransitiveClosure, times(1))
                .addNotRelation(TraceUtils.extractSingleEvent(thread4), TraceUtils.extractSingleEvent(thread5));
        verify(mockTransitiveClosure, never())
                .addRelation(any(ReadonlyEventInterface.class), any(ReadonlyEventInterface.class));
    }
}
