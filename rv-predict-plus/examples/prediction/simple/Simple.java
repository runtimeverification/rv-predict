package simple;
public class Simple extends Thread {
   static public int i = 1;
   static Object lock = new Object();
   public static void main(String[] args) {
      (new Simple()).start();
      (new Simple()).start();
   }
   public void run() {
  //   synchronized(lock){
       i++;
       System.out.println(i);
    // }
  }
}
