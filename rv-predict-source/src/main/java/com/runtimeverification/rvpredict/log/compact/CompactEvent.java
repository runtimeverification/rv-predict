package com.runtimeverification.rvpredict.log.compact;

import com.google.common.annotations.VisibleForTesting;
import com.runtimeverification.rvpredict.log.DataAddress;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEvent;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public abstract class CompactEvent extends ReadonlyEvent {
    private long eventId;
    private long locationId;
    private final long originalThreadId;
    private final int signalDepth;
    private final EventType type;

    CompactEvent(Context context, EventType type) {
        this(context.newId(), context.getPC(), context.getThreadId(), context.getSignalDepth(), type);
    }

    @VisibleForTesting
    public CompactEvent(long eventId, long locationId, long originalThreadId, int signalDepth, EventType type) {
        this.eventId = eventId;
        this.locationId = locationId;
        this.originalThreadId = originalThreadId;
        this.signalDepth = signalDepth;
        this.type = type;
    }

    @Override
    public long getEventId() {
        return eventId;
    }

    // TODO(virgil): Convert getLocationId to long.
    @Override
    public long getLocationId() {
        return (int)locationId;
    }

    @Override
    public long getOriginalThreadId() {
        return originalThreadId;
    }

    @Override
    public int getSignalDepth() {
        return signalDepth;
    }

    @Override
    public EventType getType() {return type;}

    int getDataSizeInBytes() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public DataAddress getDataAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getDataValue() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getSyncedThreadId() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getSyncObject() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getSignalNumber() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getPartialSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getFullReadSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    @Override
    public long getFullWriteSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSignalHandlerAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public String getLockRepresentation() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public DataAddress unsafeGetAddress() {
        return DataAddress.NULL_OBJECT;
    }

    @Override
    public long unsafeGetDataValue() {
        return 0;
    }

    @Override
    public ReadonlyEvent copy() {
        return this;
    }

    @Override
    public ReadonlyEventInterface destructiveWithLocationId(long locationId) {
        this.locationId = locationId;
        return this;
    }

    @Override
    public ReadonlyEventInterface destructiveWithEventId(long eventId) {
        this.eventId = eventId;
        return this;
    }

    long getLongAddress() {
        return getDataAddress().getDataAddressOr0();
    }

    @Override
    public String toString() {
        return String.format(
                "%s(ID:%s Loc:%s Otid:%s SD:%s)",
                type, eventId, locationId, originalThreadId, signalDepth);
    }
}
