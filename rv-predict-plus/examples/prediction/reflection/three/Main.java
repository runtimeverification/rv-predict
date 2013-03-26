package reflection.three;

import java.lang.reflect.*;

public class Main {
   private static int i = 0;
   public static void inc() { i++; }
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.three.Main").getMethod("inc").invoke(null);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      System.out.println("B: "+i);
   }
}
