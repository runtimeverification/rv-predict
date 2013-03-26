package reflection.field.instance.intf;

import java.lang.reflect.*;

public class Main {
   public static int b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.intf.Main").getField("b").getInt(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.intf.Main").getField("b").setInt(instance,3);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
