/**
 * 
 */
package org.cyberborean.rdfbeans.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Base class for {@link WeakCacheMap} and {@link SoftCacheMap}
 * 
 */

abstract class CacheMap<K, V> {	

	protected final Map<K, Reference<V>> cacheMap = new HashMap<K, Reference<V>>();

	protected final ReferenceQueue<V> refQueue = new ReferenceQueue<V>();	
	
	public void put(K key, V value) {
		processQueue();
		Reference<V> entry = newReference(key, value, refQueue);
		cacheMap.put(key, entry);
	}
	
	public V get(K key) {
		processQueue();
		Reference<V> reference = cacheMap.get(key);
		if (reference != null) {
			return reference.get();
		}		
		return null;
	}

	public boolean contains(K key) {
		processQueue();
		return cacheMap.containsKey(key);
	}

	protected abstract void processQueue();

	
	public void remove(K key) {
		cacheMap.remove(key);
	}

	protected abstract Reference<V> newReference(K key, V value, ReferenceQueue<V> queue);
}
