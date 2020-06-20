package view.box.attribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import controller.AState;
import view.box.attribute.extra.AReturnInfo;
import jsdai.lang.EEntity;


/**
 * The representation of an attribute has get/set-methods which reads/writes in the actual STEP
 * model. 
 * 
 * @author Annica
 *
 */
public abstract class AAttribute{
	
	protected String attributeName;
	protected Object argument;		// Argument needed to solve multiple inheritance problem when doublets
	protected Method getMethod;
	protected Method setMethod;
	protected Method unsetMethod;
	protected EEntity entity;
	protected AReturnInfo returnInfo;
	protected Class<?> declaredClass;
	protected String lastValueString = "";
	protected Object lastValue; 
	
	public AAttribute(String attribute_name, Object argument, EEntity entity){	
		this.attributeName = attribute_name;
		this.argument = argument;
		this.entity = entity;
	}
	
	public abstract Object getAttributeValue();
	
	public abstract void setAttribute(Object value);

	public void unsetAttribute() {
		AState.hasChanged = true;
		Object arglist[] = new Object[1];
		arglist[0] = null;
		try {
			unsetMethod.invoke(entity, arglist);
		} catch (IllegalArgumentException e) { e.printStackTrace();
		} catch (IllegalAccessException e) { e.printStackTrace();
		} catch (InvocationTargetException e) { e.printStackTrace();
		}
	}
	
	public void setName(String name) { this.attributeName = name; }

	public String getName() {return attributeName; }
	
	public void setSetMethod(Method m){ setMethod = m;}
	
	public void setUnsetMethod(Method m){ unsetMethod = m;}
	
	public void setGetMethod(Method m){ getMethod = m; }
	
	public void setDeclaredClass(Class clazz){declaredClass = clazz; }
	
	public void setReturnInfo(AReturnInfo info){ returnInfo = info; }

	public AReturnInfo getReturnInfo() { return returnInfo; }
	
	public String next(){ return null; }

	public String previous(){ return null; }

	public String setCurrent(){ return null; }

	public Method getSetMethod() { return setMethod; }

	public void saveCurrent() {}

	public boolean isAggregate() { return returnInfo.aggregate; }

	public String getToolTip(){	return "";	}
	
	public void setLastValueString(String lv){ lastValueString = lv; }
	
	public String getLastValueString(){ return lastValueString; }
	
	public void updateLastValue(){ lastValue = getAttributeValue(); }
	
	public Object getLastValue(){ return lastValue; }

	public abstract boolean containsValue(Object value);

	public void setLastValue(Object value) { lastValue = value; }

	public String getStatusMessage() {	return ""; 	}

}
