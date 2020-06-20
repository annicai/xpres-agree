package view.box;

import org.eclipse.swt.graphics.Font;

public class AExampleBox extends ABox {

	public AExampleBox(String text, Font font) {
		super(text, font);
	}

	@Override
	public void setBoxBorder() {
		setBorder(new ABoxBorder());
	}

	@Override
	public void hide() {		
	}

}
