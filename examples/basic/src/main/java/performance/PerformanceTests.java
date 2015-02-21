package performance;

/**
 * Collections of performance tests.
 *
 * @author YilongL
 */
public class PerformanceTests {

    long x;

    private void testLoggingPutField() {
        for (long i = 0; i < 5000000000l; i++) {
            x = i;
        }
    }

    public static void main(String[] args) {
        new PerformanceTests().testLoggingPutField();
    }

}
