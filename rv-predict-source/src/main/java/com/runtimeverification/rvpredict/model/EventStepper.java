package com.runtimeverification.rvpredict.model;

import com.runtimeverification.rvpredict.log.Event;

public class EventStepper {
    private final ModelTrace modelTrace;

    public EventStepper(ModelTrace modelTrace) {
        this.modelTrace = modelTrace;
    }

    Configuration expandWithEvent(Configuration configuration, int threadIndex, Event event) {
        if (event.isRead()) {
            return expandWithRead(configuration, threadIndex, event.getAddr(), event.getValue());
        }
        if (event.isWrite()) {
            return expandWithWrite(configuration, threadIndex, event.getAddr(), event.getValue());
        }
        if (event.isWriteLock()) {
            return expandWithWriteLock(configuration, threadIndex, event.getSyncObject());
        }
        if (event.isReadLock()) {
            return expandWithReadLock(configuration, threadIndex, event.getSyncObject());
        }
        if (event.isJoin()) {
            return expandWithJoin(configuration, threadIndex, event.getSyncedThreadId());
        }
        return expandWithGenericEvent(configuration, threadIndex);
    }

    private Configuration expandWithJoin(Configuration configuration, int threadIndex, long joinedThread) {
        Integer joinedThreadIndex = modelTrace.getThreadIndex(joinedThread);
        // The thread may not be found if all its events were in a previous window.
        //
        // I can't tell for sure right now if it can happen that the last instruction of a thread
        // is after the join for that thread. If that happens, then this last instruction may be in a
        // different window, which can't be handled correctly here.
        if (joinedThreadIndex == null ||
                configuration.getEventIndex(joinedThreadIndex) >= modelTrace.getEventCount(joinedThreadIndex)) {
            return expandWithGenericEvent(configuration, threadIndex);
        }
        return null;
    }

    private Configuration expandWithWriteLock(Configuration configuration, int threadIndex, long addr) {
        for (int i = 0; i < configuration.getThreadCount(); i++) {
            if (i == threadIndex) {
                continue;
            }
            if (modelTrace.isReadLocked(i, configuration.getEventIndex(i), addr) ||
                    modelTrace.isWriteLocked(i, configuration.getEventIndex(i), addr)) {
                return null;
            }
        }
        return configuration.clone().advanceThread(threadIndex);
    }

    private Configuration expandWithReadLock(Configuration configuration, int threadIndex, long addr) {
        for (int i = 0; i < configuration.getThreadCount(); i++) {
            if (i == threadIndex) {
                continue;
            }
            if (modelTrace.isWriteLocked(i, configuration.getEventIndex(i), addr)) {
                return null;
            }
        }
        return configuration.clone().advanceThread(threadIndex);
    }

    private Configuration expandWithRead(Configuration configuration, int threadIndex, long addr, long value) {
        Integer variableIndex = modelTrace.getVariableIndexOrNull(addr);  // TODO: replace addresses with indexes.
        if (variableIndex == null || configuration.getValue(variableIndex) != value) {
            return null;
        }
        return configuration.clone().advanceThread(threadIndex);
    }

    private Configuration expandWithWrite(Configuration configuration, int threadIndex, long addr, long value) {
        Integer variableIndex = modelTrace.getVariableIndexOrNull(addr);  // TODO: replace addresses with indexes.
        if (variableIndex == null) {
            return null;
        }
        return configuration.clone().advanceThread(threadIndex).setValue(variableIndex, value);
    }

    private Configuration expandWithGenericEvent(Configuration configuration, int threadIndex) {
        return configuration.clone().advanceThread(threadIndex);
    }
}
