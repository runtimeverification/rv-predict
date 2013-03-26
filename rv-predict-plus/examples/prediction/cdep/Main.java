package cdep;

public class Main{

  static int lock = 0;
  static int x = 0;
 
  public static void main(String[] args){
    T1 t1 = new T1();
    T2 t2 = new T2();
    t1.start();
    t2.start();
    try{
      t1.join();
      t2.join();
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println(x); 
  }
}
