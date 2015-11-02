// Copyright (c) 2015 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

package examples;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WriteUnderReadLock {

    static int sharedVar;

    static ReadWriteLock lock = new ReentrantReadWriteLock();

    public static void main(String[] args) {
        new ThreadRunner() {
            @Override
            public void thread1() {
                lock.readLock().lock();
                sharedVar++;
                lock.readLock().unlock();
            }

            @Override
            public void thread2() {
                lock.readLock().lock();
                sharedVar++;
                lock.readLock().unlock();
            }
        };
    }
}
