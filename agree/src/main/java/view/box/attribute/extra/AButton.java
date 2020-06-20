package view.box.attribute.extra;

import jsdai.lang.AEntity;
import jsdai.lang.CAggregate;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;
import model.AModelImpl;

import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LayoutListener;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;

import controller.AState;
import exceptions.ABoxNotFoundException;
import view.ALayout;
import view.bendpoint.ABendPoint;
import view.bendpoint.AConnectionRouter;
import view.box.AEntityBox;
import view.box.attribute.AAAttribute;
import view.box.figure.AEntityFigure;

public class AButton extends Button{
	
	private AEntityFigure parent;
	private int aggIndex = -1;
	private EEntity entity = null;
	private String relation;
	private PolylineConnection connection;
	private Label attribute = null;
	
	public AButton(String text, AEntityFigure parent, EEntity entity) {
		super(text);
		this.entity = entity;
		this.parent = parent;
		setOpaque(false);
		if (entity != null)
			AModelImpl.addButtonMapping(entity, this);
		if (!AState.visibleButtonBorder)
			setBorder(null);
	}
	
	public void setRelation(String relation){
		this.relation = relation;
	}
	
	public void updateAttributeLabel(){
		if (attribute != null)
		{
			attribute.setText(relation); 
			if (parent.getAttribute().isAggregate()){
				AAAttribute a = (AAAttribute) parent.getAttribute();
				int index =  ((AAAttribute) parent.getAttribute()).getIndexOf((CAggregate)a.getAttributeValue(), entity);
				attribute.setText(attribute.getText().concat(" [").concat(Integer.toString(index)).concat("]"));
			}
		}

	}
	
	public PolylineConnection toConnection() throws ABoxNotFoundException {	
		final AEntityBox sourceBox = parent.getBox();
		AEntityBox targetBox = null;
		if (AModelImpl.isEntityBox(entity)){
			targetBox = AModelImpl.getEntityBox(entity);
		}
		else throw new ABoxNotFoundException(); //Box must be created BEFORE toConnection is called!
														
		connection = new PolylineConnection();
		
		if (parent.getBox().getEntityRepresentation() == entity){
			connection.setVisible(false);
		}

		
		final ChopboxAnchor targetAnchor = new ChopboxAnchor(targetBox);
		final ChopboxAnchor sourceAnchor;
		
		
		if (parent.getAttribute().getReturnInfo().aggLevel > 1){
			aggIndex = getListIndex();
			AConnectionRouter cr = AModelImpl.getConnectionRouter(parent, aggIndex);
			if (cr == null){
				PolylineConnection in = new PolylineConnection();
				parent.getListener().getView().addToContents(in);
				ChopboxAnchor sa = new ChopboxAnchor(sourceBox);
				cr = new AConnectionRouter(in, parent, aggIndex);
				cr.addMouseMotionListener(parent.getListener());
				cr.addMouseListener(parent.getListener());
				parent.getListener().getView().addToContents(cr);
				int xWise =  (int) (sourceBox.getLocation().x + 0.5*sourceBox.getSize().width + targetBox.getLocation().x + 0.5*targetBox.getSize().width)/2;
				int yWise =  (int) (sourceBox.getLocation().y + 0.5*sourceBox.getSize().height + targetBox.getLocation().y + 0.5*targetBox.getSize().height)/2;
				cr.setLocation(new Point(xWise, yWise));
				ChopboxAnchor ta = new ChopboxAnchor(cr);
				AModelImpl.addBendpoint(cr);
				in.setSourceAnchor(sa);
				in.setTargetAnchor(ta);
				
				ConnectionEndpointLocator l = new ConnectionEndpointLocator(in,true);
				l.setUDistance(15);
				l.setVDistance(5);

				Label lIndex = new Label("[".concat(Integer.toString(aggIndex)).concat("]"));
				in.add(lIndex, l);

				AModelImpl.addConnectionRouter(parent, aggIndex, cr);
			}
			sourceAnchor = new ChopboxAnchor(cr);
			cr.addOutConnection(connection);
		}
		else  sourceAnchor = new ChopboxAnchor(sourceBox);
	
		connection.setConnectionRouter(AState.getConnectionRouter());
		parent.buttonToConnection(this, connection);

		attribute = new Label(relation);
		
		if (parent.getAttribute().getReturnInfo().aggLevel > 1){
			attribute.setText(attribute.getText().concat(" [").concat(Integer.toString(aggIndex)).concat("]"));
		}
		if (parent.getAttribute().isAggregate()){
			AAAttribute a = (AAAttribute) parent.getAttribute();
			int index =  ((AAAttribute) parent.getAttribute()).getIndexOf((CAggregate)a.getAttributeValue(), entity);
			attribute.setText(attribute.getText().concat(" [").concat(Integer.toString(index)).concat("]"));
		}
		
		
		sourceBox.addConnection(connection);
		targetBox.addIncomingConnection(connection);

		connection.setSourceAnchor(sourceAnchor);
		connection.setTargetAnchor(targetAnchor);	
		
		final ConnectionEndpointLocator locator = new ConnectionEndpointLocator(connection,true);
		locator.setUDistance(15);
		locator.setVDistance(0);


		AModelImpl.putInLocatorMapping(attribute, locator);
		attribute.addMouseListener(parent.getListener());
		attribute.addMouseMotionListener(parent.getListener());
		connection.add(attribute,locator);
		connection.addLayoutListener(new LayoutListener(){

			@Override
			public void invalidate(IFigure arg0) {}

			@Override
			public boolean layout(IFigure figure) {
				if (sourceBox.containsPoint(attribute.getLocation()))
					locator.setUDistance(locator.getUDistance() - 3);
				if (locator.getUDistance() < 1)
					locator.setUDistance(1);
				double x2 = Math.pow(connection.getEnd().x - connection.getStart().x, 2);
				double y2 = Math.pow(connection.getEnd().y - connection.getStart().y, 2);
				// TODO: Adjust V-direction if location of attribute to far from line
				// Seems to be a bug in Draw2d
				
				double length = Math.sqrt(x2+y2);
				if (locator.getUDistance() > length)
					locator.setUDistance((int) (length - 3));
				return false;
			}

			@Override
			public void postLayout(IFigure arg0) {}

			@Override
			public void remove(IFigure arg0) {}

			@Override
			public void setConstraint(IFigure arg0, Object arg1) {}
			
		});

		PolygonDecoration decoration = ALayout.getDecoration();
	/*	if (! view.parser.inActiveSchema(source)){
			c.setAlpha(GIConstants.alphaValue());
			attribute.setVisible(false);
			decoration.setAlpha(GIConstants.alphaValue());
		}*/
		connection.setTargetDecoration(decoration);

		return connection;
	}
	
