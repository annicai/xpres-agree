package view.bendpoint;

import org.eclipse.draw2d.PolylineConnection;

import view.box.figure.AAttributeFigure;
import controller.AControllerImpl;

public class AConnectionRouter extends ABendPoint {
	
	private AAttributeFigure figure;
	private int index;

	public AConnectionRouter(PolylineConnection in, AAttributeFigure figure, int index) {
		super(in, figure.getListener());
		this.figure = figure;
		this.index = index;
	}

	public AAttributeFigure getFigure() {
		return figure;
	}

	public int getIndex() {
		return index;
	}
}
