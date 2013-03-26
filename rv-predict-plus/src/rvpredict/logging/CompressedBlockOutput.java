package rvpredict.logging;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.lang.Runtime;

//we might decide to use different compression someday
//should be fairly easy to change.  I wish I could make this more generic

//Every time an event is added we check the blockSize.  If we have equaled the block size
//we write the buffer out backwards and compressed.  Each block is written to a separate file;
//to read them back in backwards the files must be read backawards based on extension numebr

//In retrospect a nicer implementation would have been to make it a subclass of FileOutputStream, and just
//pass it to the builder, but then we would have to specifiy the block size in bytes instead of number of events,
//and events could very well be split across files.

public class CompressedBlockOutput {
  private final int blockSize;
  private final String prefix;
  private int fileCounter = 0;
  private Protos.Events.Builder builder;
  private DeflaterOutputStream deflateros;

  private boolean isShuttingDown = false;

  // 0-9, 0 is none, 1 is speediest, 9 is most compression, 6 is rvpfip default
  private final static int compressionLevel = 1;

  public CompressedBlockOutput(String prefix, int blockSize) {
    this.blockSize = blockSize;
    this.prefix = prefix;
    builder = Protos.Events.newBuilder();
    Runtime.getRuntime().addShutdownHook(new Thread() { @Override public void run() { flush(); isShuttingDown = true; } });
  }

  private DeflaterOutputStream createDeflatorOutputStream(long count){
    try{
      return new DeflaterOutputStream(new FileOutputStream(prefix + "." + count + ".rvpf"),
                                    new Deflater(compressionLevel), blockSize * 7);   
    } catch (java.io.IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null; //java is stupid, this is unreachable yet must be here
  }

  private void writeMetaData(){
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(prefix + ".meta.rvpf")); 
      oos.writeInt(blockSize);
      oos.close();
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    } 
  }

  public void addEvent(Protos.Event event) throws IOException {
    assert builder.getEventCount() <= blockSize : "buffer size is somehow bigger than blockSize!";
    builder.addEvent(event);
    if(isShuttingDown || builder.getEventCount() == blockSize) flush();
  }
 
  public void flush() {
    if(builder.getEventCount() == 0) { return; }
    try {
      DeflaterOutputStream deflateros = createDeflatorOutputStream(fileCounter++);
      writeMetaData();
      builder.build().writeTo(deflateros);
      deflateros.close();
      builder = Protos.Events.newBuilder();
    } catch(java.io.IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
