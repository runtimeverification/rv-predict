package cdep;

public class T1 extends Thread {
  public void run(){
    ++Main.x;
    Main.lock = 1;
  }

}
