package actions;

import org.eclipse.draw2d.Figure;
import org.eclipse.swt.graphics.Color;

/**
 * Represents the action of changing the color of a figure.
 *
 */
public class AColorAction extends AAction {
	
	private Color color;
	private Figure figure;		
	
	public AColorAction(Color color, Figure figure){
		this.color = color;
		this.figure = figure;
	}
	
	@Override
	public AAction restore() {
		Color lColor = figure.getBackgroundColor();
		figure.setBackgroundColor(color);
		return new AColorAction(lColor, figure);
	}

}
