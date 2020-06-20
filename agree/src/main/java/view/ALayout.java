package view;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class ALayout {

		public static int alphaValue = 25;
		public static int borderWidth = 2;
		public static int textSize = 20;
		public static float colorSValue = 0.35f;
		protected static String font = "Arial";
		
		public static PolygonDecoration getDecoration(){
			PolygonDecoration decoration = new PolygonDecoration();
			PointList decorationPointList = new PointList();
			decorationPointList.addPoint(0,0);
			decorationPointList.addPoint(-1,-1);
			decorationPointList.addPoint(-2,0);
			decorationPointList.addPoint(-1,1);
			decoration.setTemplate(decorationPointList);
			decoration.setBackgroundColor(ColorConstants.white);
			return decoration; 
		}
		
		public static PolygonDecoration getTemplateDecoration(){
			PolygonDecoration decoration = new PolygonDecoration();
			PointList decorationPointList = new PointList();
			decorationPointList.addPoint(0,0);
			decorationPointList.addPoint(-1,-1);
			decorationPointList.addPoint(-2,0);
			decorationPointList.addPoint(-1,1);
			decoration.setTemplate(decorationPointList);
			decoration.setBackgroundColor(ColorConstants.white);
			return decoration;
		}
		
		public static Color getBoxColor(){
			return new Color(null,255,255,206); 
		}
		
		public static int alphaValue(){
			return alphaValue;
		}
		
		public static Color getBlueColor(){
			return new Color(null,178,223,238);
		}
		
		public static Color getGreenColor(){
			return new Color(null, 193,255,193);
		}
		
		public static Color getPinkColor(){
			return new Color(null, 255,182,193);
		}
		
		public static Color getBrownColor(){
				return new Color(null,222,184,135);
			}
		
		public static Color getGroupColor(){
			return new Color(null, 255, 228, 255);
		}
		
	   public static void center(Shell shell) {

	        org.eclipse.swt.graphics.Rectangle bds = shell.getDisplay().getBounds();
	        Point p = shell.getSize();

	        int nLeft = (bds.width - p.x) / 2;
	        int nTop = (bds.height - p.y) / 2;

	        shell.setBounds(nLeft, nTop, p.x, p.y);
	   }	
}
