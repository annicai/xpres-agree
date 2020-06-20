package actions;

import controller.AState;
import jsdai.lang.EEntity;

/**
 * Represents deletion of entities.
 *
 */
public class ADeleteEntityAction extends AAction {
	
	private String entity;
	private EEntity oldEntity;

	public ADeleteEntityAction(EEntity entity) {
		this.entity = entity.getClass().getSimpleName().substring(1);
		oldEntity = entity;
		// TODO: - Store all set information

	}

	@Override
	public AAction restore() {	
		EEntity e = controller.createInstance(entity, false);
		AState.addReplacement(e, oldEntity);
		
		ACreateEntityAction action = new ACreateEntityAction(e);
		action.setController(controller);
		return action;
	}

}
