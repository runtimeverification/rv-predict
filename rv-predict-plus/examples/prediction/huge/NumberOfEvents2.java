package huge;

public class NumberOfEvents2 extends Thread {
  static long x = 0;
  static Integer z = new Integer(0);

  public static void main(String[] args){
    NumberOfEvents2 l1 = new NumberOfEvents2();
    NumberOfEvents2 l2 = new NumberOfEvents2();
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
    ++x;
    for(long i = 0; i < 10000; ++i){
      synchronized(z){
        x = x + i;
      }
    }
  }
}
