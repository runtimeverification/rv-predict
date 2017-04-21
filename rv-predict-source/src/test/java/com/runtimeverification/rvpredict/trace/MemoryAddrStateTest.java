package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import org.junit.Assert;
import org.junit.Test;

public class MemoryAddrStateTest {
    private static final long GID = 1;
    private static final int ID = 1;
    private static final long ADDR = 1;
    private static final long VALUE = 1;
    @Test
    public void notSharedWithoutWriters() {
        MemoryAddrState addrState = new MemoryAddrState();
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.READ));
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(2, EventType.READ));
        Assert.assertFalse(addrState.isWriteShared());
    }

    public void notSharedWithOnlyOneThreadReadingAndWriting() {
        MemoryAddrState addrState = new MemoryAddrState();
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.WRITE));
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.READ));
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.WRITE));
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.READ));
        Assert.assertFalse(addrState.isWriteShared());
    }

    public void sharedWithWriteOnFirstThreadAndReadOnSecond() {
        MemoryAddrState addrState = new MemoryAddrState();
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.WRITE));
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(2, EventType.READ));
        Assert.assertTrue(addrState.isWriteShared());
    }

    public void sharedWithWriteOnFirstThreadAndWriteOnSecond() {
        MemoryAddrState addrState = new MemoryAddrState();
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.WRITE));
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(2, EventType.WRITE));
        Assert.assertTrue(addrState.isWriteShared());
    }

    public void sharedWithReadOnFirstThreadAndWriteOnSecond() {
        MemoryAddrState addrState = new MemoryAddrState();
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(1, EventType.READ));
        Assert.assertFalse(addrState.isWriteShared());
        addrState.touch(createEvent(2, EventType.WRITE));
        Assert.assertTrue(addrState.isWriteShared());
    }

    private Event createEvent(long threadId, EventType eventType) {
        return new Event(1, threadId, 2, 3, 4, eventType);
    }
}
