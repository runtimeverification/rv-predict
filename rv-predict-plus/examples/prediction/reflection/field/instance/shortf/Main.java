package reflection.field.instance.shortf;

import java.lang.reflect.*;

public class Main {
   public static short b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.shortf.Main").getField("b").getShort(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.shortf.Main").getField("b").setShort(instance,(short)10000);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
