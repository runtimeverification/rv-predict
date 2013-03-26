package reflection.field.instance.longf;

import java.lang.reflect.*;

public class Main {
   public static long b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.longf.Main").getField("b").getLong(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.longf.Main").getField("b").setLong(instance,84L);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
