package view.box;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;

public class ABottomLineBorder extends AbstractBorder{
		
		private Color color = ColorConstants.black;
		private int space = 8;
		private int lift = 1;
		private boolean showLine = true;
		
		private static final Insets INSETS = new Insets(3, 3, 3, 3);

		public Insets getInsets(IFigure figure) {
			return INSETS;
		}

		public void paint(IFigure figure, Graphics g, Insets insets) {
			if (showLine){
				Rectangle r = getPaintRectangle(figure, insets);
				r.resize(-1, -1);
				g.setBackgroundColor(color);
				Point start = new Point(r.getBottomLeft().x + space, r.getBottomLeft().y - lift);
				Point stop = new Point(r.getBottomRight().x - space, r.getBottomRight().y - lift);
				g.drawLine(start, stop);
			}
		}

		public void setColor(Color color) {
			this.color = color;
		}
			
		public void addSpace(int space){
			this.space += space;
		}
			
		public void setVisible(boolean v){
			showLine = v;
		}

		public boolean isShowingLine() {
			return showLine;
		}
		
}
