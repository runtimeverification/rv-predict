package reflection.field.instance.doublef;

import java.lang.reflect.*;

public class Main {
   public double b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.doublef.Main").getField("b").getDouble(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.doublef.Main").getField("b").setDouble(instance,1.1);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
