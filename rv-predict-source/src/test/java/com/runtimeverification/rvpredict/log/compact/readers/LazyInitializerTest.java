package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;
import com.runtimeverification.rvpredict.log.compact.TraceHeader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class LazyInitializerTest {
    @Mock private LazyInitializer.Factory<Integer> mockFactory;
    @Mock private TraceHeader mockTraceHeader;

    @Test
    public void createsElement() throws InvalidTraceDataException {
        when(mockFactory.create(mockTraceHeader)).thenReturn(3);

        LazyInitializer<Integer> initializer = new LazyInitializer<>(mockFactory);
        verify(mockFactory, never()).create(any(TraceHeader.class));

        Assert.assertEquals(new Integer(3), initializer.getInit(mockTraceHeader));
        verify(mockFactory, times(1)).create(mockTraceHeader);

        Assert.assertEquals(new Integer(3), initializer.getInit(mockTraceHeader));
        verify(mockFactory, times(1)).create(mockTraceHeader);
        verify(mockFactory, times(1)).create(any(TraceHeader.class));
    }
}
