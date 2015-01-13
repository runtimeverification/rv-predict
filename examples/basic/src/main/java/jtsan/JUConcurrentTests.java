/* Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Copyright (c) 2014 Runtime Verification Inc. All Rights Reserved. */

package jtsan;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class contains tests for jtsan. Tests cover java.util.concurrent functionality.
 *
 * @author Konstantin Serebryany
 * @author Sergey Vorobyev
 *
 * Added a main method to facilitate testing and a few more tests.
 *
 * @author YilongL
 */
public class JUConcurrentTests {

    //------------------ Positive tests ---------------------

    @RaceTest(expectRace = true,
            description = "Three threads writing under a reader lock, one under a writing lock")
    public void writingUnderReaderLock() {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        new ThreadRunner(4) {
            @Override
            public void thread1() {
                lock.readLock().lock();
                sharedVar++;
                lock.readLock().unlock();
            }

            @Override
            public void thread2() {
                thread1();
            }

            @Override
            public void thread3() {
                thread1();
            }

            @Override
            public void thread4() {
                lock.writeLock().lock();
                sharedVar++;
                lock.writeLock().unlock();
            }
        };
    }

    @RaceTest(expectRace = true,
            description = "Two writes locked with different locks")
    public void differentLocksWW2() {
        final ReentrantLock lock = new ReentrantLock();
        new ThreadRunner(2) {

            @Override
            public void thread1() {
                lock.lock();
                sharedVar++;
                lock.unlock();
            }

            @Override
            public void thread2() {
                synchronized (this) {
                    sharedVar++;
                }
            }
        };
    }

