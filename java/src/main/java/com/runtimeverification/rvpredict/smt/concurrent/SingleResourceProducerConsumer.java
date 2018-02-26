package com.runtimeverification.rvpredict.smt.concurrent;

import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /**
     * Producer class. Not thread safe in general, but its interactions with its consumers are thread-safe.
     */
    public static class Producer<R> {
        private final MutableObject<R> resource = new MutableObject<>();
        // Should NOT waitUntilSignaled on this when the resource lock is held.
        private final SignalWaiter consumerChangeSignal = new SignalWaiter();

        // Should be touched only when the resource lock is held.
        private boolean shouldFinish = false;
        // Should be touched only when the resource lock is held.
        private Optional<Exception> exception = Optional.empty();
        // Should be touched only when the resource lock is held.
        private int waitingConsumers = 0;
        // Should be touched only when the resource lock is held.
        private int deadConsumers = 0;

        private List<Thread> consumers = new ArrayList<>();

        public void process(R currentResource) throws Exception {
            waitUntilNoDataQueued();
            synchronized (resource) {
                resource.setValue(currentResource);
                resource.notify();
            }
        }

        public void finishAllWork() throws Exception {
            waitUntilNoDataQueued();
            // Wait until all consumers are waiting for input data.
            while (true) {
                synchronized (resource) {
                    checkForExceptions();
                    if (waitingConsumers + deadConsumers == consumers.size()) {
                        break;
                    }
                }
                consumerChangeSignal.waitUntilSignaled();
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
            synchronized (resource) {
                checkForExceptions();
            }
        }

        // Only call when the resource lock is held.
        private void checkForExceptions() throws Exception {
            Optional<Exception> localException = exception;
            if (localException.isPresent()) {
                shouldFinish = true;
                resource.notifyAll();
                throw new Exception(localException.get());
            }
        }

        private void waitUntilNoDataQueued() throws Exception {
            while (true) {
                synchronized (resource) {
                    checkForExceptions();
                    if (resource.getValue() == null) {
                        break;
                    }
                }
                consumerChangeSignal.waitUntilSignaled();
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
                while (!producer.shouldFinish) {
                    Optional<R> aquiredResource = Optional.empty();
                    synchronized (producer.resource) {
                        if (producer.shouldFinish) {
                            break;
                        }
                        try {
                            if (producer.resource.getValue() == null) {
                                producer.waitingConsumers++;
                                producer.consumerChangeSignal.signal();
                                producer.resource.wait();
                                producer.waitingConsumers--;
                            }
                        } catch (InterruptedException ignored) {
                        }
                        if (producer.resource.getValue() != null) {
                            aquiredResource = Optional.of(producer.resource.getValue());
                            producer.resource.setValue(null);
                            producer.consumerChangeSignal.signal();
                        }
                    }
                    if (aquiredResource.isPresent()) {
                        try {
                            resourceProcessor.process(aquiredResource.get());
                        } catch (Exception e) {
                            synchronized (producer.resource) {
                                producer.exception = Optional.of(e);
                                producer.consumerChangeSignal.signal();
                            }
                            return;
                        }
                    }
                }
            } finally {
                synchronized (producer.resource) {
                    producer.deadConsumers++;
                }
            }
        }


    }
}
