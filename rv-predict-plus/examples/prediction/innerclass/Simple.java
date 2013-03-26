package innerclass;
public class Simple {
   static public int i;
   public static void main(String[] args) {
      (new Thread(){
         @Override public void run(){
            i = 1;
         }
      }).start();
      (new Thread(){
         @Override public void run(){
            i = 2;
         }
      }).start();
   }
}
