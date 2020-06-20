package view.box.attribute;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import controller.AState;
import view.box.attribute.extra.ASelectMethod;
import jsdai.lang.AEntity;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;

public class ASelectAttribute extends AAttribute implements AMouseEditable{
	
	private int current = 0;
	private String lvalue = "";
	private ArrayList<ASelectMethod> selectMethods = new ArrayList<ASelectMethod>(); 
	private ASelectMethod currentSM;

	public ASelectAttribute(String attribute_name, Object argument, EEntity entity) {
		super(attribute_name, argument, entity);	
	}
	
	
	public void setAttribute(ASelectMethod method, Object value){
		AState.hasChanged = true;
		setSelectMethod(method);
		setAttribute(value);
	}
	
	public void setSelectMethod(ASelectMethod method){
		currentSM = method;
	}

	@Override
	public Object getAttributeValue() {
		if (currentSM == null)
			return null;
		Object retur = null;
		Object[] arglist = new Object[2];
		arglist[0] = null;
		arglist[1] = currentSM.getArgument();
		try {
			retur = currentSM.getGetMethod().invoke(entity, arglist);
		} catch (IllegalArgumentException e) { 
		} catch (IllegalAccessException e) { 
		} catch (InvocationTargetException e) {
		}
		return retur;	
	}

	@Override
	public void setAttribute(Object value) {
		AState.hasChanged = true;
		try {
			if (value instanceof EEntity && returnInfo.aggregate) {			
					Object argl[] = new Object[2];
					argl[0]= null;
					argl[1]= currentSM.getArgument();
					((AEntity) currentSM.getGetMethod().invoke(entity, argl)).addUnordered((EEntity) value);
			}
			else {	
				Object argl[] = new Object[3];
				argl[0]= null;
				argl[1] = value;
				argl[2]= currentSM.getArgument();

				currentSM.getSetMethod().invoke(entity, argl);
			}
		} catch (IllegalArgumentException e) {e.printStackTrace();
		} catch (SdaiException e) { e.printStackTrace();
		} catch (IllegalAccessException e) { e.printStackTrace();
		} catch (InvocationTargetException e) { e.printStackTrace();
		}
	}

	public void addMethod(ASelectMethod selectMethod) {
		selectMethods.add(selectMethod);
	}

	public void setCurrentMethod(ASelectMethod sm) {
		currentSM = sm;
	}

	public void setCurrentMethod(String fc) {
		for (ASelectMethod sm: selectMethods){
			if (fc.contains(sm.getName().toUpperCase())){
				currentSM = sm;
				return;
			}
		}
	}

	@Override
	public String next(){
		if (current < selectMethods.size()-1)
				current+=1;
		currentSM = selectMethods.get(current);
		return currentSM.getName().toUpperCase().concat("(").concat(lvalue).concat(")");
	}
	
	@Override
	public String previous(){
		System.out.println("Current value: " + current);
		if (current > 0){
			current-=1;
			currentSM = selectMethods.get(current);
			return currentSM.getName().toUpperCase().concat("(").concat(lvalue).concat(")");
		}
		else {
			currentSM = null;
			return "$";
		}
	}
	
	@Override
	public String setCurrent() {		//FIXME: Boolean and Logical
		if (lastValue != null){
			setAttribute(lastValue);
			return currentSM.getName().toUpperCase().concat("(").concat(returnInfo.toString(lastValue)).concat(")");
		}
		else return currentSM.getName().toUpperCase().concat("()");
	}
	
	public void saveCurrent(){
		if (currentSM != null){
			Object o = getAttributeValue();
			lvalue = returnInfo.toString(o);	
		}
		else lvalue = "";
	}

	public ArrayList<ASelectMethod> getMethods() {
		return selectMethods;		
	}
	
	public String getToolTip(){
		return "Type: ".concat(returnInfo.returntype).concat( " (SELECT) \nDouble-click and scroll through options to set attribute.");
	}
	
	public String getStatusMessage(){
		return "Set SELECT attribute - scroll through the options and double-click to set attribute";
		
	}


	@Override
	public boolean containsValue(Object value) {
		return (getAttributeValue() == value);
	}


	public boolean isUnset() {
		return currentSM == null;
	}

}
