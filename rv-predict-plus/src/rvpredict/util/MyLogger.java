package rvpredict.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileOutputStream;

import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import java.lang.Runtime;

public class MyLogger {

  public static DeflaterOutputStream writer = null;
  public static String seperator = "~";

  // 0-9, 0 is none, 1 is speediest, 9 is most compression, 6 is gzip default
  private static int compressionLevel = 1;
  private static String name = "trace.rvpf";

  static {
    try {
      writer = new DeflaterOutputStream(new FileOutputStream(name), new Deflater(compressionLevel));
    } catch(Exception e) { e.printStackTrace(); System.exit(1); }
    Runtime.getRuntime().addShutdownHook(new Thread() { @Override public void run() { close(); } });
  }
  public static synchronized void logClassAccess(String cls, int ln){
    try {
      // forcing the static init happens first
      Class.forName(cls);
      StringBuilder buf = new StringBuilder();
      Thread currThread = Thread.currentThread();
      buf.append(currThread.getClass().getName());
      buf.append("@");
      buf.append(System.identityHashCode(currThread));
      buf.append(seperator);
      buf.append(ln);
      buf.append(seperator);
      buf.append(cls);
      buf.append(seperator);
      buf.append("!");
      buf.append('\n');
      writer.write(buf.toString().getBytes());
      writer.flush();
    } catch(Exception e) {throw new RuntimeException(e);}
  }
  public static synchronized void logFieldAccess(Object obj, int ln){
    // this method is called before the real access, so the obj can be null! -- this will trigger a nullpoint exception later
    if (obj == null){
      //System.err.println("Access a null object!");
      return;
    }
    try {
      StringBuilder buf = new StringBuilder();
      Thread currThread = Thread.currentThread();
      buf.append(currThread.getClass().getName());
      buf.append("@");
      buf.append(System.identityHashCode(currThread));
      buf.append(seperator);
      buf.append(ln);
      buf.append(seperator);
      buf.append(obj.getClass().getName());
      buf.append(seperator);
      buf.append(System.identityHashCode(obj));
      buf.append('\n');
      writer.write(buf.toString().getBytes());
      writer.flush();
    } catch(Exception e) {throw new RuntimeException(e);}
  }
  public static synchronized void logArrayAccess(Object obj, int index, int ln){
    try {
      StringBuilder buf = new StringBuilder();
      Thread currThread = Thread.currentThread();
      buf.append(currThread.getClass().getName());
      buf.append("@");
      buf.append(System.identityHashCode(currThread));
      buf.append(seperator);
      buf.append(ln);
      buf.append(seperator);
      buf.append(obj.getClass().getName());
      buf.append(seperator);
      buf.append(System.identityHashCode(obj));
      buf.append(seperator);
      buf.append(index);
      buf.append('\n');
      writer.write(buf.toString().getBytes());
      writer.flush();
    } catch(Exception e) {throw new RuntimeException(e);}
  }
  public static synchronized void logInvocationEnd(int ln){
    try {
      StringBuilder buf = new StringBuilder();
      Thread currThread = Thread.currentThread();
      buf.append(currThread.getClass().getName());
      buf.append("@");
      buf.append(System.identityHashCode(currThread));
      buf.append(seperator);
      buf.append("post");
      buf.append(ln);
      buf.append('\n');
      writer.write(buf.toString().getBytes());
      writer.flush();
    } catch(Exception e) {throw new RuntimeException(e);}
  }
  public static synchronized void logBlockEntry(int ln){
    try {
      StringBuilder buf = new StringBuilder();
      Thread currThread = Thread.currentThread();
      buf.append(currThread.getClass().getName());
      buf.append("@");
      buf.append(System.identityHashCode(currThread));
      buf.append(seperator);
      buf.append(ln);
      buf.append('\n');
      writer.write(buf.toString().getBytes());
      writer.flush();
    } catch(Exception e) {throw new RuntimeException(e);}
  }
  public static synchronized void logExEntry(int ln, String cls, String m){
    try {
      StringBuilder buf = new StringBuilder();
      Thread currThread = Thread.currentThread();
      buf.append(currThread.getClass().getName());
      buf.append("@");
      buf.append(System.identityHashCode(currThread));
      buf.append(seperator);
      buf.append("Ex");
      buf.append(ln);
      buf.append(seperator);
      buf.append(cls);
      buf.append(seperator);
      buf.append(m);
      buf.append('\n');
      writer.write(buf.toString().getBytes());
      writer.flush();
    } catch(Exception e) {throw new RuntimeException(e);}
  }
  public static synchronized void logMethodEntry(String cls, String m){
    try {
      StringBuilder buf = new StringBuilder();
      Thread currThread = Thread.currentThread();
      buf.append(currThread.getClass().getName());
      buf.append("@");
      buf.append(System.identityHashCode(currThread));
      buf.append(seperator);
      buf.append(cls);
      buf.append(seperator);
      buf.append(m);
      buf.append('\n');
      writer.write(buf.toString().getBytes());
      writer.flush();
    } catch(Exception e) {throw new RuntimeException(e);}
  }

  public static void close() {
    try {
      writer.close();
    } catch(java.io.IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

}
// vim: tw=100:sw=2
