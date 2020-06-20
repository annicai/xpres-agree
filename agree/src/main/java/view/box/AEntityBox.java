package view.box;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jsdai.lang.AEntity;
import jsdai.lang.A_string;
import jsdai.lang.Aggregate;
import jsdai.lang.CAggregate;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Font;

import sdai.ASdaiHandler;
import sdai.structure.AAttributeMappings;
import view.bendpoint.ABendPoint;
import view.bendpoint.AConnectionShaper;
import view.box.attribute.AAttribute;
import view.box.attribute.ASelectAttribute;
import view.box.attribute.extra.AReturnInfo;
import view.box.attribute.extra.ASelectMethod;
import view.box.figure.AAttributeFigure;
import view.box.figure.AEntityFigure;
import view.box.figure.AValueFigure;
import controller.AControllerImpl;
import controller.AState;

/**
 * Represents a graphical instance-box. 
 *
 */
public class AEntityBox extends ABox {

	private LinkedList <AAttributeFigure> attributes = new LinkedList <AAttributeFigure>();
	private Map<String, AAttributeFigure> attributeMap = new HashMap<String, AAttributeFigure>();
	
	// Say, SELECT of two different types (string OR entities) that both are shown in the entity-box
	private Map<String, LinkedList<AAttributeFigure>> twins = new HashMap<String, LinkedList<AAttributeFigure>>();
	
	private EEntity entity;
	private Point location = new Point(0,0);
	private String name; 
	
	private LinkedList<Method> usedMethods = new LinkedList<Method>();
	private Map <String, AAttribute> usedNames = new HashMap<String, AAttribute>();
	private AControllerImpl listener;
	
	
	public AEntityBox(String text, Font font, Point point, EEntity entity, AControllerImpl controller) {
		super(text, font);
		this.entity = entity;
		listener = controller;
		setBounds(new Rectangle(200, 200, 250, 100));
		addMouseListener(controller);
		addMouseMotionListener(controller);
		if (point != null)
			location = point;
		controller.getModel().addEntityBox(this, entity);
		addAttributes(controller);
		name = entity.getClass().getSimpleName().substring(1);
	}
	
	@Override
	public void setBoxBorder() {
		setBorder(new ABoxBorder());
	}
	
	public void setButtonVisibility(boolean visible){
		for (AAttributeFigure figure: attributes){
			if (figure instanceof AEntityFigure){
				((AEntityFigure) figure).setBorderVisibility(visible);
			}
		}
	}
	

	public AAttributeFigure getAttributeFigure(String attributeName) {
		return attributeMap.get(attributeName);
	}

	public EEntity getEntityRepresentation(){
		return entity;
	}
	
	public Point getNextLocation(){
		return location;
	}
	
