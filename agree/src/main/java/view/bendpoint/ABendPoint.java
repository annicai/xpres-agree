package view.bendpoint;

import java.util.LinkedList;

import model.AModelImpl;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PolylineConnection;

import controller.AControllerImpl;

public abstract class ABendPoint extends Label{
	
	// The last PolylineConnection in a list of connections should be the one referred 
	// to in the EntityBox
	protected LinkedList<PolylineConnection> outConnections = new LinkedList <PolylineConnection>();	
	protected PolylineConnection inConnection;
	
	public ABendPoint(PolylineConnection in, AControllerImpl listener){
		inConnection = in;
		setBackgroundColor(ColorConstants.black);
		setBorder(new LineBorder(ColorConstants.black,1));
		setOpaque(true);
		setSize(5,5);
		addMouseMotionListener(listener);
		AModelImpl.addBendpoint(this);
	}
	

	public void addOutConnection(PolylineConnection c){
		outConnections.add(c);
	}
	
	public PolylineConnection getInConnection(){
		return inConnection;
	}
	
	public void removeConnection(PolylineConnection c){
		outConnections.remove(c);
		if (outConnections.isEmpty()){
			Figure owner = (Figure) inConnection.getSourceAnchor().getOwner();
			inConnection.getParent().remove(inConnection);
			getParent().remove(this);	
			// Since boxes always refer to the connection last in line the connection just need to be removed
			AModelImpl.removeBendPoint(this);
			if (owner instanceof ABendPoint)
				((ABendPoint)owner).removeConnection(inConnection);	
		}
	}

}
