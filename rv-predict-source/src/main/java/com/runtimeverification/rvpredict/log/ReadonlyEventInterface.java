package com.runtimeverification.rvpredict.log;

public interface ReadonlyEventInterface extends Comparable<ReadonlyEventInterface> {
    long getEventId();
    long getOriginalThreadId();
    int getSignalDepth();
    long getSignalNumber();
    long getLocationId();
    long getDataValue();
    /**
     * Returns an internal identifier of the data that is accessed
     * by a read or write event.
     */
    long getDataInternalIdentifier();
    EventType getType();
    long getSyncObject();
    long getSyncedThreadId();
    long getPartialSignalMask();
    long getFullWriteSignalMask();
    long getFullReadSignalMask();
    long getCanonicalFrameAddress();
    long unsafeGetDataInternalIdentifier();
    long unsafeGetDataValue();

    String getLockRepresentation();
    ReadonlyEventInterface copy();
    /**
     * Returns an identifier of the object containing the data that is accessed
     * by a read or write event (e.g. if the event accesses an array element,
     * this method may, but is not required to, return an identifier of the entire array).
     *
     * The identifier returned by this method may be used to produce an external representation
     * of the event (e.g. it may be used for pretty-printing the event).
     */
    long getDataObjectExternalIdentifier();
    int getFieldIdOrArrayIndex();

    // TODO(virgil): Make this a non-readonly interface and remove these destructive calls since they
    // are not used properly anyway.
    /**
     * Returns an event with the same base data as the current one, except that it has the new location id.
     * The current object cannot be used anymore after this call.
     */
    ReadonlyEventInterface destructiveWithLocationId(long locationId);

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
    boolean isSignalEvent();
    long getLockId();
    boolean isSimilarTo(ReadonlyEventInterface event);
    boolean isSignalMaskRead();
}