	public void toButton() {
		if (connection != null){
			removeConnection();
			parent.addButton(this);
			parent.getBox().removeOutgoingConnection(connection);
			connection = null;
		}
		if (!AState.visibleButtonBorder)
			setBorder(null);
	}

	public void removeConnection() {
		if (connection.getSourceAnchor().getOwner() instanceof ABendPoint){
			ABendPoint bp = (ABendPoint) connection.getSourceAnchor().getOwner();
			bp.removeConnection(connection);
		}
		connection.getParent().remove(connection);
		
	}

	public EEntity getEntityRepresentation() {
		return entity;
	}
	
	public AEntityBox getParentBox(){
		return parent.getBox();
	}

	public boolean isConnection() {
		if (connection != null) 
			return true;
		return false;
	}
	
	/**
	 * Returns the index of the list in which an entity object is contained. (Aa's) ---FIXME: All aggregates!
	 * **/
	public int getListIndex(){
        Object arglist[] = new Object[1];
        arglist[0] = null;        ///TODO? Aaa... with selects!!!!
		CAggregate agg;
		try {
			Object o =  parent.getAttribute().getAttributeValue();
			if (o instanceof CAggregate)
				agg = (CAggregate) o;
			else return -1;
			int aggSize = agg.getMemberCount();
			for (int i=1; i <= aggSize; i++){
				if (agg.getByIndexObject(i) instanceof CAggregate && ((CAggregate) agg.getByIndexObject(i)).isMember(entity)){
					return i;
				}
			}
		} catch (IllegalArgumentException e) { e.printStackTrace();
		} catch (SdaiException e) { e.printStackTrace();
		} catch (NullPointerException e) {e.printStackTrace();
		}
		return -1;
	}
	
	/**
	 * Removes a button, as when the entity is deleted
	 */
	public void remove() {
		if (isConnection()){
			toButton();
		}
		parent.deleteButton(this);
	}

	public void setEntityRepresentation(EEntity nentity) {
		entity = nentity;
	}
	
	/**
	 * Returns true if the entity representation is contained in the return value if this attribute.
	 * Used when "Replace with... " 
	 * 
	 * @return
	 */
	public boolean isContained() {
		try{
			Object o = parent.getAttribute().getAttributeValue();
			if (o instanceof EEntity && o == entity)
				return true;
			if (o instanceof AEntity && ((AEntity)o).isMember(entity))
				return true;
			if (o instanceof CAggregate){
				SdaiIterator it = ((CAggregate)o).createIterator();
				while (it.next()){
					Object o2 = ((CAggregate)o).getCurrentMemberObject(it);
					if (o2 instanceof AEntity && ((AEntity)o2).isMember(entity))
						return true;
					if (o2 instanceof AEntity && ((AEntity)o2).isMember(entity))
						return true;
				}
				
			}
		} catch (SdaiException e){ e.printStackTrace();			
		} catch (Exception e) {e.printStackTrace();}
		
		return false;
	}



}
