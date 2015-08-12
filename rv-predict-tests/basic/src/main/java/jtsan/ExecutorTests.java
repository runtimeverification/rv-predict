package jtsan;

import java.util.concurrent.*;



public class ExecutorTests {

    static int sharedVar;

    static final Runnable SIMPLE_TASK = () -> sharedVar++;

    private static void awaitTermination(ExecutorService executor) {
        try {
            while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) { }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void fixedThreadPoolRaceyTasks() {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Runnable task = () -> sharedVar++;
        for (int i = 0; i < 10; i++) {
            executor.execute(task);
        }
        executor.shutdown();
        awaitTermination(executor);
    }

    public void cachedThreadPoolRaceyTasks() {
        ExecutorService executor = Executors.newCachedThreadPool();
        Runnable task = () -> sharedVar++;
        for (int i = 0; i < 10; i++) {
            executor.execute(task);
        }
        executor.shutdown();
        awaitTermination(executor);
    }

    public void shutdownWrong() {
        ExecutorService executor0 = Executors.newCachedThreadPool();
        ExecutorService executor1 = Executors.newCachedThreadPool();
        Runnable task = () -> sharedVar++;
        executor0.execute(task);
        /* shutdown() does not wait for previously submitted tasks to complete execution */
        executor0.shutdown();
        executor1.execute(task);
        executor1.shutdown();
        awaitTermination(executor0);
        awaitTermination(executor1);
    }

    public void executorThreadStartJoin() {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        sharedVar++;
        executor.execute(SIMPLE_TASK);
        executor.shutdown();
        awaitTermination(executor);
        sharedVar++;
    }

    public void singleThreadExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        sharedVar++;
        for (int i = 0; i < 10; i++) {
            executor.execute(SIMPLE_TASK);
        }
        executor.shutdown();
        awaitTermination(executor);
        sharedVar++;
    }

    public void scheduledThreadExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        sharedVar++;
        for (int i = 0; i < 5; i++) {
            // test ScheduledThreadPoolExecutor$DelayWorkQueue
            executor.scheduleWithFixedDelay(() -> { int x = sharedVar; }, 0, 100, TimeUnit.MILLISECONDS);
        }
        executor.shutdown();
        awaitTermination(executor);
    }

    public static void main(String[] args) {
        ExecutorTests tests = new ExecutorTests();
        // positive tests
        if (args[0].equals("positive")) {
            tests.fixedThreadPoolRaceyTasks();
            tests.cachedThreadPoolRaceyTasks();
            tests.shutdownWrong();
        } else {
            // negative tests
            tests.executorThreadStartJoin();
            tests.singleThreadExecutor();
            tests.scheduledThreadExecutor();
        }
    }
}