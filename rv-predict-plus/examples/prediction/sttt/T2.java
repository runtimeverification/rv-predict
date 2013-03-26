package sttt;

public class T2 extends Thread { 
  public void run(){
    synchronized(Main.Lck){
      int tmp2 = Main.X;
      ++tmp2;
	  Main.X = tmp2;
	}
    Main.Z = 1;
  }
}
