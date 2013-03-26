package subtle;
public class MyLock {
   public boolean flag;
   public int count;
   public MyLock() {
      flag = false;
      count = 0;
   }
   public boolean tryAcquiring() {
      synchronized (this) {
         if (flag)
            return false;
         flag = true;
      }
      count++;
      return true;
   }
   public void release() {
      synchronized (this) {
         flag = false;
      }
   }
}
