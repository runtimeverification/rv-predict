package reflection.one;

public class Main {
   static int i = 0;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println("A: "+((Main)Class.forName("reflection.one.Main").newInstance()).i++);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      System.out.println("B: "+i);
   }
}
