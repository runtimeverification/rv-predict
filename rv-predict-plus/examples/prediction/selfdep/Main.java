package selfdep; 

public class Main extends Thread{
  static int x = 0;
 
  public static void main(String[] args){
    (new Main()).start();
    Main m = new Main();
    m.start();
    try{
      m.sleep(1);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public void run(){
    int i = 0;
    while(x != 0){ if((i++ % 100) == 0) System.out.println(this + " is busy waiting");}
    x = 1;
    System.out.println(this + " is in the critical section");
    try {
      sleep(1);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    x = 0;
  }

}
