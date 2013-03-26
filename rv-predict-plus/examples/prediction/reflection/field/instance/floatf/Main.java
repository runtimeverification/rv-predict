package reflection.field.instance.floatf;

import java.lang.reflect.*;

public class Main {
   public static float b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.floatf.Main").getField("b").getFloat(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.floatf.Main").getField("b").setFloat(instance,(float)8.1);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
