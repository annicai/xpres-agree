package view.box.figure;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import model.AModelImpl;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ButtonBorder;
import org.eclipse.draw2d.PolylineConnection;

import sdai.ASdaiHandler;
import view.box.AEntityBox;
import view.box.attribute.AAAttribute;
import view.box.attribute.extra.AButton;
import controller.AControllerImpl;
import controller.AState;
import exceptions.ABoxNotFoundException;

/**
 * Attribute containing entities. 
 *
 */
public class AEntityFigure extends AAttributeFigure{

	private Map<AButton, PolylineConnection> connectedEntities = new HashMap<AButton, PolylineConnection>();
	private LinkedList <AButton> entities = new LinkedList <AButton>();
	private boolean visible = true;
	private AButton parentButton;
	
	public AEntityFigure(String attribute_name, AEntityBox box, boolean aggregate, AControllerImpl controller){
		super(attribute_name, box, controller);
		AButton button;
		if (aggregate)//(info.aggregate && !info.select){
			button = new AButton("     " + attribute_name + " [A] : ", this, null);
		else {
			button = new AButton("     " + attribute_name + " : ", this , null);
		}
		if (!AState.visibleButtonBorder)
			button.setBorder(null);
		button.addActionListener(listener);
		parentButton = button;
		add(button);
		repaint();
	}
	
	public boolean validAttribute(EEntity entity) {
		for (Class clazz: attribute.getReturnInfo().getReturnClasses())
			if (ASdaiHandler.findAllInstances(clazz).contains(entity))
				return true;
		return false;
	}

	public int entitySize() {
		return entities.size();
	}

	/**
	 * Show the entity attribute. 
	 * Remove the button and create a connection to the attribute instance.
	 * 
	 * @param button
	 * @param connection
	 */
	public void buttonToConnection(AButton button, PolylineConnection connection) {
		connectedEntities.put(button, connection);
		if (button.getParent() == this)
			remove(button);
		if (((entities.size()-connectedEntities.size()) == 0) && (entities.size()>0)){	
			if (visible){
				try {
					getParent().remove(this);
					visible=false;
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Show a button representing the attribute instance when hidden.
	 * 
	 * @param button
	 */
	public void addButton(AButton button) {
		connectedEntities.remove(button);
		add(button);
		if (!visible){
			parentBox.add(this);
			visible = true;
		}
	}
	
	/**
	 * 
	 * 
	 * @param button
	 */
	public void removeButton(AButton button) {
		if (connectedEntities.containsKey(button)){
			connectedEntities.remove(button);
			button.toButton();
			remove(button);
			if (entities.size() == 0 && !visible){
				parentBox.add(this);
				visible = true;
			}
		}
		else remove(button);	

		
		AModelImpl.removeButtonMapping(button);
		entities.remove(button);
		if (visible && (entities.size() - connectedEntities.size() == 0) && (entities.size() > 0)){
			getParent().remove(this);
			visible=false;
		}
		for (AButton b: entities){
			b.updateAttributeLabel();
		}
	}

	public void hide() {
		for (AButton button: entities){
			if (button.isConnection()){
				button.removeConnection();
			}
			AModelImpl.removeButtonMapping(button);
		}
		
	}
	
	@Override
	public boolean setAttributeValue(Object entity) {
		String set = "";
		if ((! attribute.containsValue(entity)) && entity != null){
			attribute.setAttribute(entity);	
			try {
				addAttributeValue(((EEntity) entity).getPersistentLabel(), entity);
				if (entities.size() == 0)
					set = UNSET;
				parentBox.notifyAttributeChange(attribute, set);
				return true;
			} catch (SdaiException e) { e.printStackTrace(); }
		}
		if (entities.size() == 0)
			set = UNSET;
		parentBox.notifyAttributeChange(attribute, set);
		return false;
	}
	
	@Override
	public void addAttributeValue(String attribute_value, Object entity) {
		if (attribute_value.equals(UNSET))	
			return;
		AButton button = new AButton(attribute_value, this, (EEntity) entity);
		button.setRelation(attribute.getName());
		if (! (attribute instanceof AAAttribute)){
			for (AButton b: entities){
				if (b.isConnection())
					b.toButton();
				LinkedList<AButton> buttons =  AModelImpl.getButtons(b.getEntityRepresentation());
				if (buttons != null && buttons.contains(b))
					buttons.remove(b);
				remove(b);
			}
			entities.clear();
		}
		button.addActionListener(listener);
		entities.add(button);
		if (AModelImpl.isEntityBox((EEntity) entity)){
			PolylineConnection connection = null;
			try {
				connection = button.toConnection();
				listener.getView().addToContents(connection);
			} catch (ABoxNotFoundException e) { e.printStackTrace();}
		}
		else {
			add(button);
			if (!visible){
				parentBox.add(this);
				visible = true;
			}
			this.repaint();
		}
		String set = "";
		if (entities.size() == 0)
			set = UNSET;
		parentBox.notifyAttributeChange(attribute, set);
	}

	@Override
	public void unsetAttribute(Object value) {
		String set = "";
		if (value == null){
			attribute.unsetAttribute();
			for (AButton button: entities){
				removeButton(button);
			}
			parentBox.notifyAttributeChange(attribute,UNSET);
		}
		else if (value instanceof EEntity) { 
			EEntity entity = (EEntity) value;
			((AAAttribute) attribute).unsetAttribute(entity);
			for (AButton button: entities){
				if (button.getEntityRepresentation() == entity){
					removeButton(button);
					if (entities.size() == 0)
						set = UNSET;
					parentBox.notifyAttributeChange(attribute, set);
					return;
				}
			}
		}
		
	}

	public void setBorderVisibility(boolean b) {
		Border border = null;
		if (b)
			border = new ButtonBorder();
		
		for (AButton button: entities){
			if (button.isVisible())
					button.setBorder(border);
		}
		parentButton.setBorder(border);

	}

	public void deleteButton(AButton aButton) {
		remove(aButton);
		entities.remove(aButton);
		for (AButton b: entities){
			b.updateAttributeLabel();
		}
	}



}
