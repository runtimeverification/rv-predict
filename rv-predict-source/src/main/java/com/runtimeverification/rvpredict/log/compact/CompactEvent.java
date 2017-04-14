package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.EventType;

public abstract class CompactEvent {
    private final long eventId;
    private final long locationId;
    private final long threadId;
    private final EventType type;

    CompactEvent(Context context, EventType type) {
        this.eventId = context.newId();
        this.locationId = context.getPC();
        this.threadId = context.getThreadId();
        this.type = type;
    }

    long getEventId() {
        return eventId;
    }

    long getLocationId() {
        return locationId;
    }

    long getThreadId() {
        return threadId;
    }

    EventType getType() {return type;}

    int getDataSizeInBytes() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getDataAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getDataValue() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSignalNumber() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSignalHandlerAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSyncedThreadId() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSyncObject() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
}
