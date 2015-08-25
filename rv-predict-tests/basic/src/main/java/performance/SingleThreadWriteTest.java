package performance;

/**
 * Single thread writing to an array.
 *
 * @author YilongL
 */
public class SingleThreadWriteTest {

    public static void main(String[] args) {
        long[] a = new long[1000];
        for (int i = 0; i < 500000; i++) { // warm up
            a[i % a.length] = i;
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {   // loop 10 million times
            for (int k = 0; k < 1000; k++) {
                a[k] = i;
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Time elapsed: " + (endTime - startTime) + "ms");
    }

}
