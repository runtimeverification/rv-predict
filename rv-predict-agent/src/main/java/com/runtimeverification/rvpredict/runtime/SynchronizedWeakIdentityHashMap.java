package com.runtimeverification.rvpredict.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceIdentityMap;

/**
 * Custom implementation of a synchronized {@link WeakHashMap} used by
 * {@link RVPredictRuntime}.
 * <p>
 * We are unable to use {@link Collections#synchronizedMap(Map)} because we
 * instrument all {@code Collections$SynchronizedX} classes.
 *
 * @author YilongL
 *
 * @param <K>
 *            the type of keys maintained by this map
 * @param <V>
 *            the type of mapped values
 */
public class SynchronizedWeakIdentityHashMap<K, V> implements Map<K, V> {

    private final ReferenceIdentityMap<K, V> m; // Backing Map

    SynchronizedWeakIdentityHashMap() {
        this.m = new ReferenceIdentityMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK);
    }

    @Override
    public synchronized int size() {
        return m.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return m.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return m.containsValue(value);
    }

    @Override
    public synchronized V get(Object key) {
        return m.get(key);
    }

    @Override
    public synchronized V put(K key, V value) {
        return m.put(key, value);
    }

    @Override
    public synchronized V remove(Object key) {
        return m.remove(key);
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> map) {
        m.putAll(map);
    }

    @Override
    public synchronized void clear() {
        m.clear();
    }

    @Override
    public synchronized Set<K> keySet() {
        return m.keySet();
    }

    @Override
    public synchronized Set<Map.Entry<K, V>> entrySet() {
        return m.entrySet();
    }

    @Override
    public synchronized Collection<V> values() {
        return m.values();
    }

    @Override
    public synchronized V computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        return m.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o)
            return true;
        return m.equals(o);
    }

    @Override
    public synchronized int hashCode() {
        return m.hashCode();
    }

    @Override
    public synchronized String toString() {
        return m.toString();
    }

}
