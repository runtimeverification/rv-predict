package reflection.field.instance.bytef;

import java.lang.reflect.*;

public class Main {
   public byte b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.instance.bytef.Main").getField("b").getByte(instance));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.bytef.Main").getField("b").setByte(instance,(byte)100);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
