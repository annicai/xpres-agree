package actions;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Represents the action of moving or resizing a figure. 
 *
 */
public class AMoveAction extends AAction{
	
	private Figure figure;
	private Rectangle bounds;

	public AMoveAction(Figure f, Rectangle b) {
		figure = f;
		this.bounds = new Rectangle(b.x, b.y,b.width, b.height);
	}
	
	public Rectangle getBounds(){
		return bounds;
	}
	

	@Override
	public AMoveAction restore() {
		AMoveAction rAction = new AMoveAction(figure, figure.getBounds());
		figure.setBounds(bounds);		

		figure.repaint();
		return rAction;
	}

	public boolean isDifferentTo(Rectangle r) {
		int diff = Math.abs(r.x - bounds.x) + Math.abs(r.y - bounds.y) + Math.abs(r.height - bounds.height)
				+ Math.abs(r.width - bounds.width);
		return (diff > 10);
	}

}
