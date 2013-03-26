package file;

import java.io.FileWriter;

public class File extends Thread {
  public static FileWriter f = null;
  public static void main(String[] args){
    try{
      f = new FileWriter("./tmp");
      (new File()).start();
      sleep(400);
      f.close();
    } catch(Exception e) { e.printStackTrace(); }
  }
  public void run(){
    try{
      f.write("this is some text that I am writing\n"); 
      System.out.println("writing file");
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}
