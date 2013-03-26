package staticsynch;
public class Main extends Thread {
  static public int i = 1;
  static Object lock;
  public static void main(String[] args) {
    (new Main()).start();
    (new Main()).start();
  }
  public void run() {
    i = incr(i);
    System.out.println(i);
  }
  public static synchronized int incr(int i) {
    return i+1;
  }
}
