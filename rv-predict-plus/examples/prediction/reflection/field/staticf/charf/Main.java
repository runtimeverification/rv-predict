package reflection.field.staticf.charf;

import java.lang.reflect.*;

public class Main {
   public static char b;
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            System.out.println(Class.forName("reflection.field.staticf.charf.Main").getField("b").getChar(null));
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.staticf.charf.Main").getField("b").setChar(null,'C');
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
