package rvpredict;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.lang.ref.SoftReference;

import rvpredict.util.ActuallyCloneable;

import com.google.common.base.FinalizableSoftReference;
import com.google.common.base.FinalizableReferenceQueue;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import soot.options.Options;

public final class SuperList<E extends Serializable> extends AbstractList<E> {
  @Override public int hashCode() { return System.identityHashCode(this); }
  @Override public boolean equals(Object o) { return this == o; }
  private static final int blocksize = 10000;
  private final File tmpdir;
  private final Iterator<E> it;
  private ArrayList<E> currentBlock = new ArrayList<E>();
  private int blocksWritten;
  private final List<SoftReference<ArrayList<E>>> blockTable = new ArrayList<SoftReference<ArrayList<E>>>() { private static final long serialVersionUID = 0; {
    add(new SoftReference<ArrayList<E>>(currentBlock));
  }};
  public SuperList(final Iterator<E> inIt) {
    it = inIt;
    File tmpdirTemp;
    try {
      tmpdirTemp = new File(Options.v().output_dir()+File.separator+"SuperList-store-"+System.identityHashCode(this));
      tmpdirTemp.mkdirs();
    } catch (Exception e) { e.printStackTrace(); System.exit(1); tmpdirTemp = null; }
    tmpdir = tmpdirTemp;
  }

  @Override public int size() { return blocksWritten*blocksize+currentBlock.size(); }
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
  private ArrayList<E> getBlock(final int i) {
    final ArrayList<E> l = blockTable.get(i/blocksize).get();
    if (l == null)
      return readBlock(i/blocksize);
    else
      return l;
  }
  @Override public E get(final int i) {
    return getBlock(i).get(i%blocksize);
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
      ArrayList<E> l = (ArrayList<E>)(new ObjectInputStream(new GZIPInputStream(new FileInputStream(new File(tmpdir,Integer.toString(i))))).readObject());
      blockTable.set(i/blocksize,new SoftReference<ArrayList<E>>(l));
      return l;
    } catch (Exception e) { e.printStackTrace(); System.exit(1); return null; }
  }

  @Override public AbstractCLIterator<E,SuperList<E>> iterator() { return listIterator(); }
  @Override public AbstractCLIterator<E,SuperList<E>> listIterator() { return listIterator(0); }
  @Override public AbstractCLIterator<E,SuperList<E>> listIterator(final int i) { return new AbstractCLIterator<E,SuperList<E>>(this,i); }
}
// vim: tw=100:sw=2
