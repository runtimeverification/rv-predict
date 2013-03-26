package reflection.field.instance.booleanf;

import java.lang.reflect.*;

public class Main {
   public boolean b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.booleanf.Main").getField("b").getBoolean(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.booleanf.Main").getField("b").setBoolean(instance,true);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
