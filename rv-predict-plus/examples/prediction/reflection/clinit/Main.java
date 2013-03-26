package reflection.clinit;

public class Main {
   static int i;
   public static class Inner {
      static { i = 4; System.out.println("clinit"); }
   }
   public static void main(String[] args) {
      System.out.println("start");
      new Thread() { @Override public void run() {
         new Inner();
      }}.start();
      try { Thread.sleep(1000);
         Class.forName("reflection.clinit.Main$Inner");
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      i = 9;
      System.out.println("done");
   }
}
