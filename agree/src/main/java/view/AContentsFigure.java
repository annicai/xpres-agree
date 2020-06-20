package view;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.ScaledGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Translatable;

public class AContentsFigure extends Figure implements ScalableFigure{
	
	private double scale = 1.0;

	protected void paintClientArea(Graphics graphics) {
		if (getChildren().isEmpty())
			return;
		if (scale == 1.0) {
			super.paintClientArea(graphics);
		} else {
			ScaledGraphics g = new ScaledGraphics(graphics);
			boolean optimizeClip = getBorder() == null	|| getBorder().isOpaque();
		if (!optimizeClip)
			g.clipRect(getBounds().getCropped(getInsets()));
			g.translate(getBounds().x + getInsets().left, getBounds().y + getInsets().top);
			g.scale(scale);
			g.pushState();
			paintChildren(g);
			g.dispose();
			graphics.restoreState();
		} 
	}
	
	public Rectangle getClientArea(Rectangle rect) {
			super.getClientArea(rect);
			rect.width /= scale;
			rect.height /= scale;
			return rect;
		}
	
	public Dimension getPreferredSize(int wHint, int hHint) {
			Dimension d = super.getPreferredSize(wHint, hHint);
			int w = getInsets().getWidth();
			int h = getInsets().getHeight();
			return d.getExpanded(-w, -h).scale(scale).expand(w, h);	
		}
		
	protected boolean useLocalCoordinates() {
		return true;
	}
	
	public void translateToParent(Translatable t) {
		t.performScale(scale);
		super.translateToParent(t);
		}


	public void translateFromParent(Translatable t) {
			super.translateFromParent(t);
			t.performScale(1 / scale);
	}
	
	
	@Override
	public double getScale() {
		return scale;
	}

	@Override
	public void setScale(double scale) {
		this.scale=scale;
		repaint();
	}

}
