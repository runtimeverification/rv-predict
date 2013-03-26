package reflection.field.staticf.shortf;

import java.lang.reflect.*;

public class Main {
   public static short b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.shortf.Main").getField("b").getShort(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.shortf.Main").getField("b").setShort(null,(short)10000);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
