package reflection.field.staticf.intf;

import java.lang.reflect.*;

public class Main {
   public static int b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.intf.Main").getField("b").getInt(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.intf.Main").getField("b").setInt(null,3);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
