package actions;

import controller.AState;
import model.AModelImpl;
import sdai.ASdaiHandler;
import jsdai.lang.EEntity;

/**
 * Used when a new entity instance is created.
 *
 */
public class ACreateEntityAction extends AAction {
	
	private EEntity entity;
	
	public ACreateEntityAction(EEntity entity){
		this.entity = entity;
	}

	@Override
	public AAction restore() {
		entity = AState.getReplacementEntity(entity);
		
		ADeleteEntityAction action = new ADeleteEntityAction(entity);
		action.setController(controller);
		
		AModelImpl.removeEntity(entity);

		ASdaiHandler.deleteEntityInstance(entity);
		return action;
	}

}
