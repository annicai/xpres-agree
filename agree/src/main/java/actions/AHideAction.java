package actions;

import jsdai.lang.EEntity;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import sdai.ASdaiHandler;
import view.box.AEntityBox;
import controller.AControllerImpl;
import controller.AState;

/**
 * Used when a box is hidden.
 *
 */
public class AHideAction extends AAction {
	
	private EEntity entity;
	private Rectangle bounds;

	public AHideAction(EEntity entity, Rectangle b) {
		this.entity= entity;
		this.bounds = new Rectangle(b.x, b.y,b.width, b.height);
	}

	@Override
	public AAction restore() {
		entity = AState.getReplacementEntity(entity);
		System.out.println("Entity entity: " + ASdaiHandler.getPersistantLabel(entity));
		AEntityBox box = controller.createEntityBox(entity, new Point(bounds.x, bounds.y), false);
		
		System.out.println("		Entity box " + box + " is made visible");

		box.setBounds(bounds);
		//status = "Instance ".concat(ASdaiHandler.getPersistantLabel(box.getEntityRepresentation())).concat(" made visible");
		
		AShowAction action = new AShowAction(box);
		action.setController(controller);
		return action;
	}

}
