package org.cyberborean.rdfbeans.proxy;

import java.lang.reflect.Proxy;
import java.util.Objects;

import org.cyberborean.rdfbeans.RDFBeanManagerContext;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.util.WeakCacheMap;
import org.eclipse.rdf4j.model.Resource;

public class ProxyInstancesPool {

	private final WeakCacheMap<Integer, Object> instances = new WeakCacheMap<Integer, Object>();
	private RDFBeanManagerContext rdfBeanManagerContext;
	
	public ProxyInstancesPool(RDFBeanManagerContext rdfBeanManagerContext) {
		this.rdfBeanManagerContext = rdfBeanManagerContext;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T getInstance(Resource r, RDFBeanInfo rbi, Class<T> iface) {
		int key = createCacheKey(r);
		Object instance = instances.get(key);
		if (instance == null) {		
			instance = Proxy.newProxyInstance(rdfBeanManagerContext.getClassLoader(),	new Class[] { iface }, 
				new RDFBeanDelegator(r, rbi, rdfBeanManagerContext));
			instances.put(key, instance);
		}
		return (T) instance;
	}

	private int createCacheKey(Resource r) {
		return Objects.hash(r);
	}

	public synchronized void purge(Resource r) {
		instances.remove(createCacheKey(r));
	}
}
