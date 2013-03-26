package constructor;
public class Simple extends Thread {
   public int i;
   static Simple leak;
   Simple() {
      leak = this;
      i = 0;
   }
   public static void main(String[] args) {
      (new Thread() {
         @Override public void run() {
            new Simple();
         }
      }).start();
      (new Thread() {
         @Override public void run() {
            leak.i = 1;
         }
      }).start();
   }
}
