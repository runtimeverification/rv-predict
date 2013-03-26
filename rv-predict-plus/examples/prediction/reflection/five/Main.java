package reflection.five;

import java.lang.reflect.*;

public class Main {
   public static int i = 0;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.five.Main").getField("i").get(null);
            System.out.println("A: " + i);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      i = 42;
      System.out.println("B: " + i);
   }
}
