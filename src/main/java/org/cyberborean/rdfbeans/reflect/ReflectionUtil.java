/**
 * ReflectionUtil.java
 * 
 * RDFBeans Feb 15, 2011 3:22:03 PM alex
 *
 * $Id: ReflectionUtil.java 21 2011-04-02 09:15:34Z alexeya $
 *  
 */
package org.cyberborean.rdfbeans.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * ReflectionUtil.
 *
 * @author alex
 *
 */
public class ReflectionUtil {
	
	public static <A extends Annotation> A getClassAnnotation(Class cls, Class<A> annotationType) {
		A ann =  (A) cls.getAnnotation(annotationType);
		if (ann == null) {
			// No class annotation present, inspect interfaces
			for (Class iface: cls.getInterfaces()) {
				ann = (A) iface.getAnnotation(annotationType);
				if (ann != null) {
					break;
				}
			}
		}
		return ann;
	}
	
	public static <A extends Annotation> List<A> getAllClassAnnotations(Class cls, Class<A> annotationType) {
		List<A> results = new ArrayList<A>();
		findClassAnnotations(cls, annotationType, results);
		return results;		
	}
	
	private static void findClassAnnotations(Class cls, Class annotationType, List list) {
		Annotation ann =  cls.getAnnotation(annotationType);
		if (ann != null) {
			list.add(ann);
		}		
		for (Class iface: cls.getInterfaces()) {
			findClassAnnotations(iface, annotationType, list);
		}
	}
	
	public static Annotation getMethodAnnotation(Method method, Class... annotationTypes) {
		Annotation ann = null;
		for (Class annType: annotationTypes) {
			ann = method.getAnnotation(annType);
			if (ann != null) {
				return ann;
			}
		}
		if (ann == null) {
			// Inspect interface methods
			for (Class iface: method.getDeclaringClass().getInterfaces()) {
				for (Method otherMethod: iface.getMethods()) {
					if (isMatchingMethodSignatures(method, otherMethod)) {
						return getMethodAnnotation(otherMethod, annotationTypes);
					}
				}
			}
		}
		return ann;
	}

	/**
	 * @param method
	 * @param otherMethod
	 * @return
	 */
	public static boolean isMatchingMethodSignatures(Method method, Method otherMethod) {
		return calcMethodSignatureHash(method) == calcMethodSignatureHash(otherMethod);
	}
	
	private static int calcMethodSignatureHash(Method m) {
		return 
			m.getName().hashCode() ^ 
			m.getReturnType().hashCode() ^ 
			Arrays.hashCode(m.getParameterTypes());
	}
	
	public static Class[] getAllInterfaces(Class[] interfaces) {
		List allInterfaces = new ArrayList();
        for (int i = 0; i < interfaces.length; i++) {
            allInterfaces.add(interfaces[i]);
            allInterfaces.addAll(
                Arrays.asList(
                    getAllInterfaces(interfaces[i].getInterfaces())));
        }
        return (Class[]) allInterfaces.toArray(new Class[allInterfaces.size()]);
	}

}
