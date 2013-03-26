package runtimeexcept;

public class Main extends Thread {
  public static int x;

  public Main(int i){
    x = i;
  }

  public static void main(String[] args){
     try{
       if(new Integer(args[0]) == 0) throw new RuntimeException();
     } catch (RuntimeException e){
       e.printStackTrace();
     }
     (new Main(new Integer(args[0]))).start();
     (new Main(new Integer(args[0]))).start();
  }

  public int yo(int y){
    if(y == 1) return y;
    System.out.println(y);
    if(y == 2) return y;
    throw new RuntimeException();
  }

  public int zo(int y){
    return yo(y);
  }

  public void run(){
    try {
      x = zo(x);
    } catch(Exception e){
      e.printStackTrace();
    }
    System.out.println((double)x);
  }

}
