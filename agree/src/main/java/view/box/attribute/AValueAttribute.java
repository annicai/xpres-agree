package view.box.attribute;

import java.lang.reflect.InvocationTargetException;

import controller.AState;
import jsdai.lang.EEntity;

public class AValueAttribute extends AAttribute implements AMouseEditable{
	
	private int current = 0;
	
	public AValueAttribute(String attribute_name, Object argument, EEntity entity) {
		super(attribute_name, argument, entity);
	}

	@Override
	public Object getAttributeValue() {
	    Object args[] = new Object[1]; args[0] = argument; Object value = null;
		try {
			value = getMethod.invoke(entity, args);
		} catch (IllegalArgumentException e) {}
		catch (IllegalAccessException e) {}
		catch (InvocationTargetException e) {}
		return value;
	}

	@Override
	public void setAttribute(Object value) {
		AState.hasChanged = true;
		if (value == null){
			unsetAttribute();
			return;
		}
		
		if (value instanceof String){
			value = ((String)value).replaceAll("\0", "");		
		}
		
		Object arglist[] = new Object[2]; arglist[0] = argument; arglist[1] = value;
		try {
		
			setMethod.invoke(entity, arglist);
		} catch (IllegalArgumentException e) { 
		} catch (IllegalAccessException e) { 
		} catch (InvocationTargetException e) { 
		}
	}
	
	@Override
	public String next(){
		if (returnInfo.isMultipleChoise()){
			if (current < returnInfo.getChoises().size()-1){
				current+=1;
			}
			return returnInfo.getChoises().get(current);
		}
		return null;
	}
	
	@Override
	public String previous(){
		if (returnInfo.isMultipleChoise()){
			if (current > 0)
				current-=1;
			return returnInfo.getChoises().get(current);
		}
		return null;
	}
	
	@Override
	public String setCurrent() {	
		try {
		if (returnInfo.enumeration)
			setAttribute(current+1);
		else if (returnInfo.returntype.equals("Boolean"))
			setAttribute(returnInfo.fromString(returnInfo.getChoises().get(current)));
		else if (returnInfo.returntype.equals("Logical"))
			setAttribute(current+1);
		return returnInfo.getChoises().get(current);
		} catch (Exception e) { e.printStackTrace(); }
		return "ERROR";
	}
	
	public String getToolTip(){
		if (returnInfo.enumeration)
			return "Type: ".concat(returnInfo.returntype).concat(" \n Double-click and scroll through options to set attribute.");
		return "Type: ".concat(returnInfo.returntype).concat(" \nDouble-click and type a new value to set the attribute.");
	}
	
	public String getStatusMessage(){
		if (returnInfo.enumeration)
			return "Scroll through options and double-click to set the Enumeration attribute";
		else if (returnInfo.isBoolean()){
			return "Scroll through options and double-click to set the Boolean attribute";
		}
		else return "Type a ".concat(returnInfo.returntype).concat(" value to set the attribute");
	}

	@Override
	public boolean containsValue(Object value) {
		return (getAttributeValue() == value);
	}
}
