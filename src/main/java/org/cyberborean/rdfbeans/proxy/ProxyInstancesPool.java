package org.cyberborean.rdfbeans.proxy;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.util.WeakCacheMap;
import org.eclipse.rdf4j.model.Resource;

public class ProxyInstancesPool {

	private final WeakCacheMap<Integer, Object> instances = new WeakCacheMap<Integer, Object>();
	private RDFBeanManager rdfBeanManager;
	
	public ProxyInstancesPool(RDFBeanManager rdfBeanManager) {
		this.rdfBeanManager = rdfBeanManager;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T getInstance(Resource r, RDFBeanInfo rbi, Class<T> iface, Resource... contexts) {
		int key = createCacheKey(r, contexts);
		Object instance = instances.get(key);
		if (instance == null) {		
			instance = Proxy.newProxyInstance(rdfBeanManager.getClassLoader(),	new Class[] { iface }, 
				new RDFBeanDelegator(r, rbi, rdfBeanManager, contexts));
			instances.put(key, instance);
		}
		return (T) instance;
	}

	private int createCacheKey(Resource r, Object[] contexts) {
		return Objects.hash(r, Arrays.hashCode(contexts));
	}

	public synchronized void purge(Resource r, Resource... contexts) {
		instances.remove(createCacheKey(r, contexts));
	}
}
