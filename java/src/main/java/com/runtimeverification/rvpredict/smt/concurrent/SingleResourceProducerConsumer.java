package com.runtimeverification.rvpredict.smt.concurrent;

import com.runtimeverification.rvpredict.runtime.java.util.concurrent.Semaphore;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Producer-consumer implementation with a single consumable item. Consumer exceptions are rethrown in the producer
 * thread.
 *
 * It is the responsibility of the client to start the consumer threads.
 */
public class SingleResourceProducerConsumer {
    /**
     * Interface for processing a resource in one of the consumers.
     * Must be thread safe.
     */
    public interface ResourceProcessor<R> {
        void process(R resource) throws Exception;
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

    /**
     * Producer class. Not thread safe in general, but its interactions with its consumers are thread-safe.
     *
     * It expects that the public interface is called from a single thread, in which case
     * addTask can be used to wrap callbacks in order to make sure that they run on that thread.
     *
     * If the public interface is called from multiple threads, besides the usual requirement that the
     * calls are synchronized, one must make sure that the callbacks passed to the producer can run on
     * any of these threads.
     */
    public static class Producer<R> {
        private final MutableObject<R> resource = new MutableObject<>();
        // Should NOT acquire this when the resource lock is held.
        private final Semaphore stateChangeSignal = new Semaphore(0);

        // Should be touched only when the resource lock is held.
        private boolean shouldFinish = false;
        // Should be touched only when the resource lock is held.
        private List<RunnableWithException> pendingTasks = new ArrayList<>();
        // Should be touched only when the resource lock is held.
        private int waitingConsumers = 0;
        // Should be touched only when the resource lock is held.
        private int finishedConsumers = 0;

        private List<Thread> consumers = new ArrayList<>();

        public void process(R currentResource) throws Exception {
            waitForEmptyResource();
            synchronized (resource) {
                resource.setValue(currentResource);
                resource.notify();
            }
        }

        public void finishAllWork() throws Exception {
            waitForEmptyResource();
            // Wait until all consumers are waiting for input data.
            while (true) {
                runPendingTasks();
                synchronized (resource) {
                    if (!pendingTasks.isEmpty()) {
                        continue;
                    }
                    if (waitingConsumers + finishedConsumers == consumers.size()) {
                        break;
                    }
                }
                stateChangeSignal.acquire();
            }
        }

        public void stopAllConsumers() throws Exception {
            synchronized (resource) {
                shouldFinish = true;
                resource.notifyAll();
            }
            for (Thread thread : consumers) {
                thread.join();
            }
            runPendingTasks();
        }

        /**
         * addTask can wrap a callback in order to run it on the producer thread.
         */
        public void addTask(RunnableWithException task) {
            synchronized (resource) {
                pendingTasks.add(task);
                stateChangeSignal.release();
            }
        }

        // Only call when the resource lock is held.
        private void runPendingTasks() throws Exception {
            List<RunnableWithException> localPendingTasks;
            synchronized (resource) {
                if (pendingTasks.isEmpty()) {
                    return;
                }
                localPendingTasks = pendingTasks;
                pendingTasks = new ArrayList<>();
            }
            for (RunnableWithException task : localPendingTasks) {
                task.run();
            }
        }

        private void addException(Exception e) {
            synchronized (resource) {
                pendingTasks.add(() -> {
                    synchronized (resource) {
                        shouldFinish = true;
                        resource.notifyAll();
                        throw new Exception(e);
                    }
                });
                stateChangeSignal.release();
            }
        }

        private void waitForEmptyResource() throws Exception {
            while (true) {
                runPendingTasks();
                synchronized (resource) {
                    if (!pendingTasks.isEmpty()) {
                        continue;
                    }
                    if (resource.getValue() == null) {
                        break;
                    }
                }
                stateChangeSignal.acquire();
            }
        }

        private void registerConsumer(Consumer consumer) {
            consumers.add(consumer);
        }
    }

    /**
     * Consumer class. Not thread safe in general, but its interactions with the producer are thread-safe.
     */
    public static class Consumer<R> extends Thread {
        private final Producer<R> producer;
        private final ResourceProcessor<R> resourceProcessor;

        public Consumer(Producer<R> producer, ResourceProcessor<R> resourceProcessor) {
            this.producer = producer;
            this.resourceProcessor = resourceProcessor;
            producer.registerConsumer(this);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    R acquiredResource;
                    synchronized (producer.resource) {
                        // Must be synchronized, otherwise the producer could set shouldFinish after the test,
                        // then call notifyAll, after which the consumer could start waiting forever.
                        if (producer.shouldFinish) {
                            break;
                        }
                        try {
                            // Should test this in case it was set when the consumer was outside the synchronized block.
                            if (producer.resource.getValue() == null) {
                                producer.waitingConsumers++;
                                // Signal that a consumer started waiting.
                                producer.stateChangeSignal.release();
                                producer.resource.wait();
                                producer.waitingConsumers--;
                            }
                        } catch (InterruptedException ignored) {
                        }
                        // Test for spurious wakeup and shouldFinish related notifications.
                        if (producer.resource.getValue() == null) {
                            continue;
                        }
                        acquiredResource = producer.resource.getValue();
                        producer.resource.setValue(null);
                        // Signal that a new resource is needed.
                        producer.stateChangeSignal.release();
                    }
                    try {
                        resourceProcessor.process(acquiredResource);
                    } catch (Exception e) {
                        producer.addException(e);
                        return;
                    }
                }
            } finally {
                synchronized (producer.resource) {
                    producer.finishedConsumers++;
                }
            }
        }
    }
}
