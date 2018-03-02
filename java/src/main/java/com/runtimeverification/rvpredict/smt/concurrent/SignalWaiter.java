package com.runtimeverification.rvpredict.smt.concurrent;

/**
 * Allows waiting until a signal occurs.
 *
 * The signal can be signaled even when nobody is waiting, in which case the next call to waitUntilSignaled will not
 * block. This is similar to a Semaphore, but a semaphore signaled twice would unblock two wait calls.
 */
public class SignalWaiter {
    private volatile boolean signaled = false;

    public synchronized void waitUntilSignaled() {
        while (!signaled) {
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
        signaled = false;
    }

    public synchronized void signal() {
        signaled = true;
        notify();
    }
}
