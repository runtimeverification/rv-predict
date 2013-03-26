package reflection.field.instance.charf;

import java.lang.reflect.*;

public class Main {
   public char b;
   private static Main instance = new Main();
   public static void main(String[] args) {
      new Thread() { @Override public void run() {
         try {
            Class.forName("reflection.field.instance.charf.Main").getField("b").getChar(instance);
         } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
      try {
         Class.forName("reflection.field.instance.charf.Main").getField("b").setChar(instance,'C');
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
}
