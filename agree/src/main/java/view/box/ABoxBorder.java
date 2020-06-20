package view.box;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;

public class ABoxBorder extends AbstractBorder{
	
		private Color color = ColorConstants.black;
	
		private static final Insets INSETS = new Insets(3, 3, 3, 3);

		public Insets getInsets(IFigure figure) {
			return INSETS;
		}

		public void paint(IFigure figure, Graphics g, Insets insets) {
			Rectangle r = getPaintRectangle(figure, insets);
			r.resize(-1, -1);
			PointList pl = new PointList(new int[] { 0, -13, -13, 0, 0,0, 0,-13});
			pl.translate(r.getBottomRight());
			g.setBackgroundColor(color);
			g.fillPolygon(pl);
			g.drawLine(r.getTopLeft(), r.getTopRight());
			g.drawLine(r.getTopLeft(), r.getTopLeft());
			g.drawLine(r.getBottomLeft(), r.getBottomRight());
			g.drawLine(r.getTopRight(), r.getBottomRight());
			g.drawLine(r.getTopLeft(), r.getBottomLeft());
		}

		public void setColor(Color color) {
			this.color = color;
		}
	}
