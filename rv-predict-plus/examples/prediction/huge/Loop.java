//package tight;
package huge; // not sure why it was tight, maybe incomplete example? soot errors due to dynamic-package otherwise

public class Loop extends Thread {
  static long x = 0;

  public static void main(String[] args){
    Loop l1 = new Loop();
    Loop l2 = new Loop();
    l1.start();
    l2.start();
    try {
      l1.join();
      l2.join();
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println(x);
  }

  public void run(){
    for(long i = 0; i < 100; ++i){
      x = x + i;
    }
  }
}
