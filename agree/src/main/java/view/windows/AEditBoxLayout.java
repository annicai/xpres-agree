package view.windows;

import model.AModelImpl;

import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.ButtonBorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.Viewport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import controller.AState;
import controller.AState.LabelType;
import view.ALayout;
import view.box.ABottomLineBorder;
import view.box.ABox;
import view.box.ABoxBorder;
import view.box.AEntityBox;
import view.box.AExampleBox;

public class AEditBoxLayout {
	
	public AEditBoxLayout(final Display display, Image image, Font font){
		   final Shell shell = new Shell(display, SWT.DIALOG_TRIM);
		   shell.setText("Layout");
		   shell.setLayout(new FillLayout());
		   shell.setBackground(ColorConstants.white);
		   shell.setImage(image);
		   shell.setLayout(new FillLayout());
	        
	       FigureCanvas options = new FigureCanvas(shell, SWT.H_SCROLL | SWT.V_SCROLL);
	       LightweightSystem lws = new LightweightSystem(options);
			
	       options.setViewport(new Viewport(true));
	       options.setScrollBarVisibility(FigureCanvas.NEVER);
	       options.setBorder(new ABoxBorder());
	       options.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
	       
	       Figure figure = new Figure();
	       figure.setBackgroundColor(ColorConstants.white);
	       figure.setLayoutManager(new FreeformLayout());
	       figure.setOpaque(true);
	       
	       final ABox box = new AExampleBox("#1: Example", font);
	       if (AState.labelType == LabelType.BASIC)
	    	   box.getLabel().setText("Example");
	       box.setBackgroundColor(ALayout.getBoxColor());
	       box.repaint();
	       Figure fname = new Figure();
	       fname.setLayoutManager(new ToolbarLayout(true));
	       Figure fbutton = new Figure();
	       fbutton.setLayoutManager(new ToolbarLayout(true));
	       Label name = new Label();
	       name.setText("Name:   ''");
	       fname.add(name);
	       box.add(fname);
	       final Button button = new Button("Location: ");
	       button.setOpaque(false);
	       final Button button2 = new Button("#25");
	       button2.setOpaque(false);
	       box.add(fbutton);
	       fbutton.add(button);
	       fbutton.add(button2);
	       
	       figure.repaint();
	       options.setContents(box);
	       lws.setContents(options.getViewport());
	       options.redraw();
		   Composite composite = new Composite(shell, SWT.BORDER);
		   composite.setLayout(new GridLayout(2, true));
		   composite.setBackground(ColorConstants.white);
		   
		   final org.eclipse.swt.widgets.Button showLine = new org.eclipse.swt.widgets.Button(composite, SWT.CHECK);
		   showLine.setText("Show line");
		   showLine.setBackground(ColorConstants.white);
		   showLine.setSelection(((ABottomLineBorder) AState.getLabelBorder()).isShowingLine());
		   showLine.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
            	AState.setBorderVisibility(showLine.getSelection());
				box.repaint();
			}
			   
		   });
		   Composite c = new Composite(composite, SWT.NONE);
		   c.setBackground(ColorConstants.white);
		   c.setLayout(new GridLayout(3, false));
		   org.eclipse.swt.widgets.Button plus = new org.eclipse.swt.widgets.Button(c, SWT.NONE);
		   plus.setText("-");
		   plus.setBackground(ColorConstants.white);
		   org.eclipse.swt.widgets.Label text = new org.eclipse.swt.widgets.Label(c, SWT.NONE);
		   text.setText(" width ");
		   text.setBackground(ColorConstants.white);
		   org.eclipse.swt.widgets.Button minus = new org.eclipse.swt.widgets.Button(c, SWT.NONE);
		   minus.setText("+");
		   minus.setBackground(ColorConstants.white);
		   plus.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				   AState.makeBorderLarger(1);
				   box.repaint();
				
			}
			   
		   });
		   minus.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				   AState.makeBorderLarger(-1);
				   box.repaint();
				
			}
			   
		   });
		   final org.eclipse.swt.widgets.Button instanceBorder = new org.eclipse.swt.widgets.Button(composite, SWT.CHECK);
		   instanceBorder.setText("Show instance border");
		   instanceBorder.setBackground(ColorConstants.white);
		   if (AState.visibleButtonBorder)
			   instanceBorder.setSelection(true);
		   instanceBorder.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				if (instanceBorder.getSelection()){
					button.setBorder(new ButtonBorder());
					button2.setBorder(new ButtonBorder());
				}else {
					button.setBorder(null);
					button2.setBorder(null);
				}
				
			}
			   
		   });
		   
		   final org.eclipse.swt.widgets.Button persistentLabel = new org.eclipse.swt.widgets.Button(composite, SWT.CHECK);
		   persistentLabel.setText("Show persistent label");
		   persistentLabel.setBackground(ColorConstants.white);
		   if (AState.labelType == LabelType.PERSISTENT)
			   persistentLabel.setSelection(true);
		   persistentLabel.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				if (persistentLabel.getSelection()){
					box.getLabel().setText("#1: Example");
				} else {
				    box.getLabel().setText("Example");
				}
			}
			   
		   });
		   
		   shell.setSize(600, 150);
		   ALayout.center(shell);
		   shell.redraw();
	       shell.addListener(SWT.Close, new Listener() {
	            public void handleEvent(Event event) {
	            	if (persistentLabel.getSelection() && AState.labelType != LabelType.PERSISTENT){
	            		AState.labelType = LabelType.PERSISTENT;
	            		for (AEntityBox box: AModelImpl.getBoxes()){
	            			box.persistentName();
	            		}
	            	}
	            	else if ((!persistentLabel.getSelection()) && AState.labelType == LabelType.PERSISTENT){
	            		AState.labelType = LabelType.BASIC;
	            		for (AEntityBox box: AModelImpl.getBoxes()){
	            			box.basicName();
	            		}
	            	}
	            	if (instanceBorder.getSelection() != AState.visibleButtonBorder){
	            		boolean selection = instanceBorder.getSelection();
	            		AState.visibleButtonBorder = selection;
	            		for (AEntityBox box: AModelImpl.getBoxes()){
	            			box.setButtonVisibility(selection);
	            		}
	            	}
	            	else {
	            		for (AEntityBox box: AModelImpl.getBoxes()){
	            			box.repaint();
	            		}
	            	}
	            	AState.setBorderVisibility(showLine.getSelection());
	            	
	            }
	        });
		   
		 shell.open();
		 new Runnable(){

		 @Override
		 public void run() {
				   while (!shell.isDisposed()) {
						if (!display.readAndDispatch()) {
							display.sleep();
						}
				   }
			}
			   
		   };
	}

}
