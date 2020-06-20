package controller;


import java.util.*;

import jsdai.lang.*;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import actions.*;
import controller.AState.*;
import exceptions.*;
import sdai.ASdaiHandler;
import util.AVector;
import view.ALayout;
import view.ATextBox;
import view.AViewImpl;
import view.bendpoint.ABendPoint;
import view.bendpoint.AConnectionShaper;
import view.box.*;
import view.box.attribute.AAAttribute;
import view.box.attribute.AAttribute;
import view.box.attribute.ASelectAttribute;
import view.box.attribute.extra.AButton;
import view.box.figure.*;
import view.edit.AEditAttribute;
import view.tree.display.AInstanceNode;
import model.AModelImpl;


public class AControllerImpl implements Observer, MouseListener, MouseMotionListener, Listener, ActionListener {
	
	private AModelImpl model;
	private AViewImpl view;
	
	private Stack<AAction> undoStack = new Stack<AAction>();
	private Stack<AAction> redoStack = new Stack<AAction>();
	private final int CTRL = 262144, SHIFT = 131072, ERASE = 8, F = 102, M = 109, S = 115, Y = 121, Z = 122, DELETE = 127,
			KEYDOWN = 16777217, KEYUP = 16777218, KEYRIGHT = 16777219, KEYLEFT = 16777220, I = 105, O = 111;

	public void setModel(AModelImpl model) {
		this.model = model;
	}
	
	public void setView(AViewImpl view){
		this.view = view;
	}
	
	public AViewImpl getView(){
		return view;
	}

	public void addListeners() {
		view.getContents().addMouseListener(this);
		view.getContents().addMouseMotionListener(this);
        view.getCanvas().addListener(SWT.MouseWheel, this);
		view.getCanvas().addListener(SWT.KeyDown, this);
		view.getCanvas().addListener(SWT.KeyUp, this);
	}

