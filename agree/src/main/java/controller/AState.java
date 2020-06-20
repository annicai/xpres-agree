package controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;

import jsdai.lang.EEntity;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import exceptions.AWrongFormatException;
import view.ATextBox;
import view.AViewImpl;
import view.box.ABottomLineBorder;
import view.box.ABoxBorder;
import view.box.AEntityBox;
import view.box.figure.AValueFigure;
import actions.AAction;

public class AState {

	public enum CursorMode {SQUARE, RECTANGLE, CIRCLE, TEXT, WHEELDRAG, DRAG, RESIZE, 
		ENCLOSE, DRAGDROP, NULL, DRAGALL, MENU, COPY, DRAW, TEMPLATE, ATTRIBUTE, SCROLL_ATTRIBUTE, 
		ENTITYATTRIBUTE, DRAGSHELL, FILL, TEXTBOX, TEMP_TEXT, IMPLICIT}
	public enum KeyMode {CTRL, SHIFT, CTRL_RELEASED, NULL, CTRL_C, KEEP_CTRL}
	public enum Connection { MANHATTAN, BENDPOINT }
	public enum LabelType { PERSISTENT, BASIC }
	
	private static CursorMode cursorMode = CursorMode.NULL;
	private static Object activeObject;
	public static KeyMode keyMode = KeyMode.NULL;
	
	public static String filePath = null;
	public static LabelType labelType = LabelType.PERSISTENT;
	public static boolean visibleButtonBorder = true;
	
	public static PolylineConnection activeConnection;
	public static Point lPoint;
	public static AAction currentAction;
	
	public static LinkedList<EEntity> matches;
	private static Border labelBorder = new ABottomLineBorder();
	public static Connection connection = Connection.BENDPOINT;

	public static boolean hasChanged = false;
	private static Map<EEntity, EEntity> replacementMap = new HashMap<EEntity, EEntity>();
	
	private static AViewImpl view;
	
	public static void setView(AViewImpl v){
		view = v;
	}
	
	public static CursorMode getCursorMode() {
		return cursorMode;
	}
	
	public static void setActiveObject(Object o){
		if (activeObject instanceof ATextBox){
			((ATextBox) activeObject).setBorder(null);
			((ATextBox) activeObject).selfdestruct();
		}
		if (activeObject instanceof LinkedList<?>){
			for (Object f: (LinkedList<?> )activeObject){
				if (f instanceof AEntityBox)
					((AEntityBox) f).setBorder(new ABoxBorder());
			}
		}
		if (activeObject instanceof AValueFigure){
			((AValueFigure) activeObject).restoreLast();
		}
		if (activeObject instanceof AEntityBox){
			((AEntityBox) activeObject).setBorder(new ABoxBorder());
		}


		switch (cursorMode){
		case ENCLOSE:
			if (activeObject instanceof Figure){
				try {
				((Figure) activeObject).getParent().remove(((Figure) activeObject));
				} catch (Exception e){}
			}
			break;
		case DRAGALL:
			if (activeObject instanceof LinkedList<?>){
				for (Object f: (LinkedList<?> )activeObject){
					if (f instanceof AEntityBox)
						((AEntityBox) f).setBorder(new ABoxBorder());
				}
			}
			break;
		default:
			break;
		}
		activeObject = o;
	}
	
	public static void setCursorMode(CursorMode cm) throws AWrongFormatException {
		switch (cursorMode){
		case RECTANGLE: case CIRCLE: case TEXTBOX: case FILL:
			view.setCursor(SWT.CURSOR_ARROW);
			view.uncheck(cursorMode);
			break;
		default:
			break;
		}
	/*	switch(cursorMode){	// TODO: Check leftovers from previous states
		case TEXT:
			if (getActiveObject() instanceof AValueFigure){
				((AValueFigure)getActiveObject()).checkActive();
			}
			break;
		case ENCLOSE:
			if (getActiveObject() != null && ((Figure) getActiveObject()).getParent() != null){
				((Figure) getActiveObject()).getParent().remove((Figure)getActiveObject());
			}
		default:
			break;
		}
		switch(cm){
		case FILL:
			keyMode = KeyMode.NULL;
			break;
		case NULL:
			switch(cursorMode){	// TODO: CHECK SO THAT NO LEFTOVERS FROM PREV STATES!!!
			case ATTRIBUTE:
				break;
			default:
				break;
			}
			break;
		default:
			break;
		} */
		AState.cursorMode = cm;

	}

	public static Object getActiveObject() {
		return activeObject;
	}

	public static Border getLabelBorder() {
		return labelBorder;
	}

	public static void makeBorderLarger(int i) {
		((ABottomLineBorder) labelBorder).addSpace(i);
		
	}
	
	public static void setBorderVisibility(boolean v){
		((ABottomLineBorder) labelBorder).setVisible(v);
	}

	public static ConnectionRouter getConnectionRouter() {
		return null;
	}

	public static EEntity getReplacementEntity(EEntity value) {
		if (replacementMap.containsKey(value))
			return replacementMap.get(value);
		else return value;
	}

	public static void addReplacement(EEntity e, EEntity oldEntity) {
		replacementMap.put(oldEntity, e);		
	}
	
	public static String getTempDir(){
		String tempDir = System.getProperty("java.io.tmpdir");
		if (! tempDir.endsWith(System.getProperty("file.separator"))){
			tempDir = tempDir.concat((System.getProperty("file.separator")));
		}
		return tempDir;
	}


	
}
