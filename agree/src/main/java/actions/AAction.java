package actions;

import controller.AControllerImpl;

/**
 * An undo-able action, e.g moving a figure or setting an attribute
 *
 */

public abstract class AAction {
	
	protected AControllerImpl controller;
		
	public abstract AAction restore();
	
	public void setController(AControllerImpl controller){
		this.controller = controller;
	}


}
