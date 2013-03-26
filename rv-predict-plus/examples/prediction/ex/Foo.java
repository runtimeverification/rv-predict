package ex;

public class Foo extends Thread{
  static int x = 0;
  public static void main(String[] args){
    (new Foo()).start();
    (new Foo()).start();
  }

  public void run(){
    a();
  }

  public static void a(){
    b();
  }

  public static void b(){
   try {
     c();
   } catch (RuntimeException e){
     e.printStackTrace();
   }
  }

  public static void c(){
    d();
  }

  public static void d(){
    Bar.bar();
  }

  public static void e(){
    ++x;
    throw new RuntimeException();
  }
}
