package view.help;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

public class AHelpLabelProvider implements IColorProvider, IFontProvider, IBaseLabelProvider {
	
	private Display display;
	
	public AHelpLabelProvider(Display display){
		super();
		this.display = display;
	}

	@Override
	public Font getFont(Object arg0) {
		return new Font(display,"Arial",9,SWT.BOLD);
	}

	@Override
	public Color getBackground(Object arg0) {
		return ColorConstants.white;

	}

	@Override
	public Color getForeground(Object arg0) {
		return new Color(display, 128,128,255);
	}

	@Override
	public void addListener(ILabelProviderListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {
		// TODO Auto-generated method stub
		
	}

}
