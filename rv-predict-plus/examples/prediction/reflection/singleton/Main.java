package reflection.singleton;

import java.lang.reflect.*;

public class Main {
   public static class Singleton {
      final static Singleton instance = new Singleton();
      public static Singleton v() { return instance; }
      int i;
      Singleton() {
         i = 2;
      }
   }
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.singleton.Main$Singleton").getMethod("v").invoke(null);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      System.out.println("B: " + Singleton.v().i);
   }
}
