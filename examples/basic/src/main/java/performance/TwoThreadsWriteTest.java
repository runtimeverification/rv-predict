package performance;

/**
 * Two threads writing to the same field inside a loop.
 *
 * @author YilongL
 */
public class TwoThreadsWriteTest {

    static long x;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 500000; i++) { // warm up
            x = i;
        }

        long startTime = System.currentTimeMillis();

        Thread t1 = new Thread(new Task());
        Thread t2 = new Thread(new Task());
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        long endTime = System.currentTimeMillis();
        System.err.println("Time elapsed: " + (endTime - startTime) + "ms");
    }

    static class Task implements Runnable {
        @Override
        public void run() {
            for (long i = 0; i < 5000000l; i++) { // loop 5 million times
                x = i;
            }
        }
    }

}
