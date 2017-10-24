package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MemoryAddrStateTest {
    @Mock private ReadonlyEventInterface mockEvent;

    @Test
    public void oneWriteDoesNotImplySharing() {
        MemoryAddrState state = new MemoryAddrState();

        when(mockEvent.isRead()).thenReturn(false);
        state.touch(mockEvent, 0);

        Assert.assertFalse(state.isWriteShared());
    }

    @Test
    public void twoWritesOnSameThreadDoNotImplySharing() {
        MemoryAddrState state = new MemoryAddrState();

        when(mockEvent.isRead()).thenReturn(false);
        state.touch(mockEvent, 0);
        state.touch(mockEvent, 0);

        Assert.assertFalse(state.isWriteShared());
    }

    @Test
    public void twoWritesOnDifferentThreadsImplySharing() {
        MemoryAddrState state = new MemoryAddrState();

        when(mockEvent.isRead()).thenReturn(false);
        state.touch(mockEvent, 0);
        Assert.assertFalse(state.isWriteShared());
        state.touch(mockEvent, 1);

        Assert.assertTrue(state.isWriteShared());
    }

    @Test
    public void multipleReadsDoNotImplySharing() {
        MemoryAddrState state = new MemoryAddrState();

        when(mockEvent.isRead()).thenReturn(true);
        state.touch(mockEvent, 0);
        Assert.assertFalse(state.isWriteShared());
        state.touch(mockEvent, 0);
        Assert.assertFalse(state.isWriteShared());
        state.touch(mockEvent, 1);

        Assert.assertFalse(state.isWriteShared());
    }

    @Test
    public void readingAndWritingFromTheSameThreadDoesNotImplySharing() {
        MemoryAddrState state = new MemoryAddrState();

        when(mockEvent.isRead()).thenReturn(true);
        state.touch(mockEvent, 0);
        Assert.assertFalse(state.isWriteShared());

        when(mockEvent.isRead()).thenReturn(false);
        state.touch(mockEvent, 0);

        Assert.assertFalse(state.isWriteShared());
    }

    @Test
    public void readingAndWritingFromDifferentThreadsImpliesSharing() {
        MemoryAddrState state = new MemoryAddrState();

        when(mockEvent.isRead()).thenReturn(true);
        state.touch(mockEvent, 0);
        Assert.assertFalse(state.isWriteShared());

        when(mockEvent.isRead()).thenReturn(false);
        state.touch(mockEvent, 1);

        Assert.assertTrue(state.isWriteShared());
    }

}
