package view.box.attribute.extra;

import java.lang.reflect.Method;

public class ASelectMethod {
	
	private Method getMethod;
	private Method setMethod;
	private Object argument;
	private String name;
	
	public ASelectMethod(Method method, Object argument, String fieldComponent) {
		this.getMethod = method;
		this.argument = argument;
		this.name = fieldComponent;
	}

	public void setSetMethod(Method met) {
		this.setMethod = met;
		
	}

	public Method getGetMethod() {
		return getMethod;
	}
	
	public Method getSetMethod() {
		return setMethod;
	}

	public Object getArgument() {
		return argument;
	}
	
	public String getName(){
		return name;
	}


}
