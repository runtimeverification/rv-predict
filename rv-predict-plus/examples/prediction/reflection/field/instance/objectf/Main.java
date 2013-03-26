package reflection.field.instance.objectf;

import java.lang.reflect.*;

public class Main {
   public static Object b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.objectf.Main").getField("b").get(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.objectf.Main").getField("b").set(instance,new Object());
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
