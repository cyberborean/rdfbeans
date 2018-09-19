package org.cyberborean.rdfbeans.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockKeeper {

	ConcurrentMap<Object, ReadWriteLock> locks = new ConcurrentHashMap<>();
	
	public synchronized ReadWriteLock getLock(Object o) {
		ReadWriteLock lock = locks.get(o);
		if (lock == null) {
			lock = createLock(o);
		}
		return lock;
	}
	
	private synchronized ReadWriteLock createLock(Object o) {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		locks.put(o, lock);
		return lock;
	}
}
