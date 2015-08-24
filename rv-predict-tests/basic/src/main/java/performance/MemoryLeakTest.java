package performance;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.LockSupport;

/**
 * Tests if there are memory leaks inside {@code RVPredictRuntime}.
 *
 * @author YilongL
 */
public class MemoryLeakTest {

    private static final Map<Integer, Integer> M = new TreeMap<>();
    static {
        for (int i = 0; i < 1000; i++) {
            M.put(i, i);
        }
    }

    public static void main(String[] args) {
        /* create a lot of short-lived views and iterators */
        for (int i = 0; i < 10000000; i++) {
            Map<Integer, Integer> map = new TreeMap<>(M);
            map.keySet().iterator();
            map.values().iterator();
            map.entrySet().iterator();
            if (i % 100 == 0) {
                // sleep to be nice to the profiling tool
                LockSupport.parkNanos(1);
            }
        }
    }

}
