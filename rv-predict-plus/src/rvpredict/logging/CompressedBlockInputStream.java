package rvpredict.logging;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.Arrays;

//this properly subclasses InputStream, using this to read protocol buffers should be straight-forward
public class CompressedBlockInputStream extends InputStream {
  private InflaterInputStream deflateris;
  //direcotry that files are in
  private String directory;
  //filename prefix, e.g., trace
  private String prefix;
  //filename suffix, e.g., rvpf
  private String suffix;  
  //will be initialized to the highest number index in the directory
  //tells us which current block file we are on
  private long current = 0;
  private long max = 0;

  private String mkName(long num){
    return directory + File.separator + prefix + "." + num + "." + suffix;
  }

  //we assume a format of prefix.number.suffix
  //we also assume that files will be in dir
  public CompressedBlockInputStream(String directory, String filePrefix, String fileSuffix)
       throws IOException {
     prefix = filePrefix;
     suffix = fileSuffix;
     this.directory = directory;
     ObjectInputStream ois = new ObjectInputStream(new FileInputStream(directory + File.separator + prefix + ".meta." + suffix)); 
     max = ois.readLong();
     int size = ois.readInt() * 7;
     ois.close();
     deflateris = new InflaterInputStream(new FileInputStream(mkName(current)), new Inflater(), size);
  }

  //as per the standard, returns -1 at the end of input, however, out end of input is 
  //when the last file is exhausted
  public int readAll() throws IOException {
    do {
      int out = deflateris.read();
      //we found valid input, return it
      if(out != -1) return out;
      //we are already on the last file, so the read call should return -1
      if(current == max) return -1; 
      deflateris.close();
      deflateris = new InflaterInputStream(new FileInputStream(mkName(++current)));
    } while(true);
  }

  //We don't want the protocol buffer for the whole trace in memory at once, so read
  //only reads one block at a time
  public int read() throws IOException {
    return deflateris.read();
  }

  public boolean hasNext() { return current < max; }

  //advance to the next block
  //return false if there is no next block
  public boolean next() throws IOException {
    if(current == max) return false;
    deflateris.close();
    deflateris = new InflaterInputStream(new FileInputStream(mkName(++current)));
    return true;
  }
  
  public void close() throws IOException {
    deflateris.close();
    current = 0;
  }
}
