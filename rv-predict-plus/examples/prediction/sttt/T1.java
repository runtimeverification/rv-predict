package sttt;

public class T1 extends Thread { 
  public void run(){
    Main.Z = 0;
    synchronized(Main.Lck){
      int tmp1 = Main.X;
      ++tmp1;
	  Main.X = tmp1;
	}
  }
}
