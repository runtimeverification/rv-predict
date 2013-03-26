package pseudosafecdep;

public class T2 extends Thread {
  public void run(){
    while(Main.lock != 1){}
    ++Main.x;
  }
}
