package rvpredict.util;

import java.util.Set;

public interface CheckedMap<K,V> {
  public V get(K key);
  public Set<K> keySet();
}
// vim: tw=100:sw=2
