package sttt;

public class Main {
  static int Z;
  static int X;
  static Integer Lck = new Integer(0);

  public static void main(String[] args){
     T1 t1 = new T1();
 	 t1.start();
	 T2 t2 = new T2();
     t2.start();
	 try {
       t1.join();
	   t2.join();
	 } catch (Exception e){
       e.printStackTrace();
	   System.exit(1);
	 }
	 System.out.println(Z);
  }
}
