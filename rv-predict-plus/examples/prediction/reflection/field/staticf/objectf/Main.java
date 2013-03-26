package reflection.field.staticf.objectf;

import java.lang.reflect.*;

public class Main {
   public static Object b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.objectf.Main").getField("b").get(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.objectf.Main").getField("b").set(null,new Object());
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
