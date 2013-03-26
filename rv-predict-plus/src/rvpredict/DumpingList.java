package rvpredict;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.lang.ref.SoftReference;

import com.google.common.base.FinalizableSoftReference;
import com.google.common.base.FinalizableReferenceQueue;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import soot.options.Options;

public final class DumpingList<E extends Serializable> extends AbstractList<E> {
  private static final int blocksize = 10000;
  private final File tmpdir;
  private ArrayList<E> currentBlock = new ArrayList<E>();
  private int blocksWritten;
  private final List<SoftReference<ArrayList<E>>> blockTable = new ArrayList<SoftReference<ArrayList<E>>>() { private static final long serialVersionUID = 0; {
    add(new SoftReference<ArrayList<E>>(currentBlock));
  }};
  public DumpingList() {
    File tmpdirTemp;
    try {
      tmpdirTemp = new File(Options.v().output_dir()+File.separator+"DumpList-store-"+System.identityHashCode(this));
      tmpdirTemp.mkdirs();
    } catch (Exception e) { e.printStackTrace(); System.exit(1); tmpdirTemp = null; }
    tmpdir = tmpdirTemp;
  }

  @Override public int size() { return blocksWritten*blocksize+((currentBlock==null)?0:currentBlock.size()); }
  @Override public boolean add(final E e) {
    final boolean ret = currentBlock.add(e);
    if (currentBlock.size() == blocksize) {
      writeBlock(currentBlock,blocksWritten);
      blockTable.set(blocksWritten,new SoftReference<ArrayList<E>>(null));
      blocksWritten++;
      currentBlock = new ArrayList<E>();
      blockTable.add(new SoftReference<ArrayList<E>>(currentBlock));
    }
    return ret;
  }
  @Override public E get(final int i) {
    final ArrayList<E> l = blockTable.get(i/blocksize).get();
    if (l == null)
      return readBlock(i/blocksize).get(i%blocksize);
    else
      return l.get(i%blocksize);
  }
  private void writeBlock(final ArrayList<E> b, final int i) {
    try {
      ObjectOutputStream o = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(new File(tmpdir,Integer.toString(i)))));
      o.writeObject(b);
      o.close();
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }

  @SuppressWarnings("unchecked")
  private ArrayList<E> readBlock(final int i) {
    try {
      return (ArrayList<E>)(new ObjectInputStream(new GZIPInputStream(new FileInputStream(new File(tmpdir,Integer.toString(i))))).readObject());
    } catch (Exception e) { e.printStackTrace(); System.exit(1); return null; }
  }
}
// vim: tw=100:sw=2
