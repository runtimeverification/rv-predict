package impure;
public class Simple extends Thread {
   public static void main(String[] args) {
      (new Simple()).start();
       System.out.println(1);
   }
   public void run() {
       System.out.println(0);
   }
}
