package emptyfor;
public class Simple extends Thread {
   static public int i = 1;
   static Object lock;
   public static void main(String[] args) {
      lock = new Object();
      (new Simple()).start();
      (new Simple()).start();
   }
   public void run() {
      for(int z = 0; z < 100; z++) {}
         i++;
   }
}
