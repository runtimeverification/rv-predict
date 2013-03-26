package reflection.two;

public class Main {
   private static int i = 0;
   void inc() { i++; }
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            ((Main)Class.forName("reflection.two.Main").newInstance()).inc();
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      System.out.println("B: "+i);
   }
}
