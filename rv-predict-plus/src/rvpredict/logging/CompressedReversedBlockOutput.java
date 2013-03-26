package rvpredict.logging;
import com.ning.compress.lzf.LZFOutputStream;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.List;
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

public class CompressedReversedBlockOutput {
  private final int blockSize;
  private final String prefix;
  //hopefully we don't actually need a long for the fileCount, but I think it's best to
  //future proof
  private long fileCounter = 0;
  private List<Protos.Event> buffer;
  private LZFOutputStream deflateros;

//  private final static int CUTOFF = 2000000;
  private int count = 0;
  private int maxStackDepth = 0; //so terribly imprecise

  // 0-9, 0 is none, 1 is speediest, 9 is most compression, 6 is rvpfip default
  private final static int compressionLevel = 1;
  private boolean isShuttingDown = false;

  public CompressedReversedBlockOutput(String prefix, int blockSize) {
    this.blockSize = blockSize;
    this.prefix = prefix;
    buffer = new ArrayList<Protos.Event>(blockSize);
    this.deflateros = createLZFOutputStream(fileCounter);
    Runtime.getRuntime().addShutdownHook(
      new Thread() { @Override public void run() { flush(); isShuttingDown = true;}});
  }

  private LZFOutputStream createLZFOutputStream(long count){
    try{
      return new LZFOutputStream(new FileOutputStream(prefix + "." + count + ".rvpf"));
    } catch (java.io.IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null; //java is stupid, this is unreachable yet must be here
  }

  private void writeMetaData(long count){
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(prefix + ".meta.rvpf"));
      oos.writeLong(count);
      oos.writeInt(blockSize);
      int stackDepth = Thread.currentThread().getStackTrace().length;
      maxStackDepth = (stackDepth > maxStackDepth) ? stackDepth : maxStackDepth;
      oos.writeInt(maxStackDepth);
      oos.close();
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }

  public synchronized void addEvent(Protos.Event event) throws IOException {
    assert buffer.size() <= blockSize : "buffer size is somehow bigger than blockSize!";
    //if(count == CUTOFF) return; 
    ++count;
    buffer.add(event);
   // if(count == CUTOFF) { 
   //   flush();
   //   System.out.println(rvpredict.GUIMain.RED + "  RV Predict Beta limit of " + CUTOFF + " events reached.  Quitting program.");
    //  System.exit(0);
    //} 
    if(isShuttingDown || buffer.size() == blockSize) flush();
  }

  public void flush() {
    if(buffer.size() == 0) { return; }
    Protos.Events.Builder builder = Protos.Events.newBuilder();
    for(int i = buffer.size() - 1; i >= 0; --i){
      builder.addEvent(buffer.get(i));
    }
    try {
      writeMetaData(fileCounter++);
      builder.build().writeTo(deflateros);
      deflateros.close();
      buffer = new ArrayList<Protos.Event>(blockSize);
      this.deflateros = createLZFOutputStream(fileCounter);
    } catch(java.io.IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
