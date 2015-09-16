package com.runtimeverification.rvpredict.runtime.java.util.concurrent.locks;

import static com.runtimeverification.rvpredict.util.Constants.JUC_LOCK_C;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.Condition;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;

/**
 * Thin wrapper around {@link Condition} object to implement logging.
 *
 * @author YilongL
 */
class _rvpredict_condition_wrapper implements Condition {

    private static final int RVPREDICT_CONDITION_WRAPPER_LOC_ID = RVPredictRuntime.metadata
            .getLocationId("java.util.concurrent.locks._rvpredict_condition_wrapper.dummy(_rvpredict_condition_wrapper:n/a)");

    private final AbstractOwnableSynchronizer sync;

    final Condition condition;

    _rvpredict_condition_wrapper(AbstractOwnableSynchronizer sync, Condition condition) {
        this.sync = sync;
        this.condition = condition;
    }

    private void _rvpredict_wait_acq() {
        RVPredictRuntime.saveLockEvent(EventType.WAIT_ACQ, RVPREDICT_CONDITION_WRAPPER_LOC_ID,
                JUC_LOCK_C, sync);
    }

    private void _rvpredict_wait_rel() {
        RVPredictRuntime.saveLockEvent(EventType.WAIT_REL, RVPREDICT_CONDITION_WRAPPER_LOC_ID,
                JUC_LOCK_C, sync);
    }

    @Override
    public void await() throws InterruptedException {
        _rvpredict_wait_rel();
        try {
            condition.await();
        } finally {
            _rvpredict_wait_acq();
        }
    }

    @Override
    public void awaitUninterruptibly() {
        _rvpredict_wait_rel();
        condition.awaitUninterruptibly();
        _rvpredict_wait_acq();
    }

    @Override
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        _rvpredict_wait_rel();
        try {
            return condition.awaitNanos(nanosTimeout);
        } finally {
            _rvpredict_wait_acq();
        }
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        _rvpredict_wait_rel();
        try {
            return condition.await(time, unit);
        } finally {
            _rvpredict_wait_acq();
        }
    }

    @Override
    public boolean awaitUntil(Date deadline) throws InterruptedException {
        _rvpredict_wait_rel();
        try {
            return condition.awaitUntil(deadline);
        } finally {
            _rvpredict_wait_acq();
        }
    }

    @Override
    public void signal() {
        condition.signal();
    }

    @Override
    public void signalAll() {
        condition.signalAll();
    }

}
