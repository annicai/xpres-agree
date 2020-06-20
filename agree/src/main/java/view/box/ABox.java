package view.box;

import java.util.LinkedList;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import controller.AState;

/**
 * Superclass of AEntityBox and ... TODO: AGroupBox 
 * 
 * @author Annica
 *
 */
public abstract class ABox extends Figure {
	
	// Connections coming out from the box
	protected LinkedList <PolylineConnection> outGoingConnections = new LinkedList <PolylineConnection>();
	// Connections going in to the box
	protected LinkedList <PolylineConnection> inComingConnections = new LinkedList <PolylineConnection>();
	// Box name
	protected Label label;
	
	public ABox(String text, Font font){
		Label label = new Label();		
		label.setFont(font);
		label.setText(text);
		this.label = label;
	    ToolbarLayout layout = new ToolbarLayout();
		setLayoutManager(layout);	
	    setOpaque(true);
	    label.setBorder(AState.getLabelBorder());
		add(label);
		setBoxBorder();
	}

	/** 
	 * Change the border of the box back to the standard border. (After being marked etc) 
	 **/
	public abstract void setBoxBorder(); 
	public Label getLabel(){ return label; }
	public void addConnection(PolylineConnection c){ outGoingConnections.add(c); }
	public LinkedList<PolylineConnection> getConnections(){ return outGoingConnections; }
	public void addIncomingConnection(PolylineConnection c){ inComingConnections.add(c); }
	public LinkedList<PolylineConnection> getIncomingConnections(){	return inComingConnections; }
	public void removeIncomingConnection(PolylineConnection c) { inComingConnections.remove(c); }
	public void removeOutgoingConnection(PolylineConnection c) { outGoingConnections.remove(c);	}

	public abstract void hide();
}
