package rvpredict.util;

import java.util.HashMap;
import java.lang.Cloneable;
import java.util.Map;
import java.util.Set;

public class UncloneableDefaultMap<K,V> implements DefaultMap<K,V> {
  private final Map<K,V> map;
  public UncloneableDefaultMap(final Map<? extends K,? extends V> d) {
    map = new HashMap<K,V>(d);
  }
  public UncloneableDefaultMap() {
    map = new HashMap<K,V>();
  }

  @Override public V get(final K key){
    assert map.containsKey(key);
    return map.get(key);
  }

//  @Override public V get(final K key, final V overrideDefaultValue){
//     V v = map.get(key);
//     if(v == null) {
//        v = overrideDefaultValue;
//        map.put(key, v);
//     }
//     return v;
//  }


  // Pass through some operations
  @Override public Set<K> keySet() { return map.keySet(); }
  @Override public void put(final K key, final V value) { map.put(key,value); }
}
// vim: tw=100:sw=2
