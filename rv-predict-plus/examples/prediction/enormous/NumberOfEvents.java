package enormous;

public class NumberOfEvents extends Thread {
  static long x = 0;

  public static void main(String[] args){
    NumberOfEvents l1 = new NumberOfEvents();
    NumberOfEvents l2 = new NumberOfEvents();
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
    for(long i = 0; i < 10000000; ++i){
      x = x + i;
    }
  }
}
