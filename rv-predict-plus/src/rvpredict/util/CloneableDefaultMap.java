package rvpredict.util;

import java.util.HashMap;
import java.lang.Cloneable;
import java.util.Map;
import java.util.Set;

public class CloneableDefaultMap<K, V extends ActuallyCloneable<V>> implements DefaultMap<K,V> {
  private final V defaultValue;
  private final Map<K,V> map;

  public CloneableDefaultMap() {
    map = null;
    defaultValue = null;
  }

  public CloneableDefaultMap(final CloneableDefaultMap<? extends K,? extends V> d) {
    this.map = new HashMap<K,V>(d.map);
    this.defaultValue = d.defaultValue;
  }

  public CloneableDefaultMap(final V defaultValue){
    this.map = new HashMap<K,V>();
    this.defaultValue = defaultValue;
  }

  @Override public V get(final K key){
    V ret = map.get(key);
    if (ret == null) {
      ret = defaultValue.clone();
      map.put(key, ret);
    }
    return ret;
  }

//  @Override public V get(final K key, final V overrideDefaultValue){
//    V v = map.get(key);
//    if(v == null) {
//      v = overrideDefaultValue;
//      map.put(key, v);
//    }
//    return v;
//  }

  // Pass through
  @Override public Set<K> keySet() { return map.keySet(); }
  @Override public void put(K key, V value) { map.put(key,value); }
}
// vim: tw=100:sw=2
