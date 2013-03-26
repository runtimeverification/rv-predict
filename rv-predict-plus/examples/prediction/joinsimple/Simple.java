package joinsimple;
public class Simple extends Thread {
   static public int i = 1;
   static Object lock;
   public static void main(String[] args) {
      try {
         lock = new Object();
         Thread t1 = (new Simple());
         t1.start();
         Thread t2 = (new Simple());
         t1.join();
         t2.start();
         t2.join();
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
   public void run() {
      i++;
   }
}
