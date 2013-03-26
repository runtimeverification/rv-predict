package nonssabug;

public class Main extends Thread {
   static public int i = 1;
   static Object lock;
   public static void main(String[] args) {
      lock = new Object();
      (new Main()).start();
      (new Main()).start();
   }
   public void run() {
      for(int z = 0; z < 100; z++) {
         i += z;
      }
   }
}
