package subtle;
public class MyThread extends Thread {
   static public MyLock lock;
   static public int threadCount;
   public static void main(String[] args) {
      System.out.println("running MyThread");
      lock = new MyLock();
      threadCount = 0;
      (new MyThread()).start();
      (new MyThread()).start();
   }
   public void run() {
      if (lock == null)
         return;
      lock.tryAcquiring();
      MyThread.threadCount++;
      lock.release();
   }
}
