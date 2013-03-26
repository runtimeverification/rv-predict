package rvpredict;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheManager;
import com.whirlycott.cache.CacheException;

final public class WhirlyList<E> extends AbstractList<E> {
  private static final int blocksize = 10000;
  private static Cache cache;
  private int filledBlocks = 0;
  private String prefix = "WhirlyList."+System.identityHashCode(this)+".";
  private String currentName = prefix+0;
  public WhirlyList() {
    try {
      cache = CacheManager.getInstance().getCache();
      cache.store(currentName,new ArrayList<E>());
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }
  @SuppressWarnings("unchecked")
  private List<E> currentBlock() { return (List<E>) cache.retrieve(currentName); }
  @SuppressWarnings("unchecked")
  @Override public E get(final int i) { return ((List<E>)cache.retrieve(prefix+(i/blocksize))).get(i%blocksize); }
  @Override public int size() { return filledBlocks*blocksize + currentBlock().size(); }
  @Override public boolean add(final E e) {
    final boolean ret = currentBlock().add(e);
    if (currentBlock().size() == blocksize){
      filledBlocks++;
      currentName = prefix+filledBlocks;
      cache.store(currentName,new ArrayList<E>());
    }
    return ret;
  }
}
  // vim: tw=100:sw=2
