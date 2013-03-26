package waitfile;

import java.io.FileWriter;
import java.io.IOException;

public class File extends Thread {
  static FileWriter f; static { try {f = new FileWriter("./tmp");} catch(IOException e) { e.printStackTrace();}}
  static final Object lock = new Object();
  public static void main(String[] args){
    (new Thread() { @Override public void run() {
      try{
        synchronized(lock) {
          lock.wait();
          sleep(400);
          f.write("write");
        }} catch(Exception e) { e.printStackTrace(); }
    }}).start();
    (new Thread() { @Override public void run() {
      try{
        synchronized(lock) {
          f.close();
        }} catch(Exception e) { e.printStackTrace(); }
    }}).start();
    synchronized(lock) {
      lock.notify();
    }
  }
}
// vim: tw=100:sw=2
