package reflection.field.staticf.booleanf;

import java.lang.reflect.*;

public class Main {
   public static boolean b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.booleanf.Main").getField("b").getBoolean(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.booleanf.Main").getField("b").setBoolean(null,true);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
