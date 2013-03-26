package rvpredict;

import soot.options.Options;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.lang.ref.SoftReference;

final class ZipList<E extends Serializable> extends AbstractList<E> {
  private static final int blocksize = 10000;
  private int blocksWritten = 0;
  private ZipOutputStream zos;
  private final List<SoftReference<List<E>>> blockTable = new ArrayList<SoftReference<List<E>>>();
  private List<E> currentBlock = null;
  private final File tmpfile = new File(Options.v().output_dir()+File.separator+"ZipList_tmp"+System.identityHashCode(this)+".zip");
  private ZipFile zf;

  public void close() {
    if (zos != null) {
      try {
        zos.close();
        zf = new ZipFile(tmpfile);
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
    }
  }


  @Override public int size() { return blocksWritten*blocksize+((currentBlock==null)?0:currentBlock.size()); }

  @Override public boolean add(final E e) {
    if (currentBlock == null) {
      currentBlock = new ArrayList<E>(blocksize);
      blockTable.add(new SoftReference<List<E>>(currentBlock));
    }
    final boolean ret = currentBlock.add(e);
    if (ret)
      modCount++;
    if (currentBlock.size() == blocksize) {
      try {
        if (zos == null)
          zos = new ZipOutputStream(new FileOutputStream(tmpfile));
        zos.putNextEntry(new ZipEntry(Integer.toString(blocksWritten)));
        final ObjectOutputStream oos = new ObjectOutputStream(zos);
        oos.writeObject(currentBlock);
        oos.flush();
        zos.closeEntry();
      } catch (Exception ex) { ex.printStackTrace(); System.exit(1); }
      currentBlock = null;
      blockTable.set(blockTable.size()-1,new SoftReference<List<E>>(null));
      blocksWritten++;
    }
    return ret;
  }
  @SuppressWarnings("unchecked")
  @Override public E get(final int i) {
    if  (blockTable.get(i/blocksize).get() != null)
      return blockTable.get(i/blocksize).get().get(i%blocksize);
    else {
      List<E> list;
      try {
        final ObjectInputStream oi = new ObjectInputStream(zf.getInputStream(zf.getEntry(Integer.toString(i/blocksize))));
        list = (List<E>) oi.readObject();
      } catch (Exception ex) { ex.printStackTrace(); System.exit(1); list = null; }
      blockTable.set(i/blocksize,new SoftReference<List<E>>(list));
      return list.get(i%blocksize);
    }
  }
}

// vim: tw=100:sw=2