	/**
	 * Hide this box. All outgoing connections (from AEntityFigure's) need to be hidden.
	 */
	public void hide() {
		for (AAttributeFigure figure: attributes){
			if (figure instanceof AEntityFigure){
				try{
					((AEntityFigure) figure).hide();
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public LinkedList <AAttributeFigure> getAttributeFigures() {
		return attributes;
	}
	

	public ABendPoint addBendpoint(PolylineConnection outConnection) {
		PolylineConnection  c = new PolylineConnection();
		Figure source = (Figure) outConnection.getSourceAnchor().getOwner();
		ChopboxAnchor sourceAnchor = new ChopboxAnchor(source);	
		if (source instanceof AConnectionShaper){
			((AConnectionShaper) source).replaceOutConnection(c);
		} 
		c.setConnectionRouter(AState.getConnectionRouter());
		c.setSourceAnchor(sourceAnchor);
		ABendPoint bp = new AConnectionShaper(c, listener);
		bp.addOutConnection(outConnection);
		ChopboxAnchor bpAnchor = new ChopboxAnchor(bp);
		c.setTargetAnchor(bpAnchor);
		outConnection.setSourceAnchor(bpAnchor);
		return bp;
	}

	
	/**
	 * Retrieves all figures where @param entity is a valid attribute
	 * 
	 * @param entity
	 * @return list of entity-figures where @param entity is a valid attribute
	 */
	public LinkedList<AEntityFigure> getValidAttributes(EEntity entity) {
		LinkedList<AEntityFigure> figures = new LinkedList<AEntityFigure>();
		for (AAttributeFigure f: attributes){
			if (f instanceof AEntityFigure){
				if (((AEntityFigure)f).validAttribute(entity))
					figures.add((AEntityFigure) f);
			}
		}
		return figures;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void basicName() {
		label.setText(name);
	}
	
	public void persistentName() {
		String pl = ASdaiHandler.getPersistantLabel(entity);
		label.setText(pl.concat(" : ").concat(name));	
	}
	
	
	/**
	 * Keep track of attributes occurring twice, eg Selects that can be both strings and entities
	 * 
	 * @param attribute_name
	 * @param attributeFigure
	 */
	private void addToAttributeMap(String attribute_name, AAttributeFigure attributeFigure) {
		if (attributeMap.containsKey(attribute_name)){
			LinkedList<AAttributeFigure> list;
			if (!twins.containsKey(attribute_name)){
				list = new LinkedList<AAttributeFigure>();
				list.add(attributeMap.get(attribute_name));
				twins.put(attribute_name, list);
			}
			else {
				list = twins.get(attribute_name);
			}
			list.add(attributeFigure);
			
		}
		attributeMap.put(attribute_name, attributeFigure);
		
	}
	

	/**
	 * Adds values from aggregates of aggregates.
	 * 
	 * 
	 */
	private void addAggregateButtons(Object ret, AAttributeFigure attributeFigure) {
		try {
			Aggregate entities = ((Aggregate) ret);
			SdaiIterator entityIterator = entities.createIterator();
			while (entityIterator.next()){
				Object current =  entities.getCurrentMemberObject(entityIterator);
				if (current instanceof EEntity){
					String value = ((EEntity) current).getPersistentLabel();
					attributeFigure.addAttributeValue(value, current);
				}
				else if (current instanceof CAggregate){
					CAggregate innerAgg = ((CAggregate) current);
					addAggregateButtons(innerAgg, attributeFigure);				
				}
			}
		} catch (SdaiException e) {	e.printStackTrace(); }
		
	}
	
	/**
	 * If attribute has two attribute figures, update so that only the set one is shown or
	 * all of them if none is set.
	 * 
	 * @param attribute
	 * @param string
	 */
	public void notifyAttributeChange(AAttribute attribute, String string) {
		if (twins.containsKey(attribute.getName())){
			for (AAttributeFigure figure: twins.get(attribute.getName())){
				if (figure.getAttribute() != attribute){
					if (string.equals("$")){
						if (figure.getParent() != this)
							add(figure);
					}
					else {
						if (figure.getParent() == this)
							remove(figure);
					}
				}
			}
		}
	}
	

	/**
	 * Add the attribute figures for this entity to the entity box.
	 * 
	 * @param controller
	 */
	public void addAttributes(AControllerImpl controller) {
	    Method methods[] = entity.getClass().getMethods();	
	    
	    for (int i = methods.length-1; i >=0; i--){
	    	Method method = methods[i];
	    	String methodName = methods[i].getName();
	    	
	    	if (ASdaiHandler.isSelect(method)){
	    		String attribute_name = methodName.startsWith("get") && methodName.length() > 3 ? methodName.substring(3) : null;
	    		if (attribute_name != null) {
	    			
		    		List<Class<?>> classes = ASdaiHandler.getClasses(entity);
		    		LinkedList <Field> declaredFields = new LinkedList<Field>();
		    		for (Class<?> c: classes) {
		    			declaredFields.addAll(ASdaiHandler.getDeclaredFields(c, attribute_name));
		    		}
		    		
		 		   AAttributeFigure attributeFigure = null;
				   if (declaredFields.size() != 0){ //Select
				    	boolean doublett = false;
						for (AAttributeFigure a: attributes){
							if (a.getAttribute().getName().equals(attribute_name) && (a.getAttribute() instanceof ASelectAttribute)){
								attributeFigure = a;
								doublett = true;
							}
						}	
						if (!doublett){
    				       	ASelectAttribute attribute = new ASelectAttribute(attribute_name, null, entity);	
    				       	
    						AReturnInfo info = ASdaiHandler.getReturnType(methods[i], attribute.getName(), entity);
    						info.select = true;
    						attribute.setReturnInfo(info);
			
    						attributeFigure = new AValueFigure(attribute_name, this, controller);
    						attributeFigure.setAttribute(attribute);
    						attributes.add(attributeFigure);
    						addToAttributeMap(attribute_name, attributeFigure);
    						
    						attributeFigure.addAttributeValue(" $ ", null);   	
    						attribute.setDeclaredClass(methods[i].getDeclaringClass());

						}
				       	addSelectValues(declaredFields, attribute_name, methods, i, attributeFigure, entity, controller);
				   }
	    		}
	    	}
	    	else if (methods[i].getParameterTypes().length == 1 && methods[i].getName().startsWith("get") && ASdaiHandler.isAttributeMethod(methods[i])){
														
	    		createAttributeFigureAndFillWithValues(methods, i, new Object[]{null}, controller);			
	    	}
	    	
	    }
		// Remove duplicate attribute figures
		for (String tatt : twins.keySet()){
			LinkedList<AAttributeFigure> ta = twins.get(tatt);
			for (AAttributeFigure taf: ta){
				if (taf.getAttribute().getAttributeValue() != null){
					notifyAttributeChange(taf.getAttribute(), "set");
				}
			}
				
		}
	}
	
	
	private void addSelectValues(LinkedList<Field> declaredFields, String attribute_name, Method[] methods, int i, AAttributeFigure attributeFigure, EEntity entity, AControllerImpl controller){
		ASelectAttribute attribute = (ASelectAttribute) attributeFigure.getAttribute();
		for (Field f : declaredFields){
			
				String[] fieldComponents = f.getName().split(attribute_name);
				
				if (fieldComponents.length > 1){
					try {
						if (methods[i].toString().contains(fieldComponents[1])){
							String[] split = methods[i].toString().split("\\.");
							String schema = null;
							for (int index = 1; index < split.length; index++){
								if (split[index].contains(fieldComponents[1])){
									schema = split[index-1];
								}
							}		
							Object object = Class.forName("jsdai.".concat(schema).concat(".E").concat(fieldComponents[1]));
							object = null;
	   						
		    				ASelectMethod selectMethod = new ASelectMethod(methods[i], (EEntity) object, fieldComponents[1]);
		    				attribute.addMethod(selectMethod);
		    				
		    				Method cMethods [] = entity.getClass().getMethods();
		    				for (Method met: cMethods){
		    					
		    					if (met.getName().contains("unset".concat(attribute_name)))
		    						attribute.setUnsetMethod(met);
		    					else if (met.getName().contains("set".concat(attribute_name)) && met.toString().contains(fieldComponents[1]))
		    						selectMethod.setSetMethod(met);
		    					else if (met.getName().contains("create".concat(attribute_name)) && met.toString().contains(fieldComponents[1]))
		    						selectMethod.setSetMethod(met);
		    				}

							Object[] select_arglist = new Object[2];
	   						select_arglist[0] = null;
	   						select_arglist[1] = object;
	   						try {
								Object ret = methods[i].invoke(entity, select_arglist);
								// If the select type refers to a value to be printed, i e integer, string etc 
								if (! (ret instanceof AEntity) && ! (ret instanceof EEntity) && (ret != null)){
									String attribute_value = fieldComponents[1].toUpperCase().concat("(").concat(ret.toString()).concat(")");
									attributeFigure.addAttributeValue(attribute_value, ret.toString());
								}
								else {	// We need to retrieve set Entity attributes as with ordinary entity attributes. An extra field for the buttons is added.
									String attribute_value = fieldComponents[1].toUpperCase();
									attributeFigure.addAttributeValue(attribute_value, null);	
									attribute.setCurrentMethod(fieldComponents[1]);
							
									createAttributeFigureAndFillWithValues(methods, i, select_arglist, controller);						
									return;
								}

							} 
	   						catch (IllegalArgumentException|IllegalAccessException e) {} 
							catch (InvocationTargetException e) { 		
								// The return type is a AEntity or EEntity subclass 

								if (!AAttributeMappings.labelClasses.contains(methods[i].getReturnType())){
									attribute.setGetMethod(methods[i]);
									AReturnInfo info = ASdaiHandler.getReturnType(methods[i],attribute.getName(),entity);
									attribute.setReturnInfo(info);
									attribute.setDeclaredClass(methods[i].getDeclaringClass());
									
								}
							}	
   							return;
						}

				} catch (ClassNotFoundException|ClassCastException e) { e.printStackTrace(); }
			}
		}
		attributeFigure.setAttribute(attribute);
		return;
	}
	
	
	private void createAttributeFigureAndFillWithValues(Method[] methods, int i, Object[] arglist, AControllerImpl controller) {
		AAttributeFigure figure = createAttributeFigure(methods, i, controller);
		fillWithSetValues(methods[i], arglist, figure);
	}
	
	private AAttributeFigure createAttributeFigure(Method[] methods, int i, AControllerImpl controller) {
		String attribute_name = methods[i].getName().substring(3);	
		Method origMethod = methods[i];
		Class<?> baseType = origMethod.getParameterTypes()[0];
		
		AReturnInfo info = ASdaiHandler.getReturnType(origMethod , attribute_name , entity);

		AAttribute attribute = info.createAttribute(attribute_name, baseType, entity);
		attribute.setGetMethod(methods[i]);

		for (int j=0; j<= methods.length-1; j++){
			if (methods[j].getParameterTypes().length == 0)
				continue;
			
			String methodName = methods[j].getName();
			Class<?> currentType = methods[j].getParameterTypes()[0];
			
			if (methodName.equals(("unset").concat(attribute_name))){
				attribute.setUnsetMethod(methods[j]);
			}
			
			if (attribute.getReturnInfo().aggregate && methodName.equals("create".concat(attribute_name))) {
				attribute.setSetMethod(methods[j]);
			}	
			else {
				if (currentType == baseType && methodName.equals(("set").concat(attribute_name)) &&
						methods[j].getParameterTypes().length == 2 && !usedMethods.contains(methods[j])){

						attribute.setSetMethod(methods[j]);
						usedMethods.add(methods[j]);
						if (! usedNames.containsKey(("set").concat(attribute_name))){
							usedNames.put(("set").concat(attribute_name), attribute);
						}
						else {
							attribute.setName(attribute_name.concat(" (").concat(currentType.getSimpleName().substring(1)).concat(")"));
							AAttribute oldAttribute = usedNames.get(("set").concat(attribute_name));
							if (!oldAttribute.getName().contains("(")){
								oldAttribute.setName(oldAttribute.getName().concat( " (").concat(oldAttribute.getSetMethod().getParameterTypes()[0].getSimpleName().substring(1)).concat(")"));
							}
						}

				}
				else if (methods[j].getName().equals(("set").concat(attribute_name)) &&  methods[j].getParameterTypes().length == 3){ // Select
					Object object;
					try {
						object = Class.forName(methods[j].getParameterTypes()[2].getName());
					} catch (ClassNotFoundException e) {e.printStackTrace();}
						object = null;
						attribute.getReturnInfo().arguments.put(methods[j], (EEntity) object);
						if (attribute instanceof ASelectAttribute){
							ASelectMethod sm = new ASelectMethod(methods[j], object, "..");
							((ASelectAttribute)attribute).addMethod(sm);
						}
				}
			}
		}
		
		AAttributeFigure attributeFigure;
		if (info.returnsValue()){
			attributeFigure = new AValueFigure(attribute_name, this, controller);
		}
		else {	// Add button
			if (info.aggregate)
				attributeFigure = new AEntityFigure(attribute_name, this, true, controller);	
			else attributeFigure = new AEntityFigure(attribute_name, this, false, controller);
		}	
		if (attribute.getName().contains("(")){
			attributeFigure.setToolTip(new Label(attribute.getName()));
		}
		// If Select-attribute with AEntitties, representation should be the other attribute figure
		// (with entities and not with SELCT name)
		// attribute.representation=attributeFigure;

		attribute.setDeclaredClass(methods[i].getDeclaringClass());		
		attributeFigure.setAttribute(attribute);
		attributes.add(attributeFigure);	
		addToAttributeMap(attribute_name, attributeFigure);
		return attributeFigure;
	}

	
	private void fillWithSetValues(Method method, Object[] arglist, AAttributeFigure attributeFigure) {
		AReturnInfo info = attributeFigure.getAttribute().getReturnInfo();
		
		try {
			Object ret = method.invoke(entity, arglist);
			if (ret != null){
				if (ret instanceof EEntity){
					String value = ((EEntity) ret).getPersistentLabel();
					attributeFigure.addAttributeValue(value, ret);
				}
				else if (ret instanceof AEntity && !(ret instanceof A_string) ){
					AEntity entities = (AEntity) ret;
					try {
						SdaiIterator entityIterator = entities.createIterator();										
						while (entityIterator.next()){
							EEntity current = entities.getCurrentMemberEntity(entityIterator);
							String value = current.getPersistentLabel();
							attributeFigure.addAttributeValue(value, current);
						}
					if (((AEntityFigure) attributeFigure).entitySize()==0){
						remove((AEntityFigure) attributeFigure);
					}
					} catch (SdaiException e) { e.printStackTrace(); }
				}
				else if (ret instanceof CAggregate && attributeFigure instanceof AEntityFigure){
					// An aggregate of entities, that is select type, can not be cast as above!
					if (info.aggLevel > 1){			// FIXME: Should be done recursively, can be AAgg.. 
						addAggregateButtons(ret, attributeFigure);				
					}
					else {		
						CAggregate entities = (CAggregate) ret;
						SdaiIterator entityIterator = ((CAggregate)ret).createIterator();
						while (entityIterator.next()){
							EEntity current = entities.getCurrentMemberEntity(entityIterator);
							String value = current.getPersistentLabel();
							attributeFigure.addAttributeValue(value, current);
						}
					}

				}
				else {
					String attribute_value = "";
					if (ret instanceof String)
						attribute_value = "'".concat(ret.toString()).concat("'");
					else if (ret instanceof A_string){
						SdaiIterator iterat = ((CAggregate) ret).createIterator();
						String separator = "'";
						while (iterat.next()){
							attribute_value = attribute_value.concat(separator).concat((String) ((CAggregate) ret).getCurrentMemberObject(iterat)).concat("'");
							separator = ", '";
						}
					}
					else if (ret.getClass() == Boolean.class)
						attribute_value = AAttributeMappings.bool.get(ret.toString());
					else if (info.returntype.equals("Logical"))
						attribute_value = AAttributeMappings.logical.get(ret.toString());
					else if (info.enumeration)
						attribute_value = info.enumValues.get(((Integer) ret)-1);
					else  //----- Possible Enum, Integer, Double etc, should not have a mouse-listener
   					attribute_value = ret.toString();
					attributeFigure.addAttributeValue(attribute_value, null);
				}	
			}

		} catch (InvocationTargetException e) {	 //  Value not set. Add value "$" 
			String attribute_value = "$";
			attributeFigure.addAttributeValue(attribute_value, null);
		}  catch (Exception e){ e.printStackTrace();}
		
		if (attributeFigure.getAttribute().getSetMethod() == null ){	//Derived
			if (getChildren().contains(attributeFigure)){	
				remove(attributeFigure);
			}
			attributes.remove(attributeFigure);
		} 
	}

}
