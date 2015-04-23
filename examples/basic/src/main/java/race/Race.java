package race;

/**
 * Created by Traian on 23.04.2015.
 */
public class Race {
    private static int Global;
    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    Global = 42;
                }
            }
        };
        t.start();
        Global = 43;
        t.join();
        if (Global == 0) System.exit(1);
    }
}
