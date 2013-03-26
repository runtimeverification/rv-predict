package cdep;

public class T2 extends Thread {
  public void run(){
    if(Main.lock != 1){}
    ++Main.x;
  }
}
