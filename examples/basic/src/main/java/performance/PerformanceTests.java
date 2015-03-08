package performance;

/**
 * Collections of performance tests.
 *
 * @author YilongL
 */
public class PerformanceTests {

    long x;

    private void testLoggingPutField() {
        for (int i = 0; i < 500000; i++) {
            x = i;
        }

        long startTime = System.currentTimeMillis();
        for (long i = 0; i < 1000000000l; i++) {
            x = i;
        }
        long endTime = System.currentTimeMillis();
        System.err.println("Time elapsed: " + (endTime - startTime) + "ms");
    }

    public static void main(String[] args) {
        new PerformanceTests().testLoggingPutField();
    }

}
