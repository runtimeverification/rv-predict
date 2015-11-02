// Copyright (c) 2015 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

package examples;

public abstract class ThreadRunner {

    private final Thread thread1;

    private final Thread thread2;

    public abstract void thread1();

    public abstract void thread2();

    public ThreadRunner() {
        thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                thread1();
            }
        });
        thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                thread2();
            }
        });
        thread1.start();
        thread2.start();
    }

}