    @RaceTest(expectRace = true,
            description = "Concurrent access after correct CyclicBarrier")
    public void cyclicBarrierWrong() {
        new ThreadRunner(4) {
            CyclicBarrier barrier;

            @Override
            public void setUp() {
                barrier = new CyclicBarrier(4);
                sharedVar = 0;
            }

            @Override
            public void thread1() {
                synchronized (this) {
                    sharedVar++;
                }
                try {
                    barrier.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (sharedVar == 4) {
                    sharedVar = 5;
                }
            }

            @Override
            public void thread2() {
                thread1();
            }

            @Override
            public void thread3() {
                thread1();
            }

            @Override
            public void thread4() {
                thread1();
            }
        };
    }

    @RaceTest(expectRace = true,
            description = "Test separate monitor and lock instances of Lock")
    public void lockNeMonitor() {
        final ReentrantLock lock = new ReentrantLock();
        new ThreadRunner(2) {
            @Override
            public void thread1() {
                lock.lock();
                sharedVar++;
                lock.unlock();
            }

            @Override
            public void thread2() {
                longSleep();
                synchronized(lock) {
                    sharedVar++;
                }
            }
        };
    }


    //------------------ Negative tests ---------------------

    @RaceTest(expectRace = false,
            description = "Work with BlockingQueue. Two readers, two writers")
    public void arrayBlockingQueue() {
//        final int capacity = 10;
//        final int iter = 1000;
        final int capacity = 8;
        final int iter = 10;
        new ThreadRunner(4) {

            BlockingQueue<Integer> q;

            @Override
            public void setUp() {
                q = new ArrayBlockingQueue<Integer>(capacity);
            }

            @Override
            public void thread1() {
                try {
                    for (int i = 0; i < iter; i++) {
                        q.put(i);
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Exception in arrayBlockingQueue test", ex);
                }
            }

            @Override
            public void thread2() {
                try {
                    for (int i = 0; i < iter; i++) {
                        Integer o = q.take();
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Exception in arrayBlockingQueue test", ex);
                }
            }

            @Override
            public void thread3() {
                thread1();
            }

            @Override
            public void thread4() {
                thread2();
            }

        };
    }

    @RaceTest(expectRace = false,
            description = "Work with BlockingQueue. One reader, one writer")
    public void arrayBlockingQueue2() {
        new ThreadRunner(2) {

            BlockingQueue<Integer> q;

            @Override
            public void setUp() {
                q = new ArrayBlockingQueue<Integer>(1);
            }

            @Override
            public void thread1() {
                sharedVar = 1;
                try {
                    q.put(1);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Exception in arrayBlockingQueue test", ex);
                }
            }

            @Override
            public void thread2() {
                try {
                    q.take();
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Exception in arrayBlockingQueue test", ex);
                }
                sharedVar = 2;
            }
        };
    }


    @RaceTest(expectRace = false,
            description = "Use Lock.lockInteruptibly for acquired a lock")
    public void lockInterruptibly() {
        new ThreadRunner(2) {
            Lock lock;

            @Override
            public void setUp() {
                lock = new ReentrantLock();
            }

            @Override
            public void thread1() {
                try {
                    lock.lockInterruptibly();
                    sharedVar++;
                    lock.unlock();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Exception in test reentrantLockInterruptibly", e);
                }
            }

            @Override
            public void thread2() {
                lock.lock();
                sharedVar++;
                lock.unlock();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "Use ReentrantLock.lockInteruptibly for acquired a lock")
    public void reentrantLockInterruptibly() {
        new ThreadRunner(2) {
            ReentrantLock lock;

            @Override
            public void setUp() {
                lock = new ReentrantLock();
            }

            @Override
            public void thread1() {
                try {
                    lock.lockInterruptibly();
                    sharedVar++;
                    lock.unlock();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Exception in test reentrantLockInterruptibly", e);
                }
            }

            @Override
            public void thread2() {
                lock.lock();
                sharedVar++;
                lock.unlock();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "CountDownLatch")
    public void countDownLatch() {
        new ThreadRunner(4) {
            CountDownLatch latch;

            @Override
            public void setUp() {
                latch = new CountDownLatch(3);
                sharedVar = 0;
            }

            @Override
            public void thread1() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Exception in test CountDownLatch", e);
                }
                if (sharedVar == 3) {
                    sharedVar = 4;
                } else {
                    System.err.println("CountDownLatch assert");
                    System.exit(1);
                }
            }

            @Override
            public void thread2() {
                synchronized (this) {
                    sharedVar++;
                }
                latch.countDown();
            }

            @Override
            public void thread3() {
                thread2();
            }

            @Override
            public void thread4() {
                thread2();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "After CyclicBarrier only one thread increments shared int")
    public void cyclicBarrier() {
        new ThreadRunner(4) {
            CyclicBarrier barrier;

            @Override
            public void setUp() {
                barrier = new CyclicBarrier(4);
                sharedVar = 0;
            }

            @Override
            public void thread1() {
                synchronized (this) {
                    sharedVar++;
                    shortSleep();
                }
                try {
                    barrier.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void thread2() {
                thread1();
            }

            @Override
            public void thread3() {
                thread1();
            }

            @Override
            public void thread4() {
                thread1();
                sharedVar++;
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "Semaphore")
    public void semaphore() {
        final Semaphore semaphore = new Semaphore(0);
        new ThreadRunner(2) {

            @Override
            public void thread1() {
                longSleep();
                sharedVar = 1;
                semaphore.release();
            }

            @Override
            public void thread2() {
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                sharedVar = 2;
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "ReadWriteLock: write locks only")
    public void writeLocksOnly() {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        new ThreadRunner(2) {
            @Override
            public void thread1() {
                lock.writeLock().lock();
                int v = sharedVar;
                lock.writeLock().unlock();
            }

            @Override
            public void thread2() {
                lock.writeLock().lock();
                sharedVar++;
                lock.writeLock().unlock();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "ReadWriteLock: both read and write locks")
    public void readAndWriteLocks() {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        new ThreadRunner(4) {
            @Override
            public void thread1() {
                lock.readLock().lock();
                int v = sharedVar;
                lock.readLock().unlock();
            }

            @Override
            public void thread2() {
                thread1();
            }

            @Override
            public void thread3() {
                lock.writeLock().lock();
                int v = sharedVar;
                lock.writeLock().unlock();
            }

            @Override
            public void thread4() {
                lock.writeLock().lock();
                sharedVar++;
                lock.writeLock().unlock();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "ReentrantReadWriteLock: tryLock")
    public void readAndWriteTryLocks() {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        new ThreadRunner(3) {
            @Override
            public void thread1() {
                while (!lock.readLock().tryLock()) {
                    shortSleep();
                }
                int v = sharedVar;
                lock.readLock().unlock();
            }

            @Override
            public void thread2() {
                try {
                    while (!lock.readLock().tryLock(1, TimeUnit.MILLISECONDS)) {
                        shortSleep();
                    }
                    int v = sharedVar;
                    lock.readLock().unlock();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void thread3() {
                while (!lock.writeLock().tryLock()) {
                    shortSleep();
                }
                sharedVar++;
                lock.writeLock().unlock();
            }

            @Override
            public void thread4() {
                try {
                    while (!lock.writeLock().tryLock(1, TimeUnit.MILLISECONDS)) {
                        shortSleep();
                    }
                    sharedVar++;
                    lock.writeLock().unlock();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Exception in test tryLock", e);
                }
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "ReentrantLock: simple access")
    public void reentrantLockSimple() {
        final ReentrantLock lock = new ReentrantLock();
        new ThreadRunner(2) {
            @Override
            public void thread1() {
                lock.lock();
                sharedVar++;
                lock.unlock();
            }

            @Override
            public void thread2() {
                thread1();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "ReentrantLock: tryLocks")
    public void tryLock2() {
        final ReentrantLock lock = new ReentrantLock();
        new ThreadRunner(3) {
            @Override
            public void thread1() {
                while (!lock.tryLock()) {
                    shortSleep();
                }
                sharedVar++;
                lock.unlock();
            }

            @Override
            public void thread2() {
                try {
                    while (!lock.tryLock(1, TimeUnit.MILLISECONDS)) {
                        shortSleep();
                    }
                    sharedVar++;
                    lock.unlock();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Exception in test tryLock2", e);
                }
            }

            @Override
            public void thread3() {
                lock.lock();
                sharedVar++;
                lock.unlock();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "AtomicInteger increment")
    public void atomicInteger() {
        final AtomicInteger i = new AtomicInteger();
        new ThreadRunner(4) {

            @Override
            public void thread1() {
                i.incrementAndGet();
            }

            @Override
            public void thread2() {
                thread1();
            }

            @Override
            public void thread3() {
                thread1();
            }

            @Override
            public void thread4() {
                thread1();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "ConcurrentHashMap accesses")
    public void concurrentHashMap() {
        final ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
        new ThreadRunner(4) {

            @Override
            public void thread1() {
                map.put(1, 1);
            }

            @Override
            public void thread2() {
                map.put(1, 2);
            }

            @Override
            public void thread3() {
                thread2();
            }

            @Override
            public void thread4() {
                thread1();
            }
        };
    }

    // Example from http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/
    // concurrent/locks/LockSupport.java?view=markup .
    class FIFOMutex {
        private final AtomicBoolean locked = new AtomicBoolean(false);
        private final Queue<Thread> waiters
        = new ConcurrentLinkedQueue<Thread>();

        public void lock() {
            boolean wasInterrupted = false;
            Thread current = Thread.currentThread();
            waiters.add(current);

            // Block while not first in queue or cannot acquire lock.
            while (waiters.peek() != current ||
                    !locked.compareAndSet(false, true)) {
                LockSupport.park(this);
                if (Thread.interrupted()) // ignore interrupts while waiting.
                    wasInterrupted = true;
            }

            waiters.remove();
            if (wasInterrupted)          // reassert interrupt status on exit.
                current.interrupt();
        }

        public void unlock() {
            locked.set(false);
            LockSupport.unpark(waiters.peek());
        }
    }

    @RaceTest(expectRace = false,
            description = "Use custom reentrant lock - FIFOMutex. " +
            "Test LockSupport happens-before relations")
    public void fifoMutexUser() {
        new ThreadRunner(4) {
            FIFOMutex mu;

            @Override
            public void setUp() {
                mu = new FIFOMutex();
            }

            @Override
            public void thread1() {
                mu.lock();
                shortSleep();
                sharedVar++;
                mu.unlock();
            }

            @Override
            public void thread2() {
                thread1();
            }

            @Override
            public void thread3() {
                thread1();
            }

            @Override
            public void thread4() {
                thread1();
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "Test happens-before relations between FutureTask calculation and get")
    public void futureTask() {
        FutureTask<int[]> future = new FutureTask<int[]>(new Callable<int[]>() {
            @Override
            public int[] call() {
                int[] res = new int[1];
                res[0] = 42;
                return res;
            }
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(future);
        try {
            int[] futureRes = future.get();
            futureRes[0]++;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
    }

    @ExcludedTest(reason = "SynchronousQueue is not supported yet")
    @RaceTest(expectRace = false, description = "Test SynchronousQueue")
    public void synchronousQueue() {
        final SynchronousQueue<String> queue = new SynchronousQueue<String>();
        new ThreadRunner(2) {
            @Override
            public void thread1() {
                try {
                    sharedVar++;
                    queue.put(new String("test"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void thread2() {
                try {
                    String s = queue.take();
                    sharedVar++;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static void main(String[] args) {
        JUConcurrentTests tests = new JUConcurrentTests();
        // positive tests
        if (args[0].equals("positive")) {
            // positive tests
            tests.writingUnderReaderLock();
            tests.differentLocksWW2();
            tests.cyclicBarrierWrong();
            tests.lockNeMonitor();
        } else {
            // negative tests
            tests.arrayBlockingQueue(); // testing the internal of ABQ
            tests.arrayBlockingQueue2(); // testing HB relation imposed by ABQ.put/take
            tests.lockInterruptibly();
            tests.reentrantLockInterruptibly();
            tests.countDownLatch();
            tests.cyclicBarrier();
            tests.semaphore();
            tests.writeLocksOnly();
            tests.readAndWriteLocks();
            tests.readAndWriteTryLocks();
            tests.reentrantLockSimple();
            tests.tryLock2();
            tests.atomicInteger();
            tests.concurrentHashMap();
            tests.fifoMutexUser();
//            tests.futureTask();
//            tests.synchronousQueue();
        }
    }

}