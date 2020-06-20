package view.box.figure;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;

import view.box.AEntityBox;
import view.box.attribute.ASelectAttribute;
import controller.AControllerImpl;
import controller.AState;
import controller.AState.CursorMode;
import exceptions.AWrongFormatException;

public class AValueFigure extends AAttributeFigure{
	
	private Label label, edit;
	private boolean firstTime = false;

	public AValueFigure(String name, AEntityBox box, AControllerImpl controller) {
		super(name, box, controller);
		Label attribute_description = new Label(String.format("     %s : ", name));
		add(attribute_description);
		repaint();
	}

	@Override
	public void addAttributeValue(String attribute_value, Object value) {
		if (label != null && label.getParent() != null)
			label.getParent().remove(label);;
		label = new Label(attribute_value);
		add(label);
		label.addMouseListener(listener);
		label.setToolTip(new Label(attribute.getToolTip()));
		if (attribute instanceof ASelectAttribute && attribute_value != "$")
			((ASelectAttribute)attribute).setCurrentMethod(attribute_value);
	}

	@Override
	public boolean setAttributeValue(Object value) {
		if (value == null){
			attribute.unsetAttribute();
			label.setText("$");
		}
		else {			
			attribute.setAttribute(value);
			label.setText(attribute.getReturnInfo().toString(attribute.getAttributeValue()));
		}
		//2013-11-14
		parentBox.notifyAttributeChange(attribute, label.getText());
		return true; 
	}
	
	public void restoreLast(){
		if (edit != null && edit.getParent() == this){
			label = new Label();
			label.setText(attribute.getLastValueString());
			label.setToolTip(new Label(attribute.getToolTip()));
			label.addMouseListener(listener);
			remove(edit);
			add(label);
		}
		else {
			label.setBorder(null);
			label.setText(attribute.getLastValueString());
		}
	}

	public void edit() {
		attribute.setLastValueString(label.getText());
		attribute.updateLastValue();
		firstTime = true;
		if (attribute.getReturnInfo().isMultipleChoise() || attribute instanceof ASelectAttribute){  
			remove(label);
			edit = new Label(label.getText());
			edit.setBackgroundColor(ColorConstants.lightBlue);
			edit.setBorder(new LineBorder(ColorConstants.darkBlue));
			attribute.saveCurrent();
			add(edit);
			edit.addMouseMotionListener(listener);
			try {
				AState.setCursorMode(CursorMode.ATTRIBUTE);
			} catch (AWrongFormatException e) {	e.printStackTrace(); }	
		}
		else {
			label.setBorder(new LineBorder(ColorConstants.darkBlue));
			try {
				AState.setCursorMode(CursorMode.TEXT);
			} catch (AWrongFormatException e) {	e.printStackTrace(); } 
		}
		AState.setActiveObject(this);
	}
	
	public void scrollUp(){
		String next = attribute.next();
		edit.setText(next);
		
	}
	
	public void scrollDown(){
		String next = attribute.previous();
		edit.setText(next);
	}

	public void setAttributeValue() throws AWrongFormatException{
		if (edit != null && edit.getParent() == this){
			remove(edit);
			if (edit.getText().equals("$")){
				label = new Label("$");
				attribute.unsetAttribute();
			}
			else label = new Label(attribute.setCurrent());
			edit = null;
			add(label);
			label.addMouseListener(listener);
		}
		else {
			if (attribute.getLastValueString().equals(label.getText())){
				label.setBorder(null);
				return;
			}
			Object o = null;
			try {
				o = attribute.getReturnInfo().fromString(label.getText());
			} catch (AWrongFormatException e){
				throw e;
			}
			if (o!= null){
				if (o instanceof List){
					List list = (List) o;
					attribute.setAttribute(list);
					label.setText(attribute.getReturnInfo().toString(attribute.getAttributeValue()));
					label.setBorder(null);
				}
				else {
					attribute.setAttribute(o);
					label.setText(attribute.getReturnInfo().toString(o));
					label.setBorder(null);
				}
			}
			else {
				label.setText(attribute.getLastValueString());
				label.setBorder(null);
				throw new AWrongFormatException(attribute.getReturnInfo().returntype);
			}
		}
		label.setToolTip(new Label(attribute.getToolTip()));
		attribute.setLastValueString(label.getText());
		parentBox.notifyAttributeChange(attribute, label.getText());
	}


	public void adjustText(char character) {

		if (! Character.isIdentifierIgnorable(character)){
			if (firstTime){
				label.setText(Character.toString(character));
				firstTime = false;
			}
			else label.setText(label.getText() + character);	
		}
		
	}

	public void eraseChar() {
		if (label.getText().length() > 0)
			label.setText(label.getText().substring(0, label.getText().length()-1));
	}

	public void displayLastValue() {
		if (edit != null && edit.getParent() == this){
			remove(edit);
			edit = null;
			label = new Label(attribute.getLastValueString());
			label.addMouseListener(listener);
			label.setToolTip(new Label(attribute.getToolTip()));
			add(label);
		}
		else {
			label.setText(attribute.getLastValueString());
			label.setBorder(null);
		}
	}

	@Override
	public void unsetAttribute(Object value) {
		if (value == null){
			attribute.unsetAttribute();
			label.setText("$");
		}
		
	}

	public void checkActive() throws AWrongFormatException {
		if (label.getBorder() == null && edit == null)
			return;
		else {
			setAttributeValue();
		}
	}

	public String getStatusMessage() {
		return attribute.getStatusMessage();
	}



}
