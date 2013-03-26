package rvpredict.logging;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.ArrayList;
import java.lang.Runtime;

//we might decide to use different compression someday
//should be fairly easy to change.  I wish I could make this more generic

//Every time an event is added we check the blockSize.  If we have equaled the block size
//we write the buffer out backwards and compressed.  Each block is written to a separate file;
//to read them back in backwards the files must be read backawards based on extension numebr

//In retrospect a nicer implementation would have been to make it a subclass of FileOutputStream, and just
//pass it to the builder, but then we would have to specifiy the block size in bytes instead of number of events,
//and events could very well be split across files.

public class OldCompressedReversedBlockOutput {
  private int blockSize;
  private String prefix;
  private long fileCounter = 0;
  private ArrayList<String> buffer;
  private DeflaterOutputStream deflateros;

  // 0-9, 0 is none, 1 is speediest, 9 is most compression, 6 is rvpfip default
  private static int compressionLevel = 1;

  public void setBlockSize(int blockSize) {
    assert blockSize >= 1 : "blockSize must be at least 1";
    this.blockSize = blockSize;
  }

  public int getBlockSize() { return blockSize; }

  public OldCompressedReversedBlockOutput(String prefix, int blockSize) {
    this.blockSize = blockSize;
    this.prefix = prefix;
    buffer = new ArrayList<String>(blockSize);
    try {
      String name = prefix + "." + fileCounter++ + ".rvpf";
      deflateros = new DeflaterOutputStream(new FileOutputStream(name), new Deflater(compressionLevel), blockSize * 7);
    } catch(java.io.IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    Runtime.getRuntime().addShutdownHook(new Thread() { @Override public void run() { close(); } });
  }

  public void addEvent(String event) {
    assert buffer != null : "trying to write to closed output";
    assert buffer.size() <= blockSize : "buffer size is somehow bigger than blockSize!";
    buffer.add(event);
    if(buffer.size() == blockSize){
      try {
        for(int i = buffer.size() - 1; i >= 0; --i){
          for(int j = 0 ;j < buffer.get(i).length(); ++j){
            deflateros.write((int)buffer.get(i).charAt(j));  
          } 
        }
        deflateros.close();
        deflateros = new DeflaterOutputStream(new FileOutputStream(prefix + "." + fileCounter++ + ".rvpf"), new Deflater(compressionLevel), blockSize * 7);
      } catch(java.io.IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
      buffer = new ArrayList<String>(blockSize);
    }
  }

  //going to take it on faith that this is not used after being closed
  //you have been warned
  public void close() {
    if(buffer == null) return;
    try {
      for(int i = buffer.size() - 1; i >= 0; --i){
        for(int j = 0 ;j < buffer.get(i).length(); ++j){
          deflateros.write((int)buffer.get(i).charAt(j));  
        } 
      }
      deflateros.close();
      buffer = null;
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(prefix + ".meta.rvpf")); 
      oos.writeLong(fileCounter - 1);
      oos.writeInt(blockSize);
      oos.close();
    } catch(java.io.IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

/*
  public static void main(String[] args){
    OldCompressedReversedBlockOutput orcbow = new OldCompressedReversedBlockOutput("foo",1); 
    orcbow.addEvent("foo\n");
    orcbow.addEvent("bar\n");
    orcbow.addEvent("car\n");
    orcbow.addEvent("far\n");
    orcbow.addEvent("dar\n");
    orcbow.addEvent("sars\n");
    orcbow.addEvent("fars\n");
    orcbow.addEvent("farsee\n");
    orcbow.addEvent("football\n");
    orcbow.addEvent("fooseball\n");
    orcbow.addEvent("fiddle\n");
    orcbow.close();
    try{
      CompressedBlockInputStream crbis = new CompressedBlockInputStream(".","foo","rvpf");
      while(true){
        int c = crbis.readAll();
        if(c == -1) break;
        System.out.print((char) c);
      }
    } catch(Exception e) {e.printStackTrace(); System.exit(1); }
  } */
}
