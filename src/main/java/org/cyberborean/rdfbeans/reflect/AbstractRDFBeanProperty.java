/**
 * AbstractRDFBeanProperty.java
 * 
 * RDFBeans Feb 4, 2011 9:04:39 PM alex
 *
 * $Id: AbstractRDFBeanProperty.java 31 2011-09-30 05:18:26Z alexeya $
 *  
 */
package org.cyberborean.rdfbeans.reflect;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cyberborean.rdfbeans.exceptions.RDFBeanException;

/**
 * AbstractRDFBeanProperty.
 * 
 * @author alex
 * 
 */
public abstract class AbstractRDFBeanProperty {

	private PropertyDescriptor propertyDescriptor;

	public AbstractRDFBeanProperty(PropertyDescriptor propertyDescriptor) {
		this.propertyDescriptor = propertyDescriptor;
	}

	public Object getValue(Object rdfBean) throws RDFBeanException {
		Method getter = propertyDescriptor.getReadMethod();
		if (getter != null) {
			try {
				Object value = getter.invoke(rdfBean);
				if ((value != null) && value.getClass().isArray()) {
					// wrap array into the List
					ArrayList list = new ArrayList(Array.getLength(value));
					for (int i = 0; i < Array.getLength(value); i++) {
						list.add(Array.get(value, i));
					}
					value = list;
				}
				return value;
			} catch (IllegalArgumentException ex) {
				throw new RDFBeanException("No method "
						+ rdfBean.getClass().getName() + "." + getter.getName()
						+ "() exists", ex);
			} catch (IllegalAccessException ex) {
				throw new RDFBeanException("Method "
						+ rdfBean.getClass().getName() + "." + getter.getName()
						+ " is inaccessible", ex);
			} catch (InvocationTargetException ex) {
				throw new RDFBeanException(ex);
			}
		}
		throw new RDFBeanException(
				"No public getter method is defined for property '"
						+ propertyDescriptor.getName() + "' in class "
						+ rdfBean.getClass().getName());
	}

	public void setValue(Object rdfBean, Object v) throws RDFBeanException {
		Method setter = propertyDescriptor.getWriteMethod();
		if (setter != null) {
			try {
				if (propertyDescriptor.getPropertyType().isArray()
						&& (v instanceof List)) {
					// unwrap an array from the list
					List list = (List) v;
					Object array;
					if (isIndexedProperty()) {
						array = Array.newInstance(
							((IndexedPropertyDescriptor) propertyDescriptor)
									.getIndexedPropertyType(), list.size());
					}
					else {
						array = Array.newInstance(propertyDescriptor.getPropertyType().getComponentType(), list.size());
					}
					for (int i = 0; i < list.size(); i++) {
						Array.set(array, i, list.get(i));
					}
					v = array;
				}
				setter.invoke(rdfBean, v);
			} catch (IllegalArgumentException ex) {
				throw new RDFBeanException("No method "
						+ rdfBean.getClass().getName() + "." + setter.getName()
						+ "(" + v.getClass().getName() + ") exists", ex);
			} catch (IllegalAccessException ex) {
				throw new RDFBeanException("Method "
						+ rdfBean.getClass().getName() + "." + setter.getName()
						+ "(" + v.getClass().getName() + ") is inaccessible",
						ex);
			} catch (InvocationTargetException ex) {
				throw new RDFBeanException(ex);
			}
		} else {
			throw new RDFBeanException(
					"No public setter method is defined for property '"
							+ propertyDescriptor.getName() + "' in class "
							+ rdfBean.getClass().getName());
		}
	}

	public PropertyDescriptor getPropertyDescriptor() {
		return propertyDescriptor;
	}
	
	public Class<?> getPropertyType() {
		return propertyDescriptor.getPropertyType();
	}
	
	public Class<?> getPropertyComponentType() {
		if (propertyDescriptor.getPropertyType().isArray()) {
			return propertyDescriptor.getPropertyType().getComponentType();
		}
		else if (Collection.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
			Method getter = propertyDescriptor.getReadMethod();
			if (getter != null) {
				Type returnType = getter.getGenericReturnType();
				if (returnType instanceof ParameterizedType){
				    ParameterizedType type = (ParameterizedType) returnType;
				    Type[] typeArguments = type.getActualTypeArguments();
				    if (typeArguments.length > 0) {
				    	return (Class<?>) typeArguments[0];
				    }
				}
			}
			else {
				// this normally should never happen, as PropertyDescriptor is obtained
				throw new RuntimeException("No public getter method is defined for property '"
						+ propertyDescriptor.getName());
			}
			return Object.class;
		}
		return null;
	}
	
	public boolean isIndexedProperty() {
		return propertyDescriptor instanceof IndexedPropertyDescriptor;
	}

}
