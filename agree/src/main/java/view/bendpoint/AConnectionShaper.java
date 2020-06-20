package view.bendpoint;

import model.AModelImpl;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.PolylineConnection;

import controller.AControllerImpl;

public class AConnectionShaper extends ABendPoint {

	public AConnectionShaper(PolylineConnection in, AControllerImpl listener) {
		super(in, listener);
		addMouseListener(listener);
	}
	
	/**
	 * Removes a bend-point. 
	 */
	public void remove(){
		PolylineConnection c = outConnections.getFirst();
		ConnectionAnchor anchor = inConnection.getSourceAnchor();
		c.setSourceAnchor(anchor);
		inConnection.getParent().remove(inConnection);
		if (anchor.getOwner() instanceof AConnectionShaper){
			((AConnectionShaper) anchor.getOwner()).replaceOutConnection(c);
		}
		getParent().remove(this);
		AModelImpl.removeBendPoint(this);
	}

	public void replaceOutConnection(PolylineConnection c) {
		outConnections.clear();
		outConnections.add(c);
	}

	public PolylineConnection getOutConnection() {
		return outConnections.getFirst();
	}


}
