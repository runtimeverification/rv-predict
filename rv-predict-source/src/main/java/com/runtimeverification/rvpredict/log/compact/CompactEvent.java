package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.EventType;

public abstract class CompactEvent {
    private final long id;
    private final long threadId;
    private final EventType compactType;

    CompactEvent(Context context, EventType compactType) {
        this.id = context.newId();
        this.threadId = context.getThreadId();
        this.compactType = compactType;
    }

    long getId() {
        return id;
    }

    long getThreadId() {
        return threadId;
    }

    EventType getCompactType() {return compactType;}

    int getDataSizeInBytes() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getDataAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getDataValue() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getSignalNumber() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getSignalHandlerAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getOtherThreadId() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getLockAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
}
