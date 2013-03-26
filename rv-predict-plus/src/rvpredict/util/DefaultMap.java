package rvpredict.util;

import java.util.Map;

public interface DefaultMap<K,V> extends CheckedMap<K,V> {
//  public V get(K key, V overrideDefaultValue);
  public void put(K key, V value);
}
// vim: tw=100:sw=2
