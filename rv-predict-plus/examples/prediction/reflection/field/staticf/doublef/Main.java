package reflection.field.staticf.doublef;

import java.lang.reflect.*;

public class Main {
   public static double b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.doublef.Main").getField("b").getDouble(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.doublef.Main").getField("b").setDouble(null,1.1);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
