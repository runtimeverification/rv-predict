package com.runtimeverification.rvpredict.log;

public interface ReadonlyEventInterface extends Comparable<ReadonlyEventInterface> {
    long getEventId();
    long getThreadId();
    int getLocationId();
    long getDataValue();
    EventType getType();
    long getDataAddress();
    long getSyncObject();
    long getSyncedThreadId();
    long unsafeGetAddress();
    long unsafeGetDataValue();

    String getLockRepresentation();
    ReadonlyEventInterface copy();

    // TODO(virgil): Think about making this a non-readonly interface and removing these destructive calls.
    /**
     * Returns an event with the same base data as the current one, except that it has the new location id.
     * The current object cannot be used anymore after this call.
     */
    ReadonlyEventInterface destructiveWithLocationId(int locationId);

    /**
     * Returns an event with the same base data as the current one, except that it has the new event id.
     * The current object cannot be used anymore after this call.
     */
    ReadonlyEventInterface destructiveWithEventId(long eventId);

    boolean isRead();
    boolean isWrite();
    boolean isReadOrWrite();
    boolean isStart();
    boolean isJoin();

    /**
     * Returns {@code true} if this event has type {@link EventType#WRITE_LOCK},
     * {@link EventType#READ_LOCK}, or {@link EventType#WAIT_ACQUIRE}; otherwise,
     * {@code false}.
     */
    boolean isLock();
    /**
     * Returns {@code true} if this event has type
     * {@link EventType#WRITE_UNLOCK}, {@link EventType#READ_UNLOCK}, or
     * {@link EventType#WAIT_RELEASE}; otherwise, {@code false}.
     */
    boolean isUnlock();

    boolean isPreLock();
    boolean isReadLock();
    boolean isWriteLock();
    boolean isWaitAcq();
    boolean isReadUnlock();
    boolean isWriteUnlock();
    boolean isWaitRel();
    boolean isSyncEvent();
    boolean isMetaEvent();
    boolean isCallStackEvent();
    boolean isInvokeMethod();
    int getObjectHashCode();
    int getFieldIdOrArrayIndex();
    long getLockId();
    boolean isSimilarTo(ReadonlyEventInterface event);
}
