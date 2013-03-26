package reflection.four;

import java.lang.reflect.*;

public class Main {
   public static int i = 0;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.four.Main").getField("i").set(null,5);
            System.out.println("A: " + i);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      System.out.println("B: " + i);
   }
}