	@Override
	public void mouseDoubleClicked(MouseEvent event) {
		AState.keyMode = KeyMode.NULL;
		try {
			switch (AState.getCursorMode() ){
			case RESIZE:
				((ABox)AState.getActiveObject()).setBorder(new ABoxBorder());
				AState.setCursorMode(CursorMode.NULL);
				break;
			case ATTRIBUTE: case TEXT:
				try {
					AValueFigure figure = ((AValueFigure) AState.getActiveObject());
					Object lastValue = figure.getAttribute().getAttributeValue();
					AAction action = new ASetAction(lastValue, figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
					addToUndoStack(action);
					AAttribute attribute = ((AValueFigure) AState.getActiveObject()).getAttribute();
					if (attribute instanceof ASelectAttribute){
						if (((ASelectAttribute)attribute).isUnset()){
							((AValueFigure) AState.getActiveObject()).setAttributeValue();
							AState.setCursorMode(CursorMode.NULL);
							AState.setActiveObject(null);
							return;
						}
						String value = view.getUserInput(attribute.getReturnInfo());
						if (value == null){
							((AValueFigure) AState.getActiveObject()).displayLastValue();
							AState.setCursorMode(CursorMode.NULL);
							AState.setActiveObject(null);
							return;
						}
						
						Object o = attribute.getReturnInfo().fromString(value);
						attribute.setLastValue(o);
					}
					((AValueFigure) AState.getActiveObject()).setAttributeValue();
					highLightBox(event.getSource());
				} catch (AWrongFormatException e){
					((AValueFigure) AState.getActiveObject()).displayLastValue();
					view.displayErrorMessage(e);
				}
				AState.setCursorMode(CursorMode.NULL);
				AState.setActiveObject(null);
				return;
			case NULL:
				if (event.getSource() instanceof Label){
					if (((Label)event.getSource()).getParent() instanceof AValueFigure){
						AValueFigure figure = (AValueFigure) ((Label)event.getSource()).getParent();
						view.editAttribute(figure);
						view.setStatusMessage(figure.getStatusMessage());
						return;
					}
				}
				break;
			case DRAW:
				if (event.getSource() instanceof AEntityBox && (AState.matches.contains(((AEntityBox) event.getSource()).getEntityRepresentation()))){
					try{
						AEntityBox box = (AEntityBox) AState.activeConnection.getSourceAnchor().getOwner();
						EEntity entity = ((AEntityBox) event.getSource()).getEntityRepresentation();
						LinkedList<AEntityFigure> figures = box.getValidAttributes(entity);
						AEntityFigure figure;
						if (figures.size() == 0)
							return;	
						if (figures.size() > 1){
							figure = figures.getFirst();
							int index = view.showOptionDialog(figures);
							figure = figures.get(index);
						}
						else figure = figures.getFirst();
						
						if (figure.getAttribute().getReturnInfo().aggLevel > 1){
							int count = ((AAAttribute)figure.getAttribute()).getAggregateCount();
							int index;
							if (count == -1)
								index = 1;
							else index = view.showOptionDialog(count);
							if (index == -1)
								return;
							((AAAttribute) figure.getAttribute()).setIndex(index + 1);
						}
						
						if (figure.getAttribute().isAggregate()){
							ASetAction action = new ASetAction(entity,  figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
							
							addToUndoStack(action);
						}
						else{
							AAction action = new ASetAction(figure.getAttribute().getAttributeValue(), figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());

							addToUndoStack(action);
						}

						figure.setAttributeValue(entity);
					} catch (Exception e) {e.printStackTrace();}
				}
				view.removeFromContents((Figure) AState.getActiveObject());
				view.removeFromContents(AState.activeConnection);
				view.refresh();			
				AState.setCursorMode(CursorMode.NULL);
				AState.setActiveObject(null);
				AState.matches = null;
				break;
			default:
				break;
			}
			if (event.getSource() instanceof ABox){
				ABox box = (ABox) event.getSource();
				AState.currentAction = new AMoveAction(box, box.getBounds());
				
				AState.setCursorMode(CursorMode.RESIZE);
				AState.setActiveObject(box);
				AState.lPoint = event.getLocation();
				view.setActiveColor(box.getBackgroundColor());
				highLightBox(box);
				box.setBorder(new LineBorder(2));
			}
			else if (event.getSource() instanceof AConnectionShaper){
				((AConnectionShaper)event.getSource()).setBackgroundColor(ColorConstants.red);
				AState.setActiveObject((AConnectionShaper)event.getSource());
			}
			else if (event.getSource() == view.getContents()){
					Figure f = (Figure) view.getPaintFigure(event.getLocation());
					if (f instanceof Ellipse || f instanceof RoundedRectangle ){
						AState.setCursorMode(CursorMode.RESIZE);
						AState.setActiveObject(f);
						AState.lPoint = event.getLocation();
						f.setBorder(new LineBorder(2));
						view.addToPaintContents((Shape)f);
					}
					else if (f instanceof ATextBox){
						AState.setCursorMode(CursorMode.RESIZE);
						AState.setActiveObject(f);
						AState.lPoint = event.getLocation();
						f.setBorder(new LineBorder(2));
						view.addToPaintContents((ATextBox)f);
					}
			}
		} catch (AWrongFormatException e){
			view.displayErrorMessage(e);
		}
		
	}

	
	private void highLightBox(Object o) {
		if (o instanceof AEntityBox){
			if (AState.hasChanged){
				view.updateText();
			}
			AEntityBox box = (AEntityBox) o;
			view.highlightText(ASdaiHandler.getPersistantLabel(box.getEntityRepresentation()));
			view.removeFromContents(box);
			view.addToContents(box);
		}
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (AState.getCursorMode() == CursorMode.TEXT){	
			try {
				AValueFigure figure = ((AValueFigure) AState.getActiveObject());
				Object lastValue = figure.getAttribute().getAttributeValue();
				AAction action = new ASetAction(lastValue, figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
				addToUndoStack(action);
				AAttribute attribute = ((AValueFigure) AState.getActiveObject()).getAttribute();
				if (attribute instanceof ASelectAttribute){
					if (((ASelectAttribute)attribute).isUnset()){
						((AValueFigure) AState.getActiveObject()).setAttributeValue();
						AState.setCursorMode(CursorMode.NULL);
						AState.setActiveObject(null);
						return;
					}
					String value = view.getUserInput(attribute.getReturnInfo());
					if (value == null){
						((AValueFigure) AState.getActiveObject()).displayLastValue();
						AState.setCursorMode(CursorMode.NULL);
						AState.setActiveObject(null);
						return;
					}
					
					Object o = attribute.getReturnInfo().fromString(value);
					attribute.setLastValue(o);
				}
				((AValueFigure) AState.getActiveObject()).setAttributeValue();
				AState.setCursorMode(CursorMode.NULL);
				AState.setActiveObject(null);
			} catch (AWrongFormatException ee){
				((AValueFigure) AState.getActiveObject()).displayLastValue();
				view.displayErrorMessage(ee);
			}
		}

		view.setStatusMessage("");
		try {
			switch (AState.keyMode){
			case CTRL_RELEASED:
				AState.lPoint = e.getLocation();
				if (e.button == 3){
					view.displayGroupMenu();
					AState.setCursorMode(CursorMode.MENU);
				}
				else AState.setCursorMode(CursorMode.DRAGALL);
				return;
			default:
				break;
			}
			
			switch (AState.getCursorMode()){
			case ENCLOSE:
				if (AState.getActiveObject() instanceof Figure){
					try{
						((Figure) AState.getActiveObject()).getParent().remove((IFigure) AState.getActiveObject());
					} catch (Exception ex){};
				}
				break;
			case FILL: 
				if (e.getSource() instanceof Figure){
					Figure figure = (Figure) e.getSource();
					Color color = view.getActiveColor();
					if (figure == view.getContents()){
						IFigure f = view.getPaintFigure(e.getLocation());
						if (f instanceof Ellipse || f instanceof RoundedRectangle ){
							setColor((Figure) f, color, view.getBorderColor());
						}
						else if (f instanceof ATextBox){
							((ATextBox) f).setForegroundColor(view.getActiveColor());
						}
						else setColor(view.getContentsFigure(), color, null);				
					}else 
						setColor(figure, color, null);
				}
				return;
			case TEMP_TEXT:
				AState.setCursorMode(CursorMode.NULL);
				AState.setActiveObject(null);
				break;
			case TEXTBOX:
				ATextBox text = new ATextBox();
				AState.lPoint = e.getLocation();
				view.addToPaintContents(text);
				text.setFont(view.getTextFont(ALayout.textSize));
				text.addCharacter(' ');
				return;
			case DRAW: case ATTRIBUTE: case TEXT:
				return;
			case CIRCLE:
				AState.lPoint = e.getLocation();
				view.addToPaintContents(new Ellipse());
				return;
			case RECTANGLE:
				AState.lPoint = e.getLocation();
				view.addToPaintContents(new RoundedRectangle());
				return;
			case ENTITYATTRIBUTE:
				((AEditAttribute)AState.getActiveObject()).close();
			default:
				break;
			}
			switch(e.button){
				case 1:	//Left click
					if (e.getSource() instanceof ABox || e.getSource() instanceof ABendPoint){ 
						AState.setCursorMode(CursorMode.DRAG);
						AState.setActiveObject(e.getSource());
						AState.lPoint = e.getLocation();
						if (AState.currentAction == null){
							AState.currentAction = new AMoveAction((Figure) e.getSource(), ((Figure) e.getSource()).getBounds());
						}
					} else if (e.getSource() instanceof Label && ((Label)e.getSource()).getParent() instanceof PolylineConnection){
						AState.setCursorMode(CursorMode.DRAG);
						AState.setActiveObject(e.getSource());
						AState.lPoint = e.getLocation();
					}
					else if (e.getSource() == view.getContents()){
						Figure f = (Figure) view.getPaintFigure(e.getLocation());
						if (f instanceof Ellipse || f instanceof RoundedRectangle || f instanceof ATextBox ){
							AState.setCursorMode(CursorMode.DRAG);
							AState.setActiveObject(f);
							AState.lPoint = e.getLocation();
							AMoveAction action = new AMoveAction(f, f.getBounds());
							addToUndoStack(action);
						}
						else {
							AState.lPoint = e.getLocation();
							Figure selectionBox = new Figure();
							selectionBox.setLayoutManager(new org.eclipse.draw2d.XYLayout());
							selectionBox.setBorder(new LineBorder(ColorConstants.black,1));
							selectionBox.setOpaque(false);
							selectionBox.setLocation(AState.lPoint);
							AState.setActiveObject(selectionBox);
							view.addToContents(selectionBox);
							AState.setCursorMode(CursorMode.ENCLOSE);
						}
					}
					break;
				case 2:	// Scroll click
					AState.setCursorMode(CursorMode.WHEELDRAG);
					AState.lPoint= new Point(e.x, e.y);
					break;
				case 3:	// Right click
				/*	if (AState.getCursorMode() == CursorMode.DRAGALL){
						view.displayGroupMenu();
						break;
					}*/
					if (e.getSource() instanceof AEntityBox){
						try {
							view.displayBoxMenu((AEntityBox) e.getSource());
						} catch (NoSuchMethodError ee){ee.printStackTrace();
						} catch(Exception ee) {ee.printStackTrace(); }
					} 
					else if (e.getSource() == view.getContents()){
						Figure f = (Figure) view.getPaintFigure(e.getLocation());
						if (f instanceof Ellipse || f instanceof RoundedRectangle || f instanceof ATextBox){
							view.displayShapeMenu(f);
						} else view.nullMenu();
					}
					break;
			}
		} catch (AWrongFormatException ex){
			view.displayErrorMessage(ex);
		}
	}

	private void setColor(Figure figure, Color color, Color borderColor) {
		AAction action = new AColorAction(figure.getBackgroundColor(), figure);
		figure.setBackgroundColor(color);
		if (figure instanceof Ellipse || figure instanceof RoundedRectangle)
			figure.setForegroundColor(borderColor);
		addToUndoStack(action);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		try {
			
			switch (AState.getCursorMode()){
			case DRAW: case ATTRIBUTE: case TEXT: case FILL: case TEXTBOX: case MENU:
				return;
			case CIRCLE: case SQUARE: case RECTANGLE:
		//		((Figure)AState.activeObject).setOpaque(false);
				AState.setActiveObject(null);
				return;
			case ENCLOSE:
				Figure enclosureBox = (Figure) AState.getActiveObject();
				LinkedList<Figure> boxesToMove = new LinkedList<Figure>();
				ACompoundAction action = new ACompoundAction();
				for (AEntityBox box : AModelImpl.getBoxes()) {
					if (enclosureBox.getBounds().contains(box.getBounds())){
						AMoveAction moveAction = new AMoveAction(box, box.getBounds());
						action.addAction(moveAction);
						boxesToMove.add(box);
						box.setBorder(new LineBorder(ColorConstants.red,7));
					}			
				}
				for (ABendPoint bp: AModelImpl.getBendpoints()){
					if (enclosureBox.getBounds().contains(bp.getBounds())){
						AMoveAction moveAction = new AMoveAction(bp, bp.getBounds());
						action.addAction(moveAction);
						boxesToMove.add(bp);
						bp.setBackgroundColor(ColorConstants.red);
					}
				}
				AState.setCursorMode(CursorMode.NULL);
				view.removeFromContents((Figure) AState.getActiveObject());
				
				if (boxesToMove.size() > 0){
					AState.keyMode = KeyMode.CTRL_RELEASED;
					AState.setActiveObject(boxesToMove);
					addToUndoStack(action); 
				}				
				return;
			case RESIZE:
				if (AState.getActiveObject() instanceof ABox)
					((ABox) AState.getActiveObject()).setBorder(new ABoxBorder());
				else if (AState.getActiveObject() instanceof ATextBox){
					LineBorder border = new LineBorder();
					ATextBox tb = ((ATextBox) AState.getActiveObject());
			
					border.setStyle(SWT.LINE_DASH);
					int hdiff = tb.getSize().height - tb.getPreferredSize().height;
					int wdiff = tb.getSize().width - tb.getPreferredSize().width;

					tb.setSize(tb.getPreferredSize());
					tb.setLocation(tb.getLocation().getTranslated(wdiff/2, hdiff/2));
					tb.setBorder(border);
					AState.setCursorMode(CursorMode.TEMP_TEXT);
					return;
				} else ((Figure) AState.getActiveObject()).setBorder(null);
			//	break;
			case DRAG:
				if (AState.getActiveObject() instanceof Figure && 
						 AState.currentAction != null && AState.currentAction instanceof AMoveAction){
					Figure f = (Figure) AState.getActiveObject();
					AMoveAction ma = (AMoveAction) AState.currentAction;
					if (ma.isDifferentTo(f.getBounds())){
						addToUndoStack(AState.currentAction);
						AState.currentAction = null;
					}
				}
				break;
			case DRAGALL:
				if (AState.getActiveObject() instanceof LinkedList<?>){
					for (Object f: (LinkedList<?>) AState.getActiveObject()){
						if (f instanceof AEntityBox)
							((AEntityBox)f).setBorder(new ABoxBorder());
					}
				}
				break;
			case DRAGDROP:
				if (AState.getActiveObject()!=null){
					AEntityBox b = null;
					if (AState.getActiveObject() instanceof AInstanceNode){
						EEntity entity = AModelImpl.modelMapping.get(AState.getActiveObject());
						if (AModelImpl.isEntityBox(entity)){
							b = AModelImpl.getEntityBox(entity);
							Rectangle r = b.getBounds();
							view.makeViewableAt(r, event);
						}
						else {
							b = createEntityBox(entity, event.getLocation(), true);
							b.setSize(b.getPreferredSize());
						}
					}
					else if (AState.getActiveObject() instanceof String){	//Create new entity instance
						ACompoundAction ca = new ACompoundAction();
						EEntity entity = createInstance((String) AState.getActiveObject(), false);	
						
						ACreateEntityAction a1 = new ACreateEntityAction(entity);
						a1.setController(this);
						ca.addAction(a1);
						
						if (entity != null){
							b = createEntityBox(entity, event.getLocation(), false);
							b.setSize(b.getPreferredSize());
							
							AShowAction a2 = new AShowAction(b);
							a2.setController(this);
							ca.addAction(a2);
						}
						
						addToUndoStack(ca);
						
					}
					view.refresh();
				}
				break;
			default:
				break;			
			}
			AState.keyMode = KeyMode.NULL;
			AState.setCursorMode(CursorMode.NULL);
			AState.lPoint = new Point(0,0);
			AState.setActiveObject(null);
		}catch (Exception e){ e.printStackTrace(); }
	}

	/**
	 * Creates a new entity instance, and save restore information if @param save is set to true.
	 * 
	 * @param string - the entity
	 * @param save - whether of not the action should be un-doable
	 * @return the instance created
	 */
	public EEntity createInstance(String string, boolean save) {		
		EEntity entity = null;
		try {
			entity = (EEntity) ASdaiHandler.createEntityInstance(string);
		} catch (AAbstractEntityException e) {
			view.displayErrorMessage(e);
			return null;
		}
		if (save){
			ACreateEntityAction action = new ACreateEntityAction(entity);
			action.setController(this);
			addToUndoStack(action);
		}
		model.addEntityToTree(entity);
		view.refresh();
		return entity;
	}
	
	/**
	 * 
	 * @param entity
	 * @param point
	 * @param save
	 * @return
	 */
	public AEntityBox createEntityBox(EEntity entity, Point point, boolean save) {		
		AEntityBox entitybox = null;
        if (! AModelImpl.isEntityBox(entity)){
        	String text = "";
    		if (AState.labelType == AState.LabelType.PERSISTENT)
    			text = String.format("%s : %s", ASdaiHandler.getPersistantLabel(entity), ASdaiHandler.getEntityAsString(entity));
    		else 
    			text = String.format(" %s", ASdaiHandler.getEntityAsString(entity));
    		entitybox = new AEntityBox(text, view.getBoxFont(), point, entity, this);
    	}
        else return AModelImpl.getEntityBox(entity);
        
		// Check if there exists buttons to this entity
		if (AModelImpl.hasButtons(entity)){
			for (AButton button: AModelImpl.getButtons(entity)){
				PolylineConnection connection = null;
				try {
					connection = button.toConnection();
					view.addToContents(connection);
				} catch (ABoxNotFoundException e) {	e.printStackTrace();
				}
			}
		}
		if (save){
			AShowAction action = new AShowAction(entitybox);
			action.setController(this);
			addToUndoStack(action);
		}
       return entitybox;
	}
	
	@Override
	public void mouseDragged(MouseEvent event) {
		try{
			Point point = null;				
			Dimension d = null;;										
			if (AState.lPoint!= null){
				point = event.getLocation();
				d = point.getDifference(AState.lPoint);
			}

			switch (AState.getCursorMode()) {
			case DRAW: case ATTRIBUTE:
				return;
			case RESIZE:
			    AState.lPoint = point;
			    Figure f = (Figure) AState.getActiveObject();
			    if (f.getBounds().getExpanded(d.width, d.height).width > 15 && f.getBounds().getExpanded(d.width, d.height).height > 15)
			    	f.setBounds(f.getBounds().getExpanded(d.width, d.height));
			    if (AState.getActiveObject() instanceof ATextBox){
			    	ATextBox tb = (ATextBox) AState.getActiveObject();
			    	Dimension pdim = tb.getPreferredSize();
			    	Dimension adim = tb.getSize();
			    	if (pdim.width < adim.width){
			    		int size = tb.increaseSize();
			    		tb.setFont(view.getTextFont(size));
			    		tb.repaint();
			    	}
			    	else {
			    		int size = tb.decreaseSize();
			    		tb.setFont(view.getTextFont(size));
			    		tb.repaint();
			    	}
			    }
				break;
			case ENCLOSE: case CIRCLE: case RECTANGLE: case TEXT:
				((Figure) AState.getActiveObject()).setSize(Math.abs(d.width), Math.abs(d.height));
				if (d.width > 0) d.width = 0;
				if (d.height> 0) d.height = 0;
				((Figure) AState.getActiveObject()).setLocation(AState.lPoint.getTranslated(d.width, d.height));
				if (AState.keyMode == KeyMode.CTRL){
					int height = ((Figure) AState.getActiveObject()).getSize().height;
					int width = ((Figure) AState.getActiveObject()).getSize().width;
					if (height > width)
						((Figure) AState.getActiveObject()).setSize(height, height);
					else ((Figure) AState.getActiveObject()).setSize(width, width);
				}
				else if (AState.getCursorMode() == CursorMode.CIRCLE)
					view.setStatusMessage("Hold CTRL for a circle");
				else if (AState.getCursorMode() == CursorMode.RECTANGLE)
					view.setStatusMessage("Hold CTRL for a square");
				break;
			case DRAGALL:
				 AState.lPoint = point;
				 if (AState.getActiveObject() instanceof LinkedList<?>){
					 LinkedList<Figure> boxesToMove = (LinkedList<Figure>) AState.getActiveObject();
					 for (Figure box: boxesToMove){
						box.setBounds(box.getBounds().getTranslated(d.width, d.height));
						if (box.getLocation().x < view.getContents().getBounds().x)
							box.setLocation(new Point(view.getContents().getBounds().x, box.getLocation().y));
						if (box.getLocation().y < view.getContents().getBounds().y)
							box.setLocation(new Point(box.getLocation().x, view.getContents().getBounds().y));
					}
				 }
				 else AState.setCursorMode(CursorMode.NULL); 
				break;
			case DRAG:
			    AState.lPoint = point;
			    if (AState.getActiveObject() instanceof Label && ((Label)AState.getActiveObject()).getParent() instanceof PolylineConnection){
			    	//TODO: Calculate the actual angle of the connection, adjust uv-distance accordingly

			    	Label label = (Label) AState.getActiveObject();
			    	
			    	PolylineConnection connection = (PolylineConnection) label.getParent();
			    	ConnectionEndpointLocator locator = AModelImpl.getLocator(AState.getActiveObject());

			    	//Not actually... on boarder for source-locator
			    /*	Rectangle sourceBounds = ((Figure) (connection.getSourceAnchor().getOwner())).getBounds();
			    	int sourceY  = sourceBounds.y - sourceBounds.height/2;
			    	int sourceX  = sourceBounds.x + sourceBounds.width/2;
			    	
			    	Rectangle targetBounds = ((Figure) (connection.getTargetAnchor().getOwner())).getBounds();
			    	int targetY  = targetBounds.y; // - targetBounds.height/2;
			    	int targetX  = targetBounds.x; // + targetBounds.width/2; */
			    	Point start = connection.getStart();
			    	Point end = connection.getEnd();
		//	    	AVector connectionVector = new AVector(targetX-sourceX, targetY-sourceY);
			    	AVector connectionVector = new AVector(end.x - start.x, end.y - start.y);
				    AVector mouseVector = new AVector(d.width, d.height);
			    	
				    double t = mouseVector.projectOnto(connectionVector);
				    if (end.y > start.y && d.height > 0)
				    	t *=-1;
				    else if (end.y < start.y && d.height < 0)
				    	t *=-1;
			    	locator.setUDistance((int)( locator.getUDistance() + t));
			    	//FIXME: end.y = start.y
	//		    	if (Math.abs(label.getLocation().x -start.x) < 5)	// FIXME: Doesn´t work with bendpoints
	//		    		locator.setUDistance((int) (locator.getUDistance()-2*t));

					connection.layout();
			    }	
			    else {
				    ((Figure) AState.getActiveObject()).setBounds(((Figure) AState.getActiveObject()).getBounds().getTranslated(d.width, d.height));
			    }
				break;
			case COPY:
				break;
			case DRAGDROP:
				break;
			case MENU:
				break;
			case NULL:
				break;
			case TEMPLATE:
				break;
			case WHEELDRAG:
				Point p = new Point((AState.lPoint.x - event.x)*-1, (AState.lPoint.y - event.y)*-1);
				view.translateContents(p);
				AState.lPoint = event.getLocation();
				return;
			default:
				break;
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent event) {
		if (view.textIsModified()){
			view.updateGraphics();
		}
		switch (AState.getCursorMode() ){
		case DRAW:
			if (event.getSource() instanceof AEntityBox){
				AEntityBox box = (AEntityBox) event.getSource();
				if (AState.matches.contains(box.getEntityRepresentation())){
					box.setBorder(new LineBorder(ColorConstants.green));
				}
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void mouseExited(MouseEvent event) {
		switch (AState.getCursorMode() ){
		/*case ENCLOSE:
			view.removeFromContents((Figure) AState.activeObject);
			AState.activeObject = null;
			AState.cursorMode = CursorMode.NULL;*/
		case DRAW:
			if (event.getSource() instanceof AEntityBox){
				AEntityBox box = (AEntityBox) event.getSource();
				if (AState.matches.contains(box.getEntityRepresentation())){
					box.setBorder(new ABoxBorder());
				}
			}
			break;
		default:
			break;
		}
		
	}

	@Override
	public void mouseHover(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		switch (AState.getCursorMode()){
		case DRAW:
			((Figure) AState.getActiveObject()).setLocation(event.getLocation().getTranslated(-2, -2));
			break;
		default:
			break;
		}
		
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.type) {
			case SWT.MouseWheel:
				if (AState.getCursorMode() == CursorMode.ATTRIBUTE){
					if (event.count < 0)
						((AValueFigure) AState.getActiveObject()).scrollDown();
					else 
						((AValueFigure) AState.getActiveObject()).scrollUp();
				}
				else if (AState.getCursorMode() != CursorMode.TEXT)
					view.zoom(event);
				break;
			case SWT.KeyDown:
				if ((AState.getCursorMode() == CursorMode.TEXTBOX || AState.getCursorMode() == CursorMode.TEMP_TEXT) && AState.getActiveObject() instanceof ATextBox){
					switch (event.keyCode){
					case SHIFT: case CTRL:
						return;
					case ERASE:				
						((ATextBox)AState.getActiveObject()).removeLastCharacter();
						return;
					default:
						((ATextBox)AState.getActiveObject()).addCharacter(event.character);
						return;
					}
				}
				if (AState.getCursorMode() == CursorMode.TEXT){
					if (event.keyCode == ERASE) 
						view.eraseCharacter();
					else if (event.keyCode != CTRL && event.keyCode != SHIFT)
							view.adjustText(event.character);
					return;
				} else {
					switch (event.keyCode){
					case I:
						if (AState.keyMode == KeyMode.CTRL){
							Event ev = new Event();
							ev.count = -3; 	ev.x = 450; ev.y = 450;
							view.zoom(ev);
						}
						return;
					case O:
						if (AState.keyMode == KeyMode.CTRL){
							Event ev = new Event();
							ev.count = 3; 	ev.x = 450; ev.y = 450;
							view.zoom(ev);
						}
						return;
					case F:
						if (AState.keyMode == KeyMode.CTRL)
							view.fitView();
						break;
					case M: 
						view.toggleSideMenu();
						break;
					case S:
						if (AState.keyMode == KeyMode.CTRL)
							view.save();
						break;
					case Z: 
						if (AState.keyMode == KeyMode.CTRL && !undoStack.isEmpty()){
							undo();
						}
						return;
					case Y: 
						if (AState.keyMode == KeyMode.CTRL && !redoStack.isEmpty())
							redo();
						return;
					case CTRL:       	
						AState.keyMode = KeyMode.CTRL;
						break;
					case SHIFT: 			
						AState.keyMode = KeyMode.SHIFT;
						break;
					case DELETE:
						if (AState.getActiveObject() instanceof LinkedList<?>){

							for (Object o: (LinkedList<?>)AState.getActiveObject()){
								if (o instanceof AConnectionShaper)
									((AConnectionShaper)o).remove();
								if (o instanceof AEntityBox){
									delete(((AEntityBox) o).getEntityRepresentation());
								}
							}
							view.refresh();
						}
						else if (AState.getActiveObject() instanceof AConnectionShaper){
							((AConnectionShaper)AState.getActiveObject()).remove();
							view.refresh();
						}
						break;
					case KEYDOWN:
						if (AState.getCursorMode() == CursorMode.ATTRIBUTE){
							((AValueFigure) AState.getActiveObject()).scrollDown();
						}
						else view.translateContents(new Point(0,50));
					  	break;
					case KEYUP: 
						if (AState.getCursorMode() == CursorMode.ATTRIBUTE){
							((AValueFigure) AState.getActiveObject()).scrollUp();
						}
						else view.translateContents(new Point(0,-50));
					  	break;
					case KEYRIGHT: 
						view.translateContents(new Point(50,0));
						break;
					case KEYLEFT: 
						 view.translateContents(new Point(-50,0));
						 break;
					default:
						break;
					}
				}
				break;
			case SWT.KeyUp:
				switch (event.keyCode){
					case CTRL: 
						AState.keyMode = KeyMode.CTRL_RELEASED;
						break;
					case Z: case Y: case I: case O:
						if (AState.keyMode == KeyMode.CTRL)
							return;
					default:
						AState.keyMode = KeyMode.NULL;
						break;
				}
				break;
			default:
				break;
		}
	}
	
	public void addToUndoStack(AAction action){
		if (! redoStack.isEmpty()){
			redoStack.clear();
		}
		undoStack.add(action);
		view.setUndoAction(true);
	}
	
	public void undo(){
		try {
			if (!undoStack.isEmpty()){
				AAction action = undoStack.pop().restore();				
				redoStack.add(action);
				view.refresh();
			}
			if (undoStack.isEmpty())
				view.setUndoAction(false);
			view.setRedoAction(true);
			
		} catch (Exception e){e.printStackTrace();}

	}
	
	public void redo(){
		try {
			if (!redoStack.isEmpty()){
				AAction action = redoStack.pop().restore();
				undoStack.add(action);
				view.refresh();
			}
		}catch (Exception e){e.printStackTrace();}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		try {
			if (event.getSource() instanceof AButton && !(AState.getCursorMode() == CursorMode.TEXT)){
				AButton button = (AButton) event.getSource();
				AEntityBox parentBox = button.getParentBox();

				EEntity entity = button.getEntityRepresentation();
				if (entity != null){
					AEntityBox box = createEntityBox(entity, null, true);
					view.relocateBox(box, parentBox);
				}
				else {
					AEditAttribute ed = new AEditAttribute(view.getDisplay(), this, (AEntityFigure) button.getParent());
					AState.setActiveObject(ed);
					AState.setCursorMode(CursorMode.ENTITYATTRIBUTE);
				}
			}
		} catch (AWrongFormatException e){
			view.displayErrorMessage(e);
		}
		
	}

	public AModelImpl getModel() {
		return model;
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof Action){
			switch ((Action) arg){
				case UNDO:
					undo();
					return;
				case REDO:
					redo();
					return;
			}
		}
		if (arg instanceof EEntity){
			AEntityBox box = createEntityBox((EEntity)arg, new Point(0,0), false);
			box.setSize(box.getPreferredSize());
			if (AState.lPoint != null)
				box.setLocation(AState.lPoint);
			if (AState.getActiveObject() instanceof Dimension)
				box.setSize((Dimension) AState.getActiveObject());
			if (AState.getActiveObject() instanceof AEntityBox){
				AEntityBox obox = (AEntityBox) AState.getActiveObject();
				int wDiff = obox.getSize().width - box.getSize().width;
				Point nloc = new Point(obox.getLocation().x + wDiff/2, obox.getLocation().y - box.getSize().height - 40);
				box.setLocation(nloc);
			}
		}
		if (arg instanceof AEntityFigure){	//Edit entity attribute
			AEditAttribute ed = new AEditAttribute(view.getDisplay(), this, (AEntityFigure) arg);
			AState.setActiveObject(ed);
			try {
				AState.setCursorMode(CursorMode.ENTITYATTRIBUTE);
			} catch (Exception e) {	e.printStackTrace();
			}
		}
		if (arg instanceof String){	//Create entitybox

			EEntity entity = null;
			try {
				entity = (EEntity) ASdaiHandler.createEntityInstance((String)arg);
			} catch (AAbstractEntityException e) {
				view.displayErrorMessage(e);
				return;
			}
			if (entity != null){
				AEntityBox nbox = createEntityBox(entity, new Point(0,0), true);
				AEntityBox obox = (AEntityBox) AState.getActiveObject();

				if (AState.getCursorMode() == CursorMode.IMPLICIT && AState.getActiveObject() instanceof ABox){

					EEntity e = obox.getEntityRepresentation();
					LinkedList<AEntityFigure> figures = nbox.getValidAttributes(e);
					AEntityFigure figure;
					if (figures.size() == 0)
						return;	
					if (figures.size() > 1){
						figure = figures.getFirst();
						nbox.setVisible(false);
						int index = view.showOptionDialog(figures);
						if (index < 0){	// Cancel
							ASdaiHandler.deleteEntityInstance(entity);
							nbox.getParent().remove(nbox);
							return;
						}
						nbox.setVisible(true);
						figure = figures.get(index);
					}
					else figure = figures.getFirst();
					
					if (figure.getAttribute().isAggregate()){
						ASetAction action = new ASetAction(e,  figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
						addToUndoStack(action);
					}
					else{
						ASetAction action = new ASetAction(figure.getAttribute().getAttributeValue(),  figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
						addToUndoStack(action);
					}
					figure.setAttributeValue(e);

				}
				
				nbox.setSize(nbox.getPreferredSize());
				int wDiff = obox.getSize().width - nbox.getSize().width;
				Point nloc = new Point(obox.getLocation().x + wDiff/2, obox.getLocation().y - nbox.getSize().height - 40);
				nbox.setLocation(nloc);
				
			}
		}
		else if (arg instanceof AAction){
			((AAction)arg).setController(this);
			addToUndoStack((AAction) arg);
		}
		else if (arg == null){	
			undoStack.clear();
			redoStack.clear();
		}
		AState.setActiveObject(null);
		try {
			AState.setCursorMode(CursorMode.NULL);
		} catch (AWrongFormatException e) {	e.printStackTrace();
		}
		
	}
	
	/**
	 * Delete an entity.
	 * @param entity
	 */
	public void delete(EEntity entity) {
		if (AModelImpl.isEntityBox(entity)){
			ACompoundAction action = new ACompoundAction();
			AEntityBox box = AModelImpl.getEntityBox(entity);
			AAction a1 = new AHideAction(entity, box.getBounds());
			a1.setController(this);

			AModelImpl.hideEntityBox(box);
			box.hide();
			box.getParent().remove(box);
			AModelImpl.deleteEntity(entity);
			
			ADeleteEntityAction a2 = new ADeleteEntityAction(entity);
			a2.setController(this);
			
			action.addAction(a2);
			action.addAction(a1);
			
			ASdaiHandler.deleteEntityInstance(entity);
			addToUndoStack(action);
		}
		else {
			ADeleteEntityAction action = new  ADeleteEntityAction(entity);
			action.setController(this);
			
			ASdaiHandler.deleteEntityInstance(entity);
			addToUndoStack(action);
		}		
		AState.hasChanged = true;
	}

}
