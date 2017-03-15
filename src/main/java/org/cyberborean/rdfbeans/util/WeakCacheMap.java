
package org.cyberborean.rdfbeans.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakCacheMap<K, V> extends CacheMap<K, V> {

	@SuppressWarnings("unchecked")
	@Override
	protected synchronized void processQueue() {
		WeakEntry en = null;
		while ((en = (WeakEntry) refQueue.poll()) != null) {
			K key = en.getKey();
			cacheMap.remove(key);
		}
	}

	@Override
	protected Reference<V> newReference(K key, V value, ReferenceQueue<V> vReferenceQueue) {
		return new WeakEntry(key, value, vReferenceQueue);
	}

	private class WeakEntry extends WeakReference<V> {
		private K key;

		WeakEntry(K key, V referent, ReferenceQueue<? super V> q) {
			super(referent, q);
			this.key = key;
		}

		K getKey() {
			return key;
		}
	}

}
