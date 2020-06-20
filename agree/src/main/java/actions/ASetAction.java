package actions;

import java.util.LinkedList;
import java.util.List;

import controller.AState;
import model.AModelImpl;
import jsdai.lang.AEntity;
import jsdai.lang.Aggregate;
import jsdai.lang.CAggregate;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;
import sdai.ASdaiHandler;
import view.box.AEntityBox;
import view.box.figure.AAttributeFigure;

/**
 * Represents the action of setting an instance attribute to a value.
 * A null arguments represents an 'unset' action.
 *
 */
public class ASetAction extends AAction {
	
	private Object value;
	private EEntity parentEntity;
	private String attributeName;

	public ASetAction(Object lastValue, EEntity parentEntity, String attributeName) {
		this.parentEntity = parentEntity;
		this.attributeName = attributeName;
		
		if (lastValue instanceof Aggregate){
			LinkedList<Object> lVal = new LinkedList<Object>();
			try {
				SdaiIterator it = ((Aggregate)lastValue).createIterator();
				while (it.next()){
					lVal.add(((Aggregate)lastValue).getCurrentMemberObject(it));
				}
			} catch (SdaiException e) {
				e.printStackTrace();
			}
			value = lVal;
		}	else value = lastValue;
	}

	@Override
	public ASetAction restore() {
		if (value instanceof EEntity && !ASdaiHandler.isEntity((EEntity) value)){
			value = AState.getReplacementEntity((EEntity) value);
		}

		parentEntity = AState.getReplacementEntity(parentEntity);
		AEntityBox box = AModelImpl.getEntityBox(parentEntity);
		AAttributeFigure figure = box.getAttributeFigure(attributeName);
		
		Object lastValue = figure.getAttribute().getAttributeValue();

		if (figure.getAttribute().isAggregate()){
			try {
				if (value == null ||lastValue == null)
					figure.setAttributeValue(value);
				else if ( value instanceof EEntity && ((CAggregate) lastValue).isMember((EEntity) value))
					figure.unsetAttribute(value);
				else figure.setAttributeValue(value);
			} catch (SdaiException e){	e.printStackTrace();
			} catch (Exception e) {e.printStackTrace();}
			return new ASetAction(value, parentEntity, attributeName);
		}	
		else {
			try {
				figure.setAttributeValue(value);
			} catch (Exception e) {e.printStackTrace();}
			figure.repaint();
			return new ASetAction(lastValue, parentEntity, attributeName);	
		}
	}
	
}
