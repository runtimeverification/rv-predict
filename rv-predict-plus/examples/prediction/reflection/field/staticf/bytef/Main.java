package reflection.field.staticf.bytef;

import java.lang.reflect.*;

public class Main {
   public static byte b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.bytef.Main").getField("b").getByte(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.bytef.Main").getField("b").setByte(null,(byte)100);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
