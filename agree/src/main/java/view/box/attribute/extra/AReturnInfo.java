package view.box.attribute.extra;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import exceptions.AWrongFormatException;
import sdai.structure.AAttributeMappings;
import view.box.attribute.AAAttribute;
import view.box.attribute.AAttribute;
import view.box.attribute.AValueAttribute;
import jsdai.lang.Aggregate;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiIterator;

public class AReturnInfo {
	
	public String returntype;
	public Set <Class> returnclasses = new HashSet<Class>(); 
	public Map <Class, String> selectMap = new HashMap <Class, String>();	
	public ArrayList <String> selectName = new ArrayList <String>(); 
	public Map <String, String> selectNameMap = new HashMap <String, String>();
//	public Map <String, Method> additionalSetMethods = new HashMap <String, Method>();
	public Map <Method, EEntity> arguments = new HashMap <Method, EEntity>();
	public List<Method> additionalGetMethods = new LinkedList<Method>();
	public Map <Method, EEntity> getArguments = new HashMap <Method, EEntity>();
	public ArrayList<String> enumValues = new ArrayList<String>();
	public boolean aggregate;
	public int aggLevel;
	public boolean select;
	public boolean enumeration;
	private LinkedList<Object> order = new LinkedList<Object>();
	
	public AReturnInfo(){
		select = false;
		enumeration = false;
		returntype = "<<<< error >>>>";
	}
	
	public boolean returnsValue(){
		return (returntype.equals("Logical")|| returntype.equals("Boolean") || returntype.equals("Integer") || returntype.equals("Double") || returntype.contains("(ENUM)") ||
				returntype.matches("Aa*(_string|_double|_integer)") || returntype.equals("String"));
	}	

	public boolean isMultipleChoise(){
		return (enumeration || returntype.equals("Boolean") || returntype.equals("Logical") || select);
	}
	
	public ArrayList<String> getChoises(){
		
		if (enumeration){
			ArrayList<String> enums = new ArrayList<String>(enumValues);
			enums.add("$");
			return enums;
		}
		else if (returntype.equals("Boolean"))
			return new ArrayList<String>(){{ add(AAttributeMappings.bool.get("true")); add(AAttributeMappings.bool.get("false")); add("$");}};
		else if (returntype.equals("Logical"))
			return new ArrayList<String>(){{ add(".F."); add(".T."); add(".U."); add("$");}};
		else return new ArrayList<String>();
	}
	
	public Set<Class> getReturnClasses() {
		return returnclasses;
	}
	
	public LinkedList<Object> getOrder(){
		return order;
	}
	public void setOrder(LinkedList<Object> o){
		order = o;
	}
	
	public Object fromString(String string) throws AWrongFormatException{
		Object o = null;
		try {
			Class clazz = returnclasses.iterator().next();
			if (aggregate){
				string = string.replace(" ", "");
				try {
				o = getParts(string, aggLevel);
				} catch (AWrongFormatException wfe){
					throw wfe;
				}
				return o;
			}

			if (clazz == Integer.class || clazz == int.class)
				o = Integer.parseInt(string);
			else if (clazz == Double.class || clazz == double.class)
				o = Double.parseDouble(string);
			else if (returntype.equals("Boolean")){
				if (string.equals(".T."))
					return true;
				else if (string.equals(".F."))
					return false;
			}
			else if (returntype.equals("Logical")){
				
			}
			else if (returntype.toUpperCase().equals("STRING"))
				return string;
			else return clazz.cast(string);
		} catch (Exception e) {
			throw new AWrongFormatException(returntype);
		}
		return o;
	}

	private List<Object> getParts(String string, int i) throws AWrongFormatException {
		if (string.charAt(0) != '(' || string.charAt(string.length()-1) != ')')
			throw new AWrongFormatException("");
		
		List<Object> list = new LinkedList<Object>();
		int ni = i-1;
		int pCount = 0;
		int bIndex = -1;
		if (i > 1){
			if (! string.matches("((.*)[,(.+)]*)"))
				throw new AWrongFormatException(returntype);
			
			for (int j=1; j< string.length()-1;j++){
				if (string.charAt(j) == '('){
					if (pCount == 0)
						bIndex = j;
					pCount +=1;
				}
				else if (string.charAt(j) == ')'){
					pCount--;
					if (pCount < 0)
						throw new AWrongFormatException(returntype);
					if (pCount == 0){
						list.add(getParts(string.substring(bIndex, j+1), ni));
					}
				}
				if (bIndex == -1)
					throw new AWrongFormatException(returntype);
			}
		}
		else {
			String[] parts = string.substring(1, string.length()-1).split(",");
			for (int j=0; j < parts.length; j++){
				if (parts[j].length() > 0){
					try {
						if (returntype.contains("_double"))
							list.add(Double.parseDouble(parts[j]));
						else if (returntype.contains("_integer"))
							list.add(Integer.parseInt(parts[j]));
						else list.add(parts[j]); //A_value also exists????? A_boolean??? A_logical?
					} catch (Exception e){
						throw new AWrongFormatException(returntype);
					}
				}
			}
		}
		return list;
	}
	
	/**
	 * @return the string representation of @param o
	 */
	public String toString(Object o) {
		try {
			if (isMultipleChoise() && o instanceof Integer)
				return getChoises().get((Integer) o - 1);
			else if (returntype.equals("Boolean")){
				if ((Boolean) o)
					return ".T.";
				else return ".F.";
			}
			else if (returntype.equals("String"))
				return "'".concat((String) o).concat("'");
			else return o.toString();
		} catch (Exception e){e.printStackTrace();}
		return "";
	}

	public boolean isBoolean() {
		return (returntype.equals("Boolean"));
	}

	public AAttribute createAttribute(String attribute_name, Class<?> baseType, EEntity entity) {

		AAttribute attribute = null;
		if (aggregate){	// TODO: Add cases for AggAttribute and AEntityAttribute... 
			attribute = new AAAttribute(attribute_name, baseType.cast(null), entity);
		} else {
			attribute = new AValueAttribute(attribute_name, baseType.cast(null), entity);	
		}
		attribute.setReturnInfo(this);
		
		return attribute;
	}
}
