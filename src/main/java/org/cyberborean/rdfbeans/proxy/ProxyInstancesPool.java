/**
 * 
 */
package org.cyberborean.rdfbeans.proxy;

import java.lang.reflect.Proxy;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.util.WeakCacheMap;
import org.openrdf.model.Resource;

/**
 * @author alex
 *
 */
public class ProxyInstancesPool {

	private final WeakCacheMap<String, Object> instances = new WeakCacheMap<String, Object>();
	private RDFBeanManager rdfBeanManager;
	
	public ProxyInstancesPool(RDFBeanManager rdfBeanManager) {
		this.rdfBeanManager = rdfBeanManager;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T getInstance(Resource r, RDFBeanInfo rbi, Class<T> iface) {
		String key = r.toString();
		Object instance = instances.get(key);
		if (instance == null) {		
			instance = Proxy.newProxyInstance(rdfBeanManager.getClassLoader(),	new Class[] { iface }, 
				new RDFBeanDelegator(r, rbi, rdfBeanManager));
			instances.put(key, instance);
		}
		return (T) instance;
	}

	public synchronized void purge(Resource r) {
		instances.remove(r.toString());
	}
}
