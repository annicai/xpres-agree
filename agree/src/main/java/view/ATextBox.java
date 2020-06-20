package view;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.swt.SWT;

public class ATextBox extends Label{
	
	private int fontSize = 20;
	
	public ATextBox(){
		setOpaque(false);
		LineBorder border = new LineBorder();
		border.setStyle(SWT.LINE_DASH);
		setBorder(border);
		layout();
	}
	
	public void addCharacter(char c){
		if (getText().equals(" ")){
			setText("");
		}
		setText(getText().concat(Character.toString(c)));
		setSize(getPreferredSize());
		layout();
	}

	public void removeLastCharacter() {
		if (getText().length() > 0)
			setText(getText().substring(0, getText().length()-1));
		setSize(getPreferredSize());
		layout();
	}
	
	public int increaseSize(){
		fontSize++;
		return fontSize;
	}
	
	public int decreaseSize(){
		if (fontSize > 2)
			fontSize--;
		return fontSize;
	}

	public void selfdestruct() {
		if (getText().replace(" ", "").length() == 0){
			if (getParent() != null)
				getParent().remove(this);
		}
		
	}
}
