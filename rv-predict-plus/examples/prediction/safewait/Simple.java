package safewait;
public class Simple extends Thread {
   static public int i = 1;
   static Object lock = new Object();
   public static void main(String[] args) {
      (new Simple()).start();
      (new Simple()).start();
      try { sleep(1000); } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      synchronized (lock) { lock.notify(); }
   }
   public void run() {
      try {
         synchronized (lock) {lock.wait(); }
         i++;
         synchronized (lock) { lock.notify(); }
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
