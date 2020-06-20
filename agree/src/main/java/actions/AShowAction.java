package actions;

import jsdai.lang.EEntity;
import model.AModelImpl;
import controller.AControllerImpl;
import controller.AState;
import sdai.ASdaiHandler;
import view.box.ABox;
import view.box.AEntityBox;

/**
 * Used when an instance box is made visible.
 *
 */
public class AShowAction extends AAction{
	
	private EEntity entity;
	
	public AShowAction(AEntityBox box){
		entity = box.getEntityRepresentation();
	}

	@Override
	public AAction restore() {
		entity = AState.getReplacementEntity(entity);
		AEntityBox box = AModelImpl.getEntityBox(entity);
		
		AHideAction action = new AHideAction(box.getEntityRepresentation(), box.getBounds());
		action.setController(controller);
		
		try{
			AModelImpl.hideEntityBox(box);
			box.hide();
			box.getParent().remove(box);
		} catch (NullPointerException e){ e.printStackTrace(); }
		
		return action;
	}

}
