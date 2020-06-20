package actions;

import java.util.LinkedList;

import controller.AControllerImpl;

/**
 * Represents a set of Actions seen as one compound Action,
 * e.g moving a set of figures at once. 
 *
 */
public class ACompoundAction extends AAction{
	
	private LinkedList<AAction> actions = new LinkedList<AAction>();
	
	public void addAction(AAction action){
		actions.addLast(action);
	}
	
	public void setController(AControllerImpl controller){
		for (AAction action: actions){
			action.setController(controller);
		}
	}
	
	public int size(){
		return actions.size();
	}
	
	@Override
	public AAction restore() {
		ACompoundAction redoAction = new ACompoundAction();
		for (AAction action: actions){
			AAction a = action.restore();
			redoAction.addAction(a);
		}
		return redoAction;
	}

}
