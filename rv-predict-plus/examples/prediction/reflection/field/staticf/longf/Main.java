package reflection.field.staticf.longf;

import java.lang.reflect.*;

public class Main {
   public static long b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.longf.Main").getField("b").getLong(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.longf.Main").getField("b").setLong(null,84L);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
