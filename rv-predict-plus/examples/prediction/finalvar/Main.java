package finalvar;

class Main {
   static Main leak;
   final int i;
   Main() {
      leak = this;
      i = 1;
   }
   public static void main(String[] args) {
      (new Thread() { @Override public void run() {
         new Main();
      }}).start();
      (new Thread() { @Override public void run() {
         System.out.println(leak.i);
      }}).start();
   }
}
