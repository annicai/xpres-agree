package export;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import model.AModelImpl;

import org.apache.batik.svggen.SVGIDGenerator;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gmf.runtime.draw2d.ui.render.awt.internal.svg.export.GraphicsSVG;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import view.AContentsFigure;
import view.box.AEntityBox;

public class ASVGExporter {

	public static void exportToSVG(AContentsFigure contentsFigure, String selected, LinkedList<Figure> figures) {		//TODO: TRANSFER JARS!!!!!!!!!!
		Rectangle rootFigureBounds = computeDiagramSize(contentsFigure, figures);
		if (rootFigureBounds.x < 0){
			rootFigureBounds.width = rootFigureBounds.width + rootFigureBounds.x;
			rootFigureBounds.x = 0;
		}
		if (rootFigureBounds.y < 0)
			rootFigureBounds.height = rootFigureBounds.height + rootFigureBounds.y;
			rootFigureBounds.y = 0;
			
		try {
			GraphicsSVG graphics = GraphicsSVG.getInstance(rootFigureBounds);
			SVGIDGenerator generator = new SVGIDGenerator();
			graphics.getSVGGraphics2D().getGeneratorContext().setIDGenerator(generator);
			graphics.translate(rootFigureBounds.getLocation());
			
			contentsFigure.paint(graphics); 
			OutputStream outputStream = new FileOutputStream(selected);
			graphics.getSVGGraphics2D().stream(new BufferedWriter(new OutputStreamWriter(outputStream)));
			outputStream.close();
		} catch (Exception e) {	e.printStackTrace();}
	}
	
	public static void convertToPDF(AContentsFigure contentsFigure, String selected, LinkedList<Figure> figures) {
		double oldScale = contentsFigure.getScale();
		Point point = fitPrintView(contentsFigure, figures);

	    contentsFigure.repaint();
        for (Figure f: figures){
        	f.repaint();
        }
        
        if (selected != null){
       	 	 exportToSVG(contentsFigure, selected.substring(0,selected.length()-3).concat("svg"), figures);
			 
       	 	 File svgfile = new File(selected.substring(0,selected.length()-3).concat("svg")); 
       	 	 File pdffile = new File(selected);
       	 	 try {
				convertSVG2PDF(svgfile, pdffile);
       	 	 } catch (IOException e) { e.printStackTrace();
       	 	 } catch (TranscoderException e) { 
       	 		 e.printStackTrace();
       	 	 }	
				
			svgfile.delete();
 	       	if (Desktop.isDesktopSupported()) {
	    	    try {
	    	        File pdf = new File(selected);
	    	        Desktop.getDesktop().open(pdf);
	    	    } catch (Exception ex) { ex.printStackTrace();   }
 	       	}
       }
       
       for (Figure f: figures){
        	f.setBounds(f.getBounds().getTranslated(point.x, point.y));
        	f.repaint();
       }
       
	   contentsFigure.setScale(oldScale);	
	}

	//FIXME:Pdf cannot be read with Å/Ä/Ö characters
	private static void convertSVG2PDF(File svg, File pdf) throws IOException, TranscoderException{
	
		Transcoder transcoder = new PDFTranscoder();
		InputStream in = new java.io.FileInputStream(svg);
		try {
		 	TranscoderInput input = new TranscoderInput(in);
		 	OutputStream out = new java.io.FileOutputStream(pdf);
		 	out = new java.io.BufferedOutputStream(out);
		 	try {
		 		TranscoderOutput output = new TranscoderOutput(out);
		 		transcoder.transcode(input, output);
		 	} finally {
		 		out.close();
		 	}
		 } finally {
		 	in.close();
		 }
	}
	

	public static Rectangle computeDiagramSize(AContentsFigure contentsFigure, LinkedList<Figure> figures) {
		int height=0;
		int width=0;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;

		for (Figure bgFigure : figures){
		    for (Object f: bgFigure.getChildren()){
		    	if (f instanceof AEntityBox || f instanceof Ellipse || f instanceof RoundedRectangle){
			        if (((Figure) f).getLocation().x < minX)
			        	minX = ((Figure) f).getLocation().x;
			        if (((Figure) f).getLocation().y < minY)
			        	minY = ((Figure) f).getLocation().y;
			        if ( (((Figure) f).getLocation().x +((Figure) f).getSize().width) > width)
			        	width = (((Figure) f).getLocation().x +((Figure) f).getSize().width);
			        if ( (((Figure) f).getLocation().y +((Figure) f).getSize().height) > height)
			        	height = (((Figure) f).getLocation().y +((Figure) f).getSize().height);
			        
		    	}
		    }
		}
		return new Rectangle(minX-50, minY-50, width+100, height+100);
	}
	
	public static Point fitPrintView(AContentsFigure contentsFigure, LinkedList<Figure> figures) {
		 contentsFigure.setScale(1);
		 int minX = Integer.MAX_VALUE; int minY = Integer.MAX_VALUE; int maxX = Integer.MIN_VALUE; int maxY = Integer.MIN_VALUE;
			for (Figure bgFigure : figures){
			    for (Object f: bgFigure.getChildren()){
			    	if (f instanceof AEntityBox || f instanceof Ellipse || f instanceof RoundedRectangle){
			    		 Rectangle r = ((Figure) f).getBounds();
						 if (r.x < minX)
							 minX = r.x;
						 if (r.y < minY)
							 minY = r.y;
						 if (r.x + r.width > maxX)
							 maxX = r.x + r.width;
						 if (r.y + r.height > maxY)
							 maxY = r.y + r.height;		        
			    	}
			    }
			}
		 minX -= 25;
		 minY -= 25;
		 for (Figure f: figures){
			 f.setBounds(f.getBounds().getTranslated(minX*-1,minY*-1));
		 }
		 return new Point(minX, minY);
	}
}
