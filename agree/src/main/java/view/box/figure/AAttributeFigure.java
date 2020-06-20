package view.box.figure;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.widgets.Listener;

import controller.AControllerImpl;
import view.box.AEntityBox;
import view.box.attribute.AAttribute;

/**
 * The graphical representation of an attribute. 
 * Has subclasses AEntityFigure and AValueFigure.
 *
 */
public abstract class AAttributeFigure extends Figure{
	
	protected AAttribute attribute;
	protected AEntityBox parentBox;
	protected AControllerImpl listener;
	protected final String UNSET = "$";
	
	public AAttributeFigure(String string, AEntityBox box, AControllerImpl controller){
		ToolbarLayout layout = new ToolbarLayout(true);
	    setLayoutManager(layout);
	    parentBox = box;
	    this.setOpaque(false);
	    parentBox.add(this);
	    parentBox.repaint();
	    listener = controller;
	}
	
	/**
	 * Sets the attribute to @param value and updates the graphical
	 * representation accordingly.
	 * 
	 * @param value
	 * @return true if successful
	 */
	public abstract boolean setAttributeValue(Object value);
	
	/**
	 * Adds a attribute value to the graphical representation.
	 * 
	 * @param attribute_value
	 * @param value
	 */
	public abstract void addAttributeValue(String attribute_value, Object value);
	
	/**
	 * Unsets and attribute completely if @param value is null, otherwise when the attribute is an aggregate 
	 * and @param value is not null, it is removed from the aggregate.
	 * 
	 * @param value
	 */
	public abstract void unsetAttribute(Object value);
	
	public void setAttribute(AAttribute attribute){
		this.attribute=attribute;
	}
	
	public AAttribute getAttribute(){
		return attribute;
	}
	
	public AControllerImpl getListener(){
		return listener;
	}
	
	public AEntityBox getBox(){
		return parentBox;
	}
}
