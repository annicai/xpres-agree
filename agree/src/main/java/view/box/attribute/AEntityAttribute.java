package view.box.attribute;

import java.lang.reflect.InvocationTargetException;

import controller.AState;
import jsdai.lang.EEntity;

public class AEntityAttribute extends AAttribute {

	public AEntityAttribute(String attribute_name, Object argument, EEntity entity) {
		super(attribute_name, argument, entity);
	}

	@Override	
	public Object getAttributeValue() {
	    Object args[] = new Object[1]; 
	    args[0] = argument; 
	    Object value = null;
    
		try {
			value = getMethod.invoke(entity, args);
		} catch (IllegalArgumentException e) {e.printStackTrace();}
		catch (IllegalAccessException e) { e.printStackTrace(); }
		catch (InvocationTargetException e) { e.printStackTrace(); }
		return value;
	}

	@Override
	public void setAttribute(Object value) {
		AState.hasChanged = true;
		if (value == null){
			unsetAttribute();
			return;
		}
		Object arglist[] = new Object[2];
		arglist[0] = argument;
		arglist[1] = value;
		try {
			setMethod.invoke(entity, arglist);
		} catch (IllegalArgumentException e) { e.printStackTrace();
		} catch (IllegalAccessException e) { e.printStackTrace();
		} catch (InvocationTargetException e) { e.printStackTrace();
		}
	}

	@Override
	public boolean containsValue(Object value) {
		return (getAttributeValue() == value);
	}
}
