package reflection.field.staticf.floatf;

import java.lang.reflect.*;

public class Main {
   public static float b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.floatf.Main").getField("b").getFloat(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.floatf.Main").getField("b").setFloat(null,(float)8.1);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
