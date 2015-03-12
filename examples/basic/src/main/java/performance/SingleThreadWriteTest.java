package performance;

/**
 * Single thread writing to a field inside a loop.
 *
 * @author YilongL
 */
public class SingleThreadWriteTest {

    static long x;

    public static void main(String[] args) {
        for (int i = 0; i < 500000; i++) { // warm up
            x = i;
        }

        long startTime = System.currentTimeMillis();
        for (long i = 0; i < 10000000l; i++) { // loop 10 million times
            x = i;
        }
        long endTime = System.currentTimeMillis();
        System.err.println("Time elapsed: " + (endTime - startTime) + "ms");
    }

}
