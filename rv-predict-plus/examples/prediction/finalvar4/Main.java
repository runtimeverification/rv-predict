package finalvar4;

class Main {
   final static Object lock = new Object();
   static Main leak;
   final int i = 3;
   Main() {
      leak = this;
      synchronized(lock) { lock.notify(); }
   }
   public static void main(String[] args) {
      (new Thread() { @Override public void run() {
         synchronized(lock) { try {lock.wait();} catch (Exception e) {} }
         System.out.println(leak.i);
      }}).start();
      (new Thread() { @Override public void run() {
         new Main();
      }}).start();
   }
}
