package safesimple;
public class Simple extends Thread {
   static public int i = 1;
   static Object lock;
   public static void main(String[] args) {
      lock = new Object();
      (new Simple()).start();
      (new Simple()).start();
   }
   public void run() {
     synchronized(lock){
       i++;
       System.out.println(i);
     }
   }
}
