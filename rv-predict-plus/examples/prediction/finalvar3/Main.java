package finalvar3;

class Main {
   final static Object lock = new Object();
   static Main leak;
   final int i;
   Main() {
      leak = this;
      synchronized(lock) { lock.notify();
      try {lock.wait();} catch (Exception e) {} }
      i = 1;
   }
   public static void main(String[] args) {
      (new Thread() { @Override public void run() {
         synchronized(lock) { try {lock.wait();} catch (Exception e) {} }
         System.out.println(leak.i);
         synchronized(lock) { lock.notify(); }
      }}).start();
      (new Thread() { @Override public void run() {
         new Main();
      }}).start();
   }
}
