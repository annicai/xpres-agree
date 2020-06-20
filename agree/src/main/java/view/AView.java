package view;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Event;

import exceptions.AException;
import view.box.AEntityBox;

public interface AView {
	
	public void build();
	
	public void clean();
	
	public void refresh();
	
	public void zoom(Event event);
	
	public void relocateBox(AEntityBox box, AEntityBox parent);
	
	public void displayGroupMenu();
	
	public void displayShapeMenu(final Figure f);
	
	public void displayBoxMenu(final AEntityBox box);
	
	public void fitView();
	
	public void displayErrorMessage(AException e);
	
	public void displayInfoMessage(String title, String message);
	
	public void addToPaintContents(Shape figure);
	
	public void addToPaintContents(ATextBox figure);
	
	/**
	 * Make @param bounds viewable at the location specified by @param event
	 */
	public void makeViewableAt(Rectangle bounds, org.eclipse.draw2d.MouseEvent event);
	
	

}
