package view;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import jsdai.lang.*;
import model.AModelImpl;
import model.tree.structure.ATreeNode;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Label;

import actions.AAction;
import actions.ACompoundAction;
import actions.ACreateEntityAction;
import actions.ADeleteEntityAction;
import actions.AHideAction;
import actions.AMoveAction;
import actions.AShowAction;
import controller.AState;
import controller.AState.CursorMode;
import controller.AState.KeyMode;
import controller.AState.LabelType;
import controller.Action;
import exceptions.AEntitiesNotFoundException;
import exceptions.AException;
import exceptions.AMissingEntitiesException;
import exceptions.AUnrecognizedSchemaException;
import exceptions.AWrongFormatException;
import export.ASVGExporter;
import sdai.*;
import sdai.structure.*;
import util.Util;
import view.bendpoint.ABendPoint;
import view.bendpoint.AConnectionShaper;
import view.box.*;
import view.box.attribute.extra.AReturnInfo;
import view.box.figure.*;
import view.edit.AEditAttribute;
import view.help.AHelpWindow;
import view.tree.display.*;
import view.windows.AAbout;
import view.windows.AAllSchemas;
import view.windows.AEditBoxLayout;
import view.windows.ATextEditor;
import view.windows.ATextSearch;
import xml.AXMLParser;

public class AViewImpl extends Observable implements AView, Observer {
	
	private Display display;
	private Shell shell;
    private Color[] borderColors = new Color[5];
    private LinkedList<Button> colorButtons = new LinkedList<Button>() ;
    private Map<Button, Button> borderMap = new HashMap<Button, Button>() ;
	private final String NAME = "AGREE - A Graphical Express Editor";
	private Image AGREE_LOGO_BIG, AGREE_LOGO_SMALL;
	private StyledText textArea;
	private FigureCanvas canvas;
	private Composite compA, compB;
	private AModelImpl model;
	private ATreeViewerNode twn;
	private ALeftTreeViewer treeViewer;
	private Figure contents, paintContents, bgBox;
	private AContentsFigure contentsFigure;
	private Point SCREEN_RESOLUTION = new Point(940, 669);	//FIXME: Dynamic resolution
	private SashForm sash, verticalSash; private MenuItem showSideMenu;
	private StatusLineManager manager; String sMessage;
	private AViewImpl self;
	private boolean working, textModified = false;
	private org.eclipse.swt.widgets.Button circle, rectangle, text, fill;
	private List<Color> colorList = new ArrayList<Color>();
	private MenuItem fileSave, fileSaveItem, undoItem, redoItem;
	private LinkedList<Figure> backgroundFigures = new LinkedList<Figure>();
	
	public AViewImpl(){
		java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		SCREEN_RESOLUTION = new Point(screenSize.getWidth(), screenSize.getHeight());
	}
	

	@Override
	public void build() {
		display = new Display();
		self = this;
        shell = new Shell(display);
        shell.setLayout(new FillLayout());
        shell.setText(NAME);
		
        shell.setBounds(Display.getDefault().getPrimaryMonitor().getBounds());
        shell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
            	saveOption();		
            	shell.dispose();
            }
          });
        
        shell.addListener (SWT.Resize,  new Listener () {
            public void handleEvent (Event e) {
            	try {
	        		int height = shell.getSize().y;
	        		int percent =  20000/height;
	        		int difference = 5000/height;
	        		verticalSash.setWeights(new int[] {percent/2, percent/2 - difference ,(100 + difference - percent)});
            	} catch (Exception ex){
            		ex.printStackTrace();
            	}
        	    int width = shell.getSize().x;
        	    int vertical = 41000/width;
        	    if (vertical > 99){
        	    	vertical = 95;
        	    }
        	    sash.setWeights(new int[] { vertical, 100-vertical});
        		shell.redraw();

            }
          });
        
        try{

    		AGREE_LOGO_SMALL = AResourceLoader.getSmallAgreeLogo();
        	AGREE_LOGO_BIG = AResourceLoader.getBigAgreeLogo();
        	
        	shell.setImage(AGREE_LOGO_SMALL); 
          
        } catch (Exception e){ e.printStackTrace(); }
		
		ALayout.center(shell);
		
		addColors();
		
		addTextArea();		// Split screen vertically (twice) ->  | TextArea || ..A.. || StatusBar |
		addCanvas();		// Split "A" Horizontally | ..B.. || Canvas |
		addTree();			// Split "B" vertically | Menu || Tree |
		
		createMenuItems();
		
    	final Shell splash = new Shell(SWT.ON_TOP);
    	final ProgressBar bar = new ProgressBar(splash, SWT.NONE);
    	bar.setMaximum(4);

		Image sp = AResourceLoader.getSplash();
    	
    	Label label = new Label(splash, SWT.NONE);
    	label.setImage(sp);
        FormLayout layout = new FormLayout();
        splash.setLayout(layout);
        FormData labelData = new FormData();
        labelData.right = new FormAttachment(100, 0);
        labelData.bottom = new FormAttachment(100, 0);
        label.setLayoutData(labelData);
        FormData progressData = new FormData();
        progressData.left = new FormAttachment(0, 5);
        progressData.right = new FormAttachment(100, -5);
        progressData.bottom = new FormAttachment(100, -5);
        bar.setLayoutData(progressData);
        splash.pack();
        org.eclipse.swt.graphics.Rectangle splashRect = splash.getBounds();
        org.eclipse.swt.graphics.Rectangle displayRect = display.getBounds();
        int x = (displayRect.width - splashRect.width) / 2;
        int y = (displayRect.height - splashRect.height) / 2;
        splash.setLocation(x, y);
        display.asyncExec(new Runnable() {

			@Override
			public void run() {
		        splash.open();
		        for (int i = 1; i < 7; i++){
					bar.setSelection(i);
			        try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		        }//
		        splash.close();
	   			/* MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
	   	         messageBox.setMessage("You are using a possibly unstable version of AGREE. \n"
	   	         		+ "The behaviour might differ from the Windows versions. \nWhen dragging entities to the canvas, the drop action must be followed by a right click to properly place the entity box.");
	 	         messageBox.setText("AGREE for Linux");
	 	         messageBox.open();*/
	   			/* MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
	   	         messageBox.setMessage("You are using a possibly unstable version of AGREE. \n"
	   	         		+ "The behaviour might differ from the Windows versions. ");
	 	         messageBox.setText("AGREE for OS X");
	 	         messageBox.open();*/
			}});
		
		AState.setView(this);

	}
	
	private void addColors() {
	    colorList.add(new Color(display, 255,255,128));  //Yellow 
	    colorList.add(new Color(display, 255,191,128));   //Organge
	    colorList.add(new Color(display, 255,128, 128));	// Red
	    colorList.add(new Color(display, 255,128,180));   //Pink  
	    colorList.add(new Color(display, 201,128,255));   //Purple 
	  
	    colorList.add(new Color(display, 128,128,255));	//Blue
	  //  colorMap.put("Sea blue", new Color(display, 126,207,255));	//D
	    colorList.add(new Color(display, 128,255,255));	//Light blue
	    colorList.add(new Color(display, 128,255,128)); 	//Green
	    colorList.add(new Color(display, 0,0,0)); 		//White
	    colorList.add(new Color(display, 255,255,255)); 	//Black
	}


	private void addTree() {
		verticalSash = new SashForm(compB, SWT.NONE | SWT.VERTICAL); 
		
		createPaintBar(verticalSash);
	
		treeViewer = new ALeftTreeViewer(verticalSash);
		int height = shell.getSize().y;
		//int percent =  20000/height;
		
		int percent = 20; //FIXME!!!!!!!!!!!!!
		// FIXME: This does not work well with a secondary monitor, might even crash...
		verticalSash.setWeights(new int[] {percent/2, percent/2 - 5 ,(100 + 5 -percent)});
		
	}

	private void createPaintBar(SashForm sash) {
		Composite comp = new Composite(sash, SWT.NONE);
		comp.setLayout(new FillLayout());
		
        final FigureCanvas options = new FigureCanvas(comp, SWT.H_SCROLL | SWT.V_SCROLL);
        LightweightSystem lws = new LightweightSystem(options);
        
        options.addMouseTrackListener(new MouseTrackListener(){

			@Override
			public void mouseEnter(MouseEvent arg0) {}

			@Override
			public void mouseExit(MouseEvent arg0) {
				shell.setCursor(display.getSystemCursor(SWT.CURSOR_ARROW));
			}

			@Override
			public void mouseHover(MouseEvent arg0) {}
        	
        });
		
		options.setViewport(new Viewport(true));
		options.setScrollBarVisibility(FigureCanvas.NEVER);
        options.setBorder(new ABoxBorder());
        options.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

		lws.setContents(options.getViewport());
        bgBox = new Figure();
        bgBox.setLayoutManager(new FreeformLayout());
        bgBox.setBorder(new ABoxBorder());
        borderColors[0] = ColorConstants.black;
        bgBox.setBackgroundColor(ALayout.getBoxColor());
        bgBox.setOpaque(true);
		options.setContents(bgBox);
		
        options.addMouseListener(new MouseListener(){

			@Override
			public void mouseDoubleClick(MouseEvent arg0) {}

			@Override
			public void mouseDown(MouseEvent event) {
				if (event.button == 3){
					Menu menu = new Menu(options);
			        MenuItem replace = new MenuItem(menu, SWT.CASCADE);
			        replace.setText("Other color");
			        replace.addListener(SWT.Selection, new Listener(){

						@Override
						public void handleEvent(Event event) {
					        ColorDialog cd = new ColorDialog(shell);
					        cd.setText("Color");
					        cd.setRGB(new RGB(255, 255, 255));
					        RGB newColor = cd.open();
					        if (newColor == null) {
					          return;
					        }
					        bgBox.setBackgroundColor(new Color(display, newColor));
					     //   setActiveColor(new Color(display, newColor));
							
						}
			        	
			        });
			        options.setMenu(menu);
			        refresh();
				}
				
			}

			@Override
			public void mouseUp(MouseEvent arg0) {}
        	
        });
		
   /*     Button bgColor = new Button("Backgrund color");
        bgColor.setOpaque(false);
        bgColor.setLocation(new Point(10,15));
        bgColor.setSize(150,20);
        bgColor.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
		        ColorDialog cd = new ColorDialog(shell);
		        cd.setText("Color");
		        cd.setRGB(new RGB(255, 255, 255));
		        RGB newColor = cd.open();
		        if (newColor == null) {
		          return;
		        }
		        setActiveColor(new Color(display, newColor));

			}
        	
        });*/
  /*      borderColor = new Button("Border color");
        borderColor.setOpaque(false);
        borderColor.setEnabled(false);
        borderColor.setLocation(new Point(10,35));
        borderColor.setSize(150,20);
        borderColor.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
		        ColorDialog cd = new ColorDialog(shell);
		        cd.setText("Color");
		        cd.setRGB(new RGB(255, 255, 255));
		        RGB newColor = cd.open();
		        if (newColor == null) {
		          return;
		        }
		        borderColors[0] = new Color(display, newColor);
		        LineBorder border = new LineBorder(borderColors[0]);
		        border.setWidth(2);
		        bgBox.setBorder(border);
			}
        	
        });*/
        
        int idx = 1;
        for (final Color color: colorList){
        	Button c = new Button();
        	final Button b = new Button();
        	colorButtons.add(c);
        	borderMap.put(b, c);
        	c.setSize(16,16);
        	b.setSize(16,16);
        	LineBorder lb = new LineBorder();
        	lb.setColor(color);
        	lb.setWidth(2);
        	b.setBorder(lb);
        	c.setBackgroundColor(color);
        	c.setLocation(new Point(8 + (idx-1)*19, 10));
        	b.setLocation(new Point(8 + (idx-1)*19, 40));
        	c.setOpaque(true);
        	b.setOpaque(false);
        	b.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent event) {
					if (borderMap.containsKey(event.getSource())){
						Button b = (Button) event.getSource();
						Color c = borderMap.get(b).getBackgroundColor();
				        LineBorder border = new LineBorder(borderColors[0]);
				        border.setWidth(3);
				        bgBox.setBorder(border);
				        if (fill.getSelection()){
				        	setFillCursor(bgBox.getBackgroundColor(), borderColors[0], 14);
				        }
				        borderColors[0] = c;
					}					
				}
        		
        	});

        	c.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent arg0) {
					Color color = ((Figure) arg0.getSource()).getBackgroundColor();
			        bgBox.setBackgroundColor(color);
			        if (fill.getSelection()){
			        	setFillCursor(color, borderColors[0], 14);
			        }
					
				}
        		
        	});
        	bgBox.add(b);
        	bgBox.add(c);
        	idx++;
        	
        }
        bgBox.repaint();
        
        final Composite toolbar = new Composite(sash, SWT.BORDER);
        toolbar.setBackground(ColorConstants.white);
        
        GridLayout layout = new GridLayout();
        layout.numColumns=5;
        layout.makeColumnsEqualWidth = false;
        layout.horizontalSpacing = 10;
        layout.verticalSpacing = 10;
		toolbar.setLayout(layout);
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		java.awt.Font[] fonts = ge.getAllFonts();
		
		final Combo fontCombo = new Combo(toolbar, SWT.NONE);
		fontCombo.setToolTipText("Font");
		GridData d = new GridData();
		d.horizontalSpan = 2;
		fontCombo.setLayoutData(d);
		int index = 0; int arialindex = 1;
		for(java.awt.Font f: fonts){
			fontCombo.add(f.getFontName());
			if (f.getFontName().equals("Arial")){
				arialindex  = index;
			}
			index++;
		}
		fontCombo.select(arialindex);
		fontCombo.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				String item = fontCombo.getItem(fontCombo.getSelectionIndex());
				try{
					ALayout.font = item;
				} catch (Exception e){ e.printStackTrace();}
			}
			
		});
		
		final Combo textCombo = new Combo(toolbar, SWT.NONE);
		textCombo.setToolTipText("Text size");
		for (int i=4; i<32; i+=2){
			textCombo.add(Integer.toString(i));
		}
		textCombo.select(4);
		textCombo.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				String item = textCombo.getItem(textCombo.getSelectionIndex());
				try{
					ALayout.textSize = Integer.parseInt(item);
				} catch (Exception e){ e.printStackTrace();}
			}
			
		});
		
		final Combo sizeCombo = new Combo(toolbar, SWT.NONE);
		sizeCombo.setToolTipText("Border width");
		for (int i=1; i<20; i++){
			sizeCombo.add(Integer.toString(i));
		}
		sizeCombo.select(2);
		sizeCombo.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				String item = sizeCombo.getItem(sizeCombo.getSelectionIndex());
				try{
					ALayout.borderWidth = Integer.parseInt(item);
				} catch (Exception e){ e.printStackTrace();}
			}
			
		});
		
		org.eclipse.swt.widgets.Button button = new org.eclipse.swt.widgets.Button(toolbar, SWT.NONE);
        try{
    		Image image = AResourceLoader.getExpandImage();
    		button.setImage(image);
        } catch (Exception e){ e.printStackTrace(); }
		button.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				int height = shell.getSize().y;
				int percent =  20000/height;
				int difference = 5000/height;
				int weights[] = verticalSash.getWeights();
				double sum = weights[0] + weights[1] + weights[2] ;
				if (100*verticalSash.getWeights()[1]/sum > percent/2){
	        		verticalSash.setWeights(new int[] {percent/2, percent/2 - difference ,(100 + difference - percent)});
				}
				else verticalSash.setWeights(new int[] {percent/2, percent/2 + difference ,(100 - difference - percent)});
			}
			
		});
		
		Group figure = new Group(comp, SWT.NONE);
	    figure.setBackground(ColorConstants.white);
	    figure.setText("   Figure   ");
	    GridLayout gl = new GridLayout(2, true);
	    gl.makeColumnsEqualWidth = true;
	    gl.horizontalSpacing = 5;
	    figure.setLayout(new GridLayout(2, true));
		circle = new org.eclipse.swt.widgets.Button(figure, SWT.CHECK);
		circle.setToolTipText("For a circle hold CTRL");
		text = new org.eclipse.swt.widgets.Button(figure, SWT.CHECK);
		rectangle = new org.eclipse.swt.widgets.Button(figure, SWT.CHECK);
		rectangle.setToolTipText("For a square hold CTRL");
		fill = new org.eclipse.swt.widgets.Button(figure, SWT.CHECK);			
		
		Group g = new Group(toolbar, SWT.NONE);
		g.setText("Color brightness");
		g.setLayout(new GridLayout());
		g.setBackground(ColorConstants.white);
		final Scale colScale = new Scale(g, SWT.BORDER);
		colScale.setLayoutData(new GridData());
		colScale.setSelection((int) (ALayout.colorSValue*100));
		
		
		for (Button b: borderMap.keySet()){
			Button c = borderMap.get(b);
			Color bc = c.getBackgroundColor();
			Color nColor = getSValue(ALayout.colorSValue, bc.getRGB());
			c.setBackgroundColor(nColor);
			LineBorder border = new LineBorder(3);
			border.setColor(nColor);
			b.setBorder(border);
		}
		Color bgc = bgBox.getBackgroundColor();
		bgBox.setBackgroundColor(getSValue(ALayout.colorSValue, bgc.getRGB()));
		
		
		colScale.setMaximum(100);
		colScale.setMinimum(10);
		colScale.setPageIncrement(5);
		colScale.setBackground(ColorConstants.white);
		colScale.setToolTipText("Color brightness");
		
		colScale.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				if (colScale.getSelection() > 0.1){
					double newScale = ((double)(colScale.getSelection()))/100;
					for (AEntityBox box: AModelImpl.getBoxes()){
							Color c = box.getBackgroundColor();
							Color newColor = getSValue((float) newScale, c.getRGB());
							box.setBackgroundColor(newColor);
					}
					for (Button b: borderMap.keySet()){
						Button c = borderMap.get(b);
						Color bc = c.getBackgroundColor();
						Color nColor = getSValue((float) newScale, bc.getRGB());
						c.setBackgroundColor(nColor );
						LineBorder border = new LineBorder(3);
						border.setColor(nColor);
						b.setBorder(border);
					}
					Color bgc = bgBox.getBackgroundColor();
					bgBox.setBackgroundColor(getSValue((float) newScale, bgc.getRGB()));
					
					ALayout.colorSValue = (float) newScale; 
				}
				else colScale.setSelection(10);
				toolbar.layout();
				refresh();
			}
			
		});

		Label l = new Label(toolbar, SWT.NONE);
		GridData data = new GridData();
		data.horizontalSpan = 4;
		l.setLayoutData(data);
		l.setImage(AGREE_LOGO_BIG);
		l.setBounds(AGREE_LOGO_BIG.getBounds());

		circle.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				if (circle.getSelection()){
					setCursorMode(CursorMode.CIRCLE);
				}
				else {
					setCursorMode(CursorMode.NULL);
				}

			}
			
		});
		rectangle.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				if (rectangle.getSelection()){
					setCursorMode(CursorMode.RECTANGLE);
				}
				else {
					setCursorMode(CursorMode.NULL);				
				}

			}
			
		});
		
		text.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				if (text.getSelection()){
					setCursorMode(CursorMode.TEXTBOX);
				} else {
					setCursorMode(CursorMode.NULL);
				}
			}
			
		});
		fill.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				try {
					if (fill.getSelection()){
						AState.setCursorMode(CursorMode.FILL);
					}
					else {
						AState.setCursorMode(CursorMode.NULL);
					}
				} catch(AWrongFormatException e){
					displayErrorMessage(e);
				}
			}
			
		});
		circle.setText("Ellipse"); circle.setBackground(ColorConstants.white); 
		
		MouseTrackListener listener = new MouseTrackListener() {
            @Override
            public void mouseHover(final MouseEvent e) {
               
            }

            @Override
            public void mouseExit(final MouseEvent e) {
            	shell.setCursor(display.getSystemCursor(SWT.CURSOR_ARROW));
            }

            @Override
            public void mouseEnter(final MouseEvent e) {
            	switch (AState.getCursorMode()){
            	case FILL:
					setFillCursor(bgBox.getBackgroundColor(), borderColors[0], 14);
            		break;
            	case CIRCLE: case RECTANGLE:
                	shell.setCursor(display.getSystemCursor(SWT.CURSOR_CROSS));
                	break;
                case TEXTBOX:
                	shell.setCursor(display.getSystemCursor(SWT.CURSOR_IBEAM));
                	break;
				default:
					break;
            	}
            }
        };
        
        canvas.addMouseTrackListener(listener);
		
		rectangle.setText("Rectangle"); rectangle.setBackground(ColorConstants.white); 
		text.setText("Text"); text.setBackground(ColorConstants.white);
		fill.setText("Fill"); fill.setBackground(ColorConstants.white);
	    
	    toolbar.layout();
		
	}

	protected void setCursorMode(CursorMode cm) {
		try {
			AState.setCursorMode(cm);
		} catch (AWrongFormatException e) {
			displayErrorMessage(e);
		}
	}

	private void addCanvas() {
        sash = new SashForm(compA, SWT.HORIZONTAL);
        sash.addListener (SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                sash.setBounds (e.x, e.y, e.width, e.height);
            }
        });
        sash.setSize(compA.getBorderWidth(), 1000);
        
        compB = new Composite(sash, SWT.NONE);
        compB.setLayout(new FillLayout());

        canvas = new FigureCanvas(sash, SWT.H_SCROLL | SWT.V_SCROLL);
        canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        canvas.setSize(100, 100);
        
        LightweightSystem lws = new LightweightSystem(canvas);
		contentsFigure = new AContentsFigure();
		
		canvas.setViewport(new Viewport(true));
		canvas.setScrollBarVisibility(FigureCanvas.ALWAYS);

		contentsFigure.setScale(1);
		contentsFigure.setLayoutManager(new FreeformLayout());
	    contentsFigure.setBackgroundColor(ColorConstants.white);
	    contentsFigure.setOpaque(true);

		lws.setContents(canvas.getViewport());
		canvas.setContents(contentsFigure);
		
		contents = new Figure();
		contents.setBounds(new Rectangle(-10000,-10000,1000000,1000000));	//FIXME
		contents.setLayoutManager(new FreeformLayout());
	    contents.setOpaque(false);
	    
	    backgroundFigures.add(contents);
	    
	    paintContents = new Figure();
	    paintContents.setBounds(new Rectangle(-10000,-10000,1000000,1000000));
		paintContents.setLayoutManager(new FreeformLayout());
	    paintContents.setBackgroundColor(ColorConstants.white);
	    paintContents.setOpaque(false);

	    backgroundFigures.add(paintContents);

	    contentsFigure.add(paintContents);
	    contentsFigure.add(contents);
	    
	    contentsFigure.repaint();
	    
	    contents.repaint();  
	    int width = shell.getSize().x;
	    int vertical = 41000/width;
	    sash.setWeights(new int[] { vertical, 100-vertical});
		
	}

	private void addTextArea() {
        SashForm sash = new SashForm(shell, SWT.VERTICAL);
        textArea = new StyledText(sash, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        textArea.addMouseListener(new MouseListener(){

			@Override
			public void mouseDoubleClick(MouseEvent e) {}

			@Override
			public void mouseDown(MouseEvent e) {
				if (AState.hasChanged)
						updateText();
				
			}

			@Override
			public void mouseUp(MouseEvent e) {}

        });
        textArea.addModifyListener(new ModifyListener(){

			@Override
			public void modifyText(ModifyEvent e) {
				textModified = true;
			}
        });

        compA = new Composite(sash, SWT.NONE);
        compA.setLayout(new FillLayout());
        
        Composite sb = new Composite(sash, SWT.NONE);
        sb.setLayout(new FillLayout());
        manager = new StatusLineManager();
        manager.createControl(sb);
        
        sash.setWeights(new int[] { 5, 97, 3});
	}

	public void updateText() {
		ASdaiHandler.saveModel(AState.getTempDir().concat("tmp_step"));
		setText(AState.getTempDir().concat("tmp_step.stp"));
		AState.hasChanged = false;
	}

	private void createMenuItems() {
		Menu menuBar = new Menu(shell, SWT.BAR);
        MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuHeader.setText("File");
        
        MenuItem editMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        editMenuHeader.setText("Edit");
        
        Menu editMenu = new Menu(shell, SWT.DROP_DOWN);
        editMenuHeader.setMenu(editMenu);
        
        
        undoItem = new MenuItem(editMenu, SWT.PUSH);
        undoItem.setText("Undo\t\tCTRL+Z");
        undoItem.setEnabled(false);
        undoItem.addListener(SWT.Selection, new Listener(){
        	
        	@Override
			public void handleEvent(Event arg) {
        		setChanged();
        		notifyObservers(Action.UNDO);
			}
        	
        });
        
        redoItem = new MenuItem(editMenu, SWT.PUSH);
        redoItem.setText("Redo\t\tCTRL+Y");
        redoItem.setEnabled(false);
        redoItem.addListener(SWT.Selection, new Listener(){
        	
        	@Override
			public void handleEvent(Event arg) {
        		setChanged();
        		notifyObservers(Action.REDO);
			}
        	
        });
        
        
        MenuItem viewMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        viewMenuHeader.setText("View");
        
        MenuItem textMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        textMenuHeader.setText("Text");
        
     /*   MenuItem templateMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        templateMenuHeader.setText("Template");
        
        Menu templateMenu = new Menu(shell, SWT.DROP_DOWN);
        templateMenuHeader.setMenu(templateMenu);
        
        MenuItem createTemplateItem = new MenuItem(templateMenu, SWT.PUSH);
        createTemplateItem.setText("Create N/A");
        createTemplateItem.setEnabled(false);
               
        MenuItem initializeTemplateItem = new MenuItem(templateMenu, SWT.PUSH);
        initializeTemplateItem.setText("Use N/A");
        initializeTemplateItem.setEnabled(false);	*/
        
        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("Help");
        
        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);
        
        MenuItem updates = new MenuItem(helpMenu, SWT.PUSH);
        updates.setText("Updates");
        updates.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				try {
					File file = AResourceLoader.getFile("updates.txt");
					if (Desktop.isDesktopSupported()) {
					    try {
							Desktop.getDesktop().open(file);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e1) {e1.printStackTrace();}
				
			}
        });
        
        MenuItem helpItem = new MenuItem(helpMenu, SWT.PUSH);
        helpItem.setText("Help");
        helpItem.addListener(SWT.Selection, new Listener(){
        	
        	@Override
			public void handleEvent(Event arg) {
                if (Desktop.isDesktopSupported()) {

                	try {
                    	File file = AResourceLoader.getPdfFile("user_manual.pdf");
						Desktop.getDesktop().open(file);
					} catch (IOException e) {
						e.printStackTrace();
					}

                }
        	}
        	
        });
        
        MenuItem helpAbout = new MenuItem(helpMenu, SWT.PUSH);
        helpAbout.setText("About");
        helpAbout.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				new AAbout(display, AGREE_LOGO_SMALL, self);
			}
        	
        });
        
        Menu textMenu = new Menu(shell, SWT.DROP_DOWN);
        textMenuHeader.setMenu(textMenu);
        
        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        fileMenuHeader.setMenu(fileMenu);
       
        Menu viewMenu = new Menu(shell, SWT.DROP_DOWN);
        viewMenuHeader.setMenu(viewMenu);
        
        final MenuItem showOnlyUsed = new MenuItem(viewMenu, SWT.CHECK);
        showOnlyUsed.setText("Show only used entities");
        showOnlyUsed.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg) {
   			  if (showOnlyUsed.getSelection()){
   				  showOnlyUsed();
   			  }
 			  else showAll();
			}
        	
        });
        
        final MenuItem showAbstractEntities = new MenuItem(viewMenu, SWT.CHECK);
        showAbstractEntities.setText("Show abstract entities");
        showAbstractEntities.setSelection(true);
        showAbstractEntities.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg) {
   			  if (showAbstractEntities.getSelection()){
   				  showAllAbstract();
   			  }
 			  else hideAbstractEntities();
			}
        	
        });
        
        
        
        showSideMenu = new MenuItem(viewMenu, SWT.CHECK);
        showSideMenu.setText("Show side menu\t\t\tCTRL+M");
        showSideMenu.setSelection(true);
        showSideMenu.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg) {
				showSideMenu(showSideMenu.getSelection());
			}
        	
        });
        
     /*   final MenuItem showPersistentLabel = new MenuItem(viewMenu, SWT.CHECK);
        showPersistentLabel.setText("Show persistent label");
        showPersistentLabel.setSelection(true);
        showPersistentLabel.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				if (showPersistentLabel.getSelection()){
					AState.labelType = LabelType.PERSISTENT;
					for (AEntityBox box: AModelImpl.getBoxes()){
						box.persistentName();
					}
				}
				else {
					AState.labelType = LabelType.BASIC;
					for (AEntityBox box: AModelImpl.getBoxes()){
						box.basicName();
					}
				}
			}
        	
        });*/
        
        final MenuItem showBendpoints = new MenuItem(viewMenu, SWT.CHECK);
        showBendpoints.setText("Show bendpoints");
        showBendpoints.setSelection(true);
        
        final MenuItem showButtonBorder = new MenuItem(viewMenu, SWT.PUSH);
        showButtonBorder.setText("Box layout");
        showButtonBorder.setSelection(true);
        showButtonBorder.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
			/*	for (AEntityBox box : AModelImpl.getBoxes())
						box.setButtonVisibility(showButtonBorder.getSelection());
				
				AState.visibleButtonBorder = showButtonBorder.getSelection(); */
					new AEditBoxLayout(display, AGREE_LOGO_SMALL, getBoxFont());
				}
        	
        });

        
        MenuItem connectionType = new MenuItem(viewMenu, SWT.CASCADE);
        connectionType.setText("Connection type");
        Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
        connectionType.setMenu(subMenu);
        
        final MenuItem basic = new MenuItem(subMenu, SWT.RADIO);
        basic.setText("Basic");
        basic.setSelection(true);
        basic.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				if (basic.getSelection()){
					AState.connection = AState.Connection.BENDPOINT;
					setConnectionRouter(new BendpointConnectionRouter());
				}
			}
        	
        });
        
        final MenuItem manhattan = new MenuItem(subMenu, SWT.RADIO);
        manhattan.setText("Manhattan");
        manhattan.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				if (manhattan.getSelection()){
					AState.connection = AState.Connection.MANHATTAN;
					setConnectionRouter(new ManhattanConnectionRouter());
				}
			}
        	
        });
        
        new MenuItem(viewMenu, SWT.SEPARATOR);
        
        MenuItem fileNewItem = new MenuItem(fileMenu, SWT.PUSH);
        fileNewItem.setText("New");
        fileNewItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				saveOption();
				AState.filePath = null;
				treeViewer.refresh();
				shell.setText(NAME);
			}
        	
        });
        
        MenuItem fileOpenGizItem = new MenuItem(fileMenu, SWT.PUSH);
        fileOpenGizItem.setText("Open");
        fileOpenGizItem.addListener(SWT.Selection,  new Listener(){

			@Override
			public void handleEvent(Event event) {
				openGiz();
				fileSave.setEnabled(true);
				fileSaveItem.setEnabled(true);
			}
        	
        });
        
        fileSave = new MenuItem(fileMenu, SWT.CASCADE);
        fileSave.setText("Save\t\tCTRL+S");
        fileSave.setEnabled(false);
        fileSave.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				save();
			}
        	
        });
        
        fileSaveItem = new MenuItem(fileMenu, SWT.PUSH);
        fileSaveItem.setText("Save As...");
        fileSaveItem.setEnabled(false);
        fileSaveItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				saveAs();
			}
        	
        });
        new MenuItem(fileMenu, SWT.SEPARATOR);
        
        MenuItem clearItem = new MenuItem(fileMenu, SWT.PUSH);
        clearItem.setText("Clear");
        clearItem.setEnabled(true);
        clearItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				hideAll();
			}
        	
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem fileSearchTextItem = new MenuItem(textMenu, SWT.PUSH);
        fileSearchTextItem.setText("Search");
        fileSearchTextItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				new ATextSearch(display, AGREE_LOGO_SMALL, self); 
			}
        	
        });
        
        new MenuItem(textMenu, SWT.SEPARATOR);
        
        MenuItem reorderText = new MenuItem(textMenu, SWT.PUSH);
        reorderText.setText("Text editor");
        reorderText.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				new ATextEditor(display, AGREE_LOGO_SMALL, self);
			}
        	
        });

        Menu subSchemaMenu = new Menu(shell, SWT.DROP_DOWN);
        
        MenuItem importSchemaItem = new MenuItem(fileMenu, SWT.CASCADE);
        importSchemaItem.setText("Load schema");
        importSchemaItem.setMenu(subSchemaMenu);

        
        MenuItem importSTEPItem = new MenuItem(fileMenu, SWT.PUSH);
        importSTEPItem.setText("Import STEP");
        importSTEPItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
     	        fd.setText("Open");
     	        String[] filterExt = { "*.stp", "*.p21", "*.*" };
     	        fd.setFilterExtensions(filterExt);
     	        String selected = fd.open();  
     	        
    	        if (selected != null){
    	        	saveOption();
        	        openStepFile(selected);
        	        shell.setText(NAME);
    	        }
				
			}
        	
        });
        
        MenuItem aps = new MenuItem(subSchemaMenu, SWT.CASCADE);
        aps.setText("AP's");
        
        final MenuItem all = new MenuItem(subSchemaMenu, SWT.PUSH);
        all.setText("All schemas");
        all.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				new AAllSchemas(display, AGREE_LOGO_SMALL, self);
				fileSave.setEnabled(true);
				fileSaveItem.setEnabled(true);
			}
        	
        });
        
        final MenuItem fromJar = new MenuItem(subSchemaMenu, SWT.PUSH);
        fromJar.setText("..from jar");
        fromJar.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				
				FileDialog fd = new FileDialog(shell, SWT.OPEN);
			    fd.setText("Load schema (jar)");
			    String[] filterExt = { "*.jar"};
			    fd.setFilterExtensions(filterExt);
			    final String path = fd.open();
			    
			    if (path != null){
				    try {
						String schemaName = ASchema.addDynamicSchema(path);
						openSchema(schemaName);
						fileSave.setEnabled(true);
						fileSaveItem.setEnabled(true);
						
					} catch (IOException e) {
						e.printStackTrace();
					}
			    }
			}
        	
        });
        
        Menu apMenu = new Menu(shell, SWT.DROP_DOWN);
        aps.setMenu(apMenu);
        
        for (String s: ASchema.getApSchemas()){
            final MenuItem apschema = new MenuItem(apMenu, SWT.PUSH);
            apschema.setText(s);
            apschema.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event arg0) {

					final String schemaName = apschema.getText(); 
					openSchema(schemaName);
					fileSave.setEnabled(true);
					fileSaveItem.setEnabled(true);
				}
            	
            });
            	
         }
        
        
        
        MenuItem viewFitView = new MenuItem(viewMenu, SWT.PUSH);
        viewFitView.setText("Fit view\t\tCTRL+F");
        viewFitView.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				fitView();
			}
        	
        });
        
        MenuItem fileMergeItem = new MenuItem(fileMenu, SWT.PUSH);
        fileMergeItem.setText("Merge with...");
        fileMergeItem.setEnabled(false);
        
        MenuItem s12 = new MenuItem(fileMenu, SWT.SEPARATOR);
        
        MenuItem filePrintItem = new MenuItem(fileMenu, SWT.PUSH);
        filePrintItem.setText("Print");
        filePrintItem.setEnabled(true);
        filePrintItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
    			 MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
       	         messageBox.setMessage("Do you wish to fit the diagram onto one page?");
     	         messageBox.setText("Print option");
     	         int response = messageBox.open();
     			 PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
     			 PrinterData data = dialog.open();
     			 if (data != null) {
     				 PrintFigureOperation printOperation = new PrintFigureOperation(new Printer(data), contentsFigure);
         	         if (response == SWT.YES)	{
         	        	 printOperation.setPrintMode(PrintFigureOperation.FIT_PAGE);
         	         } 
         	         else {
         	        	 printOperation.setPrintMode(PrintFigureOperation.TILE); 
         	         }
     				 printOperation.run(NAME);
     			 } 
				
			}
        	
        });
        
        MenuItem fileExportData = new MenuItem(fileMenu, SWT.PUSH);
        fileExportData.setText("Export data");
        fileExportData.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
       			FileDialog fd = new FileDialog(shell, SWT.SAVE);
     	        fd.setText("Save as..");
     	        String[] filterExt = {"*.stp"};
     	        fd.setFilterExtensions(filterExt);
     	        String selected = fd.open();
     	        if (selected != null){
        			ASdaiHandler.saveModel(selected.replace(".stp", ""));
     	        }
     	        displayInfoMessage("Success", "Export completed.");
			}
        	
        });
        fileExportData.setEnabled(true);

        MenuItem fileConvertToSVG = new MenuItem(fileMenu, SWT.PUSH);
        fileConvertToSVG.setText("Export graphics (*.SVG)");
        fileConvertToSVG.setEnabled(true);
        fileConvertToSVG.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
			    	double oldScale = contentsFigure.getScale();
     			    Point point = ASVGExporter.fitPrintView(contentsFigure, backgroundFigures);
     			    
     			    contentsFigure.repaint();
     			    contents.repaint();
    			
         			FileDialog fd = new FileDialog(shell, SWT.SAVE);
         	        fd.setText("Save as");
         	        String[] filterExt = {"*.svg"};
         	        fd.setFilterExtensions(filterExt);
         	        String selected = fd.open();
        			
         	        if (selected != null){
         	           ASVGExporter.exportToSVG(contentsFigure, selected, backgroundFigures);
              	       if (Desktop.isDesktopSupported()) {
            	    	    try {
            	    	        File svg = new File(selected);
            	    	        Desktop.getDesktop().open(svg);
            	    	    } catch (Exception ex) { ex.printStackTrace();
            	    	    }
            	    	}
         	        }
         	        
         	        contents.setBounds(contents.getBounds().getTranslated(point.x, point.y));
         	        paintContents.setBounds(paintContents.getBounds().getTranslated(point.x, point.y));
         	        contentsFigure.setScale(oldScale);
			}
        	
        });

        
        MenuItem fileConvertToPDF = new MenuItem(fileMenu, SWT.PUSH);
        fileConvertToPDF.setText("Export graphics (*.PDF)");
        fileConvertToPDF.setEnabled(true);
        fileConvertToPDF.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
		        fitView();
				FileDialog fd = new FileDialog(shell, SWT.SAVE);
				fd.setText("Save as");
		        String[] filterExt = {"*.pdf"};
		        fd.setFilterExtensions(filterExt);
		        String selected = fd.open();
		        try{
		        	ASVGExporter.convertToPDF(contentsFigure, selected, backgroundFigures);
		        } catch (Exception e){
		        	displayErrorMessage(new AException("Invalid byte 1 of 1-byte UTF-8 sequence", "Attribute values probably contain bad characters.", 1));
		        }
			}
        	
        });
        
        new MenuItem(fileMenu, SWT.SEPARATOR);
        
        MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
        fileExitItem.setText("Exit");
        fileExitItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				saveOption();
				shell.dispose();
			}
        	
        });
        
        shell.setMenuBar(menuBar);

	}


	public void openSchema(final String schemaName) {
		Runnable runnable = new Runnable(){

			@Override
			public void run() {
				ASdaiHandler.newRepository(schemaName);
				
				twn = ASchemaParser.parseLoadedSchema(schemaName);
				if (twn == null){
					twn = ASchemaParser.parseSchema(schemaName);
				}
				working = false;
			}


			
		};
		saveOption();
		shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
		
		working = true;
		new Thread(runnable).start();
		setStatusMessage("Loading schema...");
		while (working){
			displayProgress();
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		shell.setCursor(display.getSystemCursor(SWT.CURSOR_ARROW));

		treeViewer.setInput(twn);
		treeViewer.expandToLevel(2);
		treeViewer.refresh();
		setStatusMessage("Schema ".concat(schemaName).concat(" loaded"));

	}

	public void clean() {
		contents.removeAll();
		paintContents.removeAll();
		textArea.setText("");
		AModelImpl.clearFileInfo();
		setChanged();
		notifyObservers(null);
		if (ASchema.getSchema() != null)
			ASdaiHandler.newRepository(ASchema.getSchema());
	}

	/**
	 * Opens a step file from path @param selected.
	 * 
	 * @param selected
	 */
	public void openStepFile(String selected) {
		try {
			ASdaiHandler.open(selected);
	    	if (! ASdaiHandler.hasCorrectSchema()){
				twn = ASchemaParser.parseLoadedSchema(ASdaiHandler.getSchema());
				if (twn == null){
					twn = ASchemaParser.parseSchema(ASdaiHandler.getSchema());
					ASdaiHandler.open(selected); // If schema is parsed... entities created that need to be deleted..
				}
	    	}
			ASTEPParser.parseSTEP(model);
			
			setText(selected);
			treeViewer.setInput(twn);
			treeViewer.expandToLevel(2);
			treeViewer.refresh();
			
			fileSave.setEnabled(true);
			fileSaveItem.setEnabled(true);
			
		} catch (SdaiException e){ 
			e.printStackTrace(); 
		} 
		catch (AUnrecognizedSchemaException e){
			displayErrorMessage(e);
			return;
		}
		catch (AMissingEntitiesException e) {
			displayErrorMessage(e);
		}
		displayInfoMessage("Success", "STEP-file successfully imported!");
 	    treeViewer.refresh();
	}

	private void openGiz() {
		String stepFile = ""; String xmlFile = "";
		
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
	    fd.setText("Open");
	    String[] filterExt = { "*.giz", "*.*" };
	    fd.setFilterExtensions(filterExt);
	    final String selected = fd.open();
	    PrintWriter writer = null;
	    boolean errors = false;
	    if (selected != null){
 			AState.filePath = selected;
 			String path = selected.replaceAll("[a-zA-Z_0-9\\s]+\\.giz", "");
	       	try {
		    	writer = new PrintWriter(selected.concat("error_log.txt"), "UTF-8");

				if (!new File(path).canWrite()){
					displayErrorMessage(new AException("Unable to write in directory: ".concat(path).concat(".\n Permission denied."), "", 1)); //System.out.println("CANNOT WRITE IN DIRECTORY: " + AState.filePath.replaceAll("[a-z]+\\.giz", ""));
					writer.println("Write access denied.");
					writer.println();
					errors = true;
				}
				else {
					writer.println("Write access OK.");
					writer.println();
				}
				if (!new File(path).canRead()){
					displayErrorMessage(new AException("Unable to read in directory: ".concat(path).concat(".\n Permission denied."), "", 1)); //System.out.println("CANNOT WRITE IN DIRECTORY: " + AState.filePath.replaceAll("[a-z]+\\.giz", ""));
					writer.println("Read access denied.");
					writer.println();
					errors = true;
				}
				else {
					writer.println("Read access OK.");
					writer.println();
				}

				ZipFile gizFile = new ZipFile(selected);
				writer.println("Zip file successfully opened");
				Enumeration<? extends ZipEntry> entries = gizFile.entries();
				while(entries.hasMoreElements()) {
				        ZipEntry entry = entries.nextElement();
			        	Util.copyInputStream(gizFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(AState.getTempDir().concat(entry.getName()))));
				        if (entry.getName().endsWith(".stp")){
				        	stepFile = AState.getTempDir().concat(entry.getName());
				        	writer.println("STEP file found: " + stepFile);
				        }
				        else if (entry.toString().endsWith(".xml")){
				        	xmlFile = AState.getTempDir().concat(entry.getName());
				        	writer.println("Graphics file found: " + xmlFile);
				        }
				 /*       else if (entry.toString().endsWith(".sn")){
				        	String snFile = entry.getName();
				        	model.readShortNames(snFile, true);
				        }*/
				}
				gizFile.close();
				final String s = stepFile;

				saveOption();
				openStepFile(s);

			
				try {
					AXMLParser.readXML(new File(xmlFile), this);
				} catch (AEntitiesNotFoundException e) {
					writer.println("Error parsing graphics file! ");
					e.printStackTrace();
					e.printStackTrace(writer);
					errors = true;
//					writer.close();
				}
				
				shell.setText(NAME.concat(" [").concat(selected.replace(".xml", ".stp")).concat("]"));
					
					
		//		setStatusMessage("Schema loaded, creating graphics... ");
				treeViewer.setInput(twn);	
				shell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));      					        		
				treeViewer.expandToLevel(2);
				treeViewer.refresh();    					        		
	        	shell.setCursor(display.getSystemCursor(SWT.CURSOR_ARROW));    					        		
	        	treeViewer.refresh();
	        	fitView();
	        	
	        	setStatusMessage("File loaded");
	        	fitView();

	       	} catch (IOException e) {
				System.out.println("Zip file could not be opened");
				writer.println("Zip file could not be opened");
				errors = true;
//				writer.close();
	       		MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
	       		box.setMessage("Error loading file!");
	       		box.setText("ERROR");
	       		box.open();
	       	}
		    writer.close();
		    if (!errors)
		    	new File(selected.concat("error_log.txt")).delete();  	
	    }
	    
	}

	public void setModel(AModelImpl model){
		this.model = model;
	}
	
	protected void saveOption() {
		if (AModelImpl.getBoxes().size() > 0 && 
				displayYesNoMessage("Unsaved changes.", "Any unsaved changed will be lost. "
						+ "Do you wish to save the current file?")){
    		save();
    		clean();
    	}
    	else clean();
		
	}
	
	/**
	 * 
	 * @param selected
	 * @return
	 */
	private boolean setText(String selected) {
	    try {
	        StringBuilder string = new StringBuilder();
			BufferedReader reader = new BufferedReader(new FileReader(selected));
			String line = null;
			while (( line = reader.readLine()) != null){
			      string.append(line);
			      string.append(System.getProperty("line.separator"));
			}
		    reader.close();
	        textArea.setText(string.toString());
	        textArea.redraw();
	        textModified = false;
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		return true;
	}
	
	private void hideAbstractEntities(){
		treeViewer.addFilter(new AAbstractFilter());
		treeViewer.refresh();
	}
	
	private void showAllAbstract(){
		for (ViewerFilter filter: treeViewer.getFilters()){
			if (filter instanceof AAbstractFilter)
				treeViewer.removeFilter(filter);
		}
		treeViewer.refresh();
	}
	
	private void showAll(){
		for (ViewerFilter filter: treeViewer.getFilters()){
			if (filter instanceof AUsedOnlyFilter)
				treeViewer.removeFilter(filter);
		}
		treeViewer.refresh();
	}
	
	private void showOnlyUsed() {
		treeViewer.addFilter(new AUsedOnlyFilter());
		treeViewer.refresh();
	}
	
	public FigureCanvas getCanvas(){
		return canvas;
	}
	
	public Figure getContents(){
		return contents;
	}
	
	public AContentsFigure getContentsFigure(){
		return contentsFigure;
	}
	
	public void refresh() {
		 treeViewer.refresh();
		 canvas.update();
	}
	
	@Override
	public void update(Observable o, Object arg) {
		treeViewer.refresh();
		if (arg instanceof AException){
			displayErrorMessage((AException) arg);
		}
		if (arg instanceof AEntityBox){
			AEntityBox box = ((AEntityBox) arg);
			if (box.getParent() == null){
				contents.add(box);	
				box.setSize(box.getPreferredSize().width + 20, box.getPreferredSize().height + 20);
				box.setLocation(box.getNextLocation().getTranslated(-box.getSize().width/2, -box.getSize().height/2));
				box.setBackgroundColor(bgBox.getBackgroundColor());
				contents.repaint();
			}
			shell.redraw();
		}
	}


	public void display() {
        shell.open();
		shell.layout();
		shell.redraw();
		display.update();
		
        while (!shell.isDisposed()) {
        	try {
	            if (!display.readAndDispatch()) {
	              display.sleep();
	            }
        	} catch (SWTException e){
        		e.printStackTrace();
        	}
        }
		
	}

	public Font getBoxFont() {
		return new Font(display,"Arial",9,SWT.BOLD);
	}
	
	public Font getTextFont(int size) {
		try {
			return new Font(display, ALayout.font, size ,SWT.BOLD);
		} catch (Exception e){e.printStackTrace();}
		return new Font(display, "Arial", size ,SWT.BOLD);
	}

	public boolean textIsModified(){
		return textModified;
	}
	
	public void zoom(Event event) {
		double scale = contentsFigure.getScale();
		if (event.count < 0){		//Zoom in
			if (scale > 0.10){
				contentsFigure.setScale(scale*0.9); 
				Point movement = new Point(0.1*event.x/scale, 0.1*event.y/scale);
				contents.setBounds(contents.getBounds().getTranslated(movement));
				paintContents.setBounds(paintContents.getBounds().getTranslated(movement));
			}
		}
		else if (scale < 1){	//Zoom out
				contentsFigure.setScale(scale/0.9); 
				scale = contentsFigure.getScale();
				Point movement = new Point(-0.1*event.x/scale, -0.1*event.y/scale);
				contents.setBounds(contents.getBounds().getTranslated(movement));
				paintContents.setBounds(paintContents.getBounds().getTranslated(movement));
		}
		contentsFigure.repaint();
	}

	public Shell getShell() {
		return shell;
		
	}
	
	public void highlightText(String str) {
        String s = textArea.getText();
        if (s.contains(str.concat("="))){
            textArea.setSelection(s.indexOf(str.concat("=")), s.indexOf(';', s.indexOf(str.concat("="))));
        }
        else if (s.contains(str.concat(" ="))){
            textArea.setSelection(s.indexOf(str.concat(" =")), s.indexOf(';', s.indexOf(str.concat(" ="))));
        }
        textArea.showSelection();
	}

	public void addToContents(Figure figure) {
		contents.add(figure);
		
	}
	
	public void relocateBox(AEntityBox box, AEntityBox parent) {
		box.setSize((box.getPreferredSize().width + 20), (box.getPreferredSize().height));
		int width = (parent.getBounds().width - box.getBounds().width)/2;
		box.setLocation(parent.getLocation().getTranslated(width, parent.getSize().height + 40));
	//	fixAggregateConnections(box, parent);		//TODO: 
	}
	
	public void displayShapeMenu(final Figure f) {
		Menu menu = new Menu(canvas);
		final MenuItem deleteItem = new MenuItem(menu, SWT.CASCADE);
		deleteItem.setText("Delete");
		deleteItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				try {
					paintContents.remove(f);
				} catch (Exception e) {e.printStackTrace();}
			}
			
		});
		if (!(f instanceof ATextBox)){
			MenuItem borderWidthItem = new MenuItem(menu, SWT.CASCADE);
			borderWidthItem.setText("Border width");
			Menu widthMenu = new Menu(shell, SWT.DROP_DOWN);
			borderWidthItem.setMenu(widthMenu);
			int cwidth = ((Shape) f).getLineWidth();
			
			for (int i = 1; i < 16; i++){
				MenuItem widthItem = new MenuItem(widthMenu, SWT.CHECK);
				widthItem.setText(Integer.toString(i));
				final int w = i;
				if (i == cwidth)
					widthItem.setSelection(true);
				widthItem.addListener(SWT.Selection, new Listener(){

					@Override
					public void handleEvent(Event arg0) {
						((Shape) f).setLineWidth(w);
					}
					
				});
			}
		}
		
		setCursorMode(CursorMode.MENU);
		canvas.setMenu(menu);
		canvas.layout();
		
	}
	
	/**
	 * Display the right-click menu for a group of boxes
	 */
	public void displayGroupMenu() {
		if (AState.getActiveObject() instanceof LinkedList<?>){
			final LinkedList<Figure> figures = (LinkedList<Figure>) AState.getActiveObject();
			
			Menu menu = new Menu(canvas);
			
			MenuItem alineItem = new MenuItem(menu, SWT.CASCADE);
			alineItem.setText("Align");
			
			Menu subMenuB = new Menu(shell, SWT.DROP_DOWN);
			alineItem.setMenu(subMenuB);
			
			MenuItem horizontal = new MenuItem(subMenuB, SWT.CASCADE);
			horizontal.setText("Horizontally");
			Menu subMenuC = new Menu(shell, SWT.DROP_DOWN);
			horizontal.setMenu(subMenuC);
			MenuItem horizontalTop = new MenuItem(subMenuC, SWT.CASCADE);
			horizontalTop.setText("Top");
			MenuItem horizontalCenter = new MenuItem(subMenuC, SWT.CASCADE);
			horizontalCenter.setText("Center");
			MenuItem horizontalBottom = new MenuItem(subMenuC, SWT.CASCADE);
			horizontalBottom.setText("Bottom");
			
			horizontalTop.addListener(SWT.Selection, new Listener(){
				int hmin = Integer.MAX_VALUE;
				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					for (Figure box: figures){
						if (box.getLocation().y < hmin)
							hmin = box.getLocation().y;
					}
					for (Figure ff: figures){
			//			contents.add((Figure)ff);
						if (ff instanceof AEntityBox){
							AEntityBox eb = (AEntityBox) ff;
							AMoveAction moveAction = new AMoveAction(eb, eb.getBounds());
							((AEntityBox) ff).setLocation(new Point(((AEntityBox) ff).getLocation().x, hmin));	
							action.addAction(moveAction);
						}
					}
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
//					AState.setCursorMode(CursorMode.NULL);
				}
			});
			horizontalBottom.addListener(SWT.Selection, new Listener(){
				int hmax = Integer.MIN_VALUE;
				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					for (Figure box: figures){
						if (box.getLocation().y + box.getSize().height > hmax)
							hmax = box.getLocation().y + box.getSize().height;
						
					}
					for (Figure f: figures){
				//		contents.add((Figure)f);
						if (f instanceof AEntityBox){
							AEntityBox eb = (AEntityBox) f;
							AMoveAction moveAction = new AMoveAction(eb, eb.getBounds());
							eb.setLocation(new Point(((AEntityBox) f).getLocation().x, hmax - ((AEntityBox) f).getSize().height));	
							action.addAction(moveAction);
						}
					}
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
//					AState.setCursorMode(CursorMode.NULL);
				}
			});
			horizontalCenter.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					int hmax = Integer.MIN_VALUE, hmin = Integer.MAX_VALUE;
					for (Figure box: figures){
						if (box.getLocation().y + box.getSize().height > hmax)
							hmax = box.getLocation().y + box.getSize().height;
						if (box.getLocation().y < hmin)
							hmin = box.getLocation().y;
					}
					int center = hmin + (hmax-hmin)/2;
					
					for (Figure b: figures){
						AMoveAction moveAction = new AMoveAction(b, b.getBounds());
						b.setLocation(new Point(b.getBounds().x, center - b.getSize().height/2));	
						action.addAction(moveAction);
					}
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
//					AState.setCursorMode(CursorMode.NULL);
				}
			});
			
			
			MenuItem vertical = new MenuItem(subMenuB, SWT.CASCADE);
			vertical.setText("Vertically");
			
			MenuItem equalWidth = new MenuItem(subMenuB, SWT.CASCADE);
			equalWidth.setText("Equal width");
			equalWidth.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					int width =  0;
					for (Figure f: figures)
						width += f.getSize().width;
					width /= figures.size();
					
					for (Figure f: figures){
						if (f instanceof AEntityBox){
							AEntityBox eb = (AEntityBox) f;
							AMoveAction moveAction = new AMoveAction(eb, eb.getBounds());
							eb.setSize(width, eb.getSize().height);
							action.addAction(moveAction);
						}
					}
					
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
				}
				
			});
			
			MenuItem equalHeight = new MenuItem(subMenuB, SWT.CASCADE);
			equalHeight.setText("Equal height");
			equalHeight.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					int height =  0;
					for (Figure f: figures)
						height += f.getSize().height;
					height /= figures.size();
					
					for (Figure f: figures){
						if (f instanceof AEntityBox){
							AEntityBox eb = (AEntityBox) f;
							AMoveAction moveAction = new AMoveAction(eb, eb.getBounds());
							eb.setSize(eb.getSize().width, height);
							action.addAction(moveAction);
						}
					}
					
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
				}
				
			});
			
			if (figures.size() > 2){
				MenuItem equalSpacing = new MenuItem(subMenuB, SWT.CASCADE);
				equalSpacing.setText("Equal spacing");
				equalSpacing.addListener(SWT.Selection, new Listener(){

					@Override
					public void handleEvent(Event event) {
						ACompoundAction action = new ACompoundAction();
						Collections.sort(figures, new TopComparator());
						
						Point loc1 = figures.get(0).getLocation();
						Point loc2 = figures.get(1).getLocation();
						Dimension difference = loc2.getDifference(loc1);
						Point nextLocation = loc1;
						
						for (Figure f: figures){
							AMoveAction moveAction = new AMoveAction(f, f.getBounds());
							f.setLocation(nextLocation);
							action.addAction(moveAction);
							nextLocation = nextLocation.getTranslated(difference);
						}
						
						setChanged();
						notifyObservers(action);
						restoreBorders(figures);
					}
					
				});
			}

			
			
			
			Menu subMenuD = new Menu(shell, SWT.DROP_DOWN);
			vertical.setMenu(subMenuD);
			MenuItem verticalLeft = new MenuItem(subMenuD, SWT.CASCADE);
			verticalLeft.setText("Left");
			MenuItem verticalCenter = new MenuItem(subMenuD, SWT.CASCADE);
			verticalCenter.setText("Center");
			MenuItem verticalRight = new MenuItem(subMenuD, SWT.CASCADE);
			verticalRight.setText("Right");

			
			verticalLeft.addListener(SWT.Selection, new Listener(){
				int hmin = Integer.MAX_VALUE;
				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					for (Figure box: figures){
						if (box.getLocation().x < hmin)
							hmin = box.getLocation().x;
					}
					for (Figure ff: figures){
		//				contents.add((Figure)ff);
						if (ff instanceof AEntityBox){
							AEntityBox eb = (AEntityBox) ff;
							AMoveAction moveAction = new AMoveAction(eb, eb.getBounds());
							((AEntityBox) ff).setLocation(new Point(hmin, ((AEntityBox) ff).getLocation().y));	
							action.addAction(moveAction);
						}
					}
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
//					AState.setCursorMode(CursorMode.NULL);
				}
			});
			verticalRight.addListener(SWT.Selection, new Listener(){
				int hmax = 0;
				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					for (Figure box: figures){
						if (box.getLocation().x + box.getSize().width > hmax)
							hmax = box.getLocation().x + box.getSize().width;
						
					}
					for (Figure f: figures){
			//			contents.add((Figure)f);
						if (f instanceof AEntityBox){
							AEntityBox eb = (AEntityBox) f;
							AMoveAction moveAction = new AMoveAction(eb, eb.getBounds());
							((AEntityBox) f).setLocation(new Point(hmax - ((AEntityBox) f).getSize().width, ((AEntityBox) f).getLocation().y));	
							action.addAction(moveAction);
						}
					}
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
//					AState.setCursorMode(CursorMode.NULL);
				}
			});
			verticalCenter.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					int hmax = Integer.MIN_VALUE, hmin = Integer.MAX_VALUE;
					for (Figure box: figures){
						if (box instanceof AEntityBox){
							if (box.getBounds().getBottomRight().x > hmax)
								hmax = box.getBounds().getBottomRight().x;
							if (box.getBounds().getBottomLeft().x < hmin)
								hmin = box.getBounds().getBottomLeft().x;
						}
					}
					int center = hmin + (hmax-hmin)/2;
					for (Figure b: figures){
						AMoveAction moveAction = new AMoveAction(b, b.getBounds());
						b.setLocation(new Point(center - b.getSize().width/2, b.getBounds().y));	
						action.addAction(moveAction);
					}
					setChanged();
					notifyObservers(action);
					restoreBorders(figures);
//					AState.setCursorMode(CursorMode.NULL);
				}
			});
			;
			
			MenuItem replicateItem = new MenuItem(menu, SWT.CASCADE);
			replicateItem.setText("Replicate");
			replicateItem.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event event) {
					LinkedList<Figure> repFigures = new LinkedList<Figure>();
					AEntity entities = ASdaiHandler.replicateEntities(figures);
					try {
						ACompoundAction action = new ACompoundAction();
						
						SdaiIterator iterator = entities.createIterator();
						while (iterator.next()){
							EEntity entity = entities.getCurrentMemberEntity(iterator);
							model.addEntityToTree(entity);
							
							ACreateEntityAction a1 = new ACreateEntityAction(entity);
							action.addAction(a1);
							
							Figure f = figures.getFirst();
							while (! (f instanceof AEntityBox)){
								figures.remove(f);
								f = figures.getFirst();
								if (figures.size() == 0)
									return;
							}
							AState.lPoint = f.getLocation();
							f.setBorder(new ABoxBorder());
							figures.remove(f);
							setChanged();
							notifyObservers(entity);
							AEntityBox box = AModelImpl.getEntityBox(entity);
							box.setSize(f.getSize());
							box.setLocation(box.getLocation().getTranslated(150, 20));
							box.setBorder(new LineBorder(ColorConstants.red,7));
							repFigures.add(box);
							/// FIXME Add new instances to marked entities
							AShowAction a2 = new AShowAction(box);
							action.addAction(a2);
						}
						setChanged();
						notifyObservers(action);
						
						AState.setActiveObject(repFigures);
						AState.setCursorMode(CursorMode.NULL);
						AState.keyMode = KeyMode.CTRL_RELEASED;
					}catch (SdaiException e){	e.printStackTrace();
					} catch (AWrongFormatException e) {		e.printStackTrace();
					}
				}
				
			});
			
			MenuItem hideItem = new MenuItem(menu, SWT.CASCADE);
			hideItem.setText("Hide");
			hideItem.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event event) {
					ACompoundAction action = new ACompoundAction();
					for (Figure figure: figures){
						if (figure instanceof AEntityBox){
							AEntityBox box = (AEntityBox) figure;
							AAction a = new AHideAction(box.getEntityRepresentation(), box.getBounds());
							AModelImpl.hideEntityBox(box);
							box.hide();
							contents.remove(box);
							action.addAction(a);
						}
					}
					setChanged();
					notifyObservers(action);
				}
				
			});
			MenuItem deleteItem = new MenuItem(menu, SWT.CASCADE);
			deleteItem.setText("Delete");
			deleteItem.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event event) {
					for (Figure figure: figures){
						if (figure instanceof AEntityBox){
							if (figure.getParent() == contents){
								AEntityBox box = (AEntityBox) figure;
								EEntity entity = box.getEntityRepresentation();
								AModelImpl.removeEntity(entity);
								box.hide();
								contents.remove(box);
								ASdaiHandler.deleteEntityInstance(entity);
							}
						}
					}
					treeViewer.refresh();
				}
				
			});
			
			canvas.setMenu(menu);
			canvas.layout();
		}
		
	}
	
	
	

	protected void restoreBorders(LinkedList<Figure> figures) {
		for (Figure f: figures){
			if (f instanceof AEntityBox){
				f.setBorder(new ABoxBorder());
			}
		}
		
	}

	/**
	 * Display the right-click menu for a single instance-box
	 */
	public void displayBoxMenu(final AEntityBox box) {
		Menu menu = new Menu(canvas); 						
		final MenuItem referenceItem = new MenuItem(menu, SWT.CASCADE);
		MenuItem implicitItem = new MenuItem(menu, SWT.CASCADE);
		MenuItem hideItem = new MenuItem(menu, SWT.CASCADE);
		hideItem.setText("Hide");
		MenuItem drawItem = new MenuItem(menu, SWT.CASCADE);
		MenuItem nameItem = new MenuItem(menu, SWT.CASCADE);
		MenuItem editRelationship = new MenuItem(menu, SWT.CASCADE);
//		MenuItem validateItem = new MenuItem(menu, SWT.CASCADE);
		MenuItem changeColorItem = new MenuItem(menu, SWT.CASCADE);
		
		MenuItem setSizeItem = new MenuItem(menu, SWT.CASCADE);	
		setSizeItem.setText("Size");
		MenuItem replaceItem = new MenuItem(menu, SWT.CASCADE);
		replaceItem.setText("Replace with... ");
		
		MenuItem bendPointItem = new MenuItem(menu, SWT.CASCADE);
		MenuItem deleteItem = new MenuItem(menu, SWT.CASCADE);
		deleteItem.setText("Delete");
		
		//--------------------------------------------------------------------Size
		setSizeItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				String response = showSizeDialog(box.getSize());
				if (response != null){
					String[] wh = response.split(",");
					box.setSize(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
				}
			}
		});
		//------------------------------------------------------------------- Delete
		deleteItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				try  {
					EEntity entity = box.getEntityRepresentation();

					ACompoundAction action = new ACompoundAction();
					AAction a1 = new AHideAction(entity, box.getBounds());
					ADeleteEntityAction a2 = new ADeleteEntityAction(entity);
					action.addAction(a2);	
					action.addAction(a1);

					AModelImpl.removeEntity(entity);
					box.hide();
					contents.remove(box);
					ASdaiHandler.deleteEntityInstance(entity);
					treeViewer.refresh();
					
					setChanged();
					notifyObservers(action);
				} catch (Exception e){
					e.printStackTrace();
				}

			}
			
		});
		//------------------------------------------------------------------- Replace with...
		Menu replaceMenu = new Menu(shell, SWT.DROP_DOWN);
		replaceItem.setMenu(replaceMenu);
		
		MenuItem subclassItem = new MenuItem(replaceMenu, SWT.CASCADE);
		subclassItem.setText("Subclass");
		MenuItem superclassItem = new MenuItem(replaceMenu, SWT.CASCADE);
		superclassItem.setText("Superclass");
		
		Menu subclassMenu = new Menu(shell, SWT.DROP_DOWN);
		subclassItem.setMenu(subclassMenu);
		Menu superclassMenu = new Menu(shell, SWT.DROP_DOWN);
		superclassItem.setMenu(superclassMenu);
		
		ATreeNode node = AModelImpl.getTreeNode(box.getEntityRepresentation().getClass());
		if (node == null){
			Class clazz = box.getEntityRepresentation().getClass();
			int dollarIdx = clazz.getSimpleName().indexOf("$");
			//FIXME! Just temp solution. This should be in the tree to start with, Duplicate?
			node = AModelImpl.getTreeNodeByName(clazz.getSimpleName().substring(1, dollarIdx));
			if (node != null){
			//	AModelImpl.addToClassTree(box.getEntityRepresentation().getClass(), node);
			}
			else {
				System.err.println("Tree node not found! ");
				return; 
			}

		}
		Set<String> items = new HashSet<String>();
		for (final ATreeNode n: node.getChildren()){
			if (items.contains(n.getName()))
				continue;
			MenuItem subItem = new MenuItem(subclassMenu, SWT.CASCADE);
			subItem.setText(n.getName());
			items.add(n.getName());
			subItem.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event event) {
					EEntity nentity = model.replaceEntity(box, n.getName());
					if (nentity == null)
						return;
					contents.remove(box);
					setChanged();
					notifyObservers(nentity);

				}
				
			});
		}
		items.clear();
		
		for (final ATreeNode n: node.getParents()){
			if (items.contains(n.getName()))
				continue;
			if (! n.getName().equals("Schema")){
				MenuItem supItem = new MenuItem(superclassMenu, SWT.CASCADE);
				supItem.setText(n.getName());
				items.add(n.getName());
				supItem.addListener(SWT.Selection, new Listener(){

					@Override
					public void handleEvent(Event event) {
						EEntity nentity = model.replaceEntity(box, n.getName());
						if (nentity == null)
							return;
						contents.remove(box);
						setChanged();
						notifyObservers(nentity);
					}
					
				});
			}
		}
		if (subclassMenu.getItemCount() == 0)
			subclassItem.dispose();
		if (superclassMenu.getItemCount() == 0)
			superclassItem.dispose();
		if (replaceMenu.getItemCount() == 0)
			replaceItem.dispose();
		//------------------------------------------------------------------- Bendpoints
		bendPointItem.setText("Add bendpoint to..");
		Menu bendPointMenu = new Menu(shell, SWT.DROP_DOWN);
		bendPointItem.setMenu(bendPointMenu);
		if (box.getConnections().size() > 0){
			for (final PolylineConnection c: box.getConnections()){
				for (Object f: c.getChildren()){
					if (f instanceof org.eclipse.draw2d.Label){
						MenuItem subClass = new MenuItem(bendPointMenu, SWT.CASCADE);
						subClass.setText(((org.eclipse.draw2d.Label)f).getText());
						subClass.addListener(SWT.Selection, new Listener(){

							@Override
							public void handleEvent(Event event) {
							//	addBendpoint(c);
								ABendPoint bp = box.addBendpoint(c);
								Point point = c.getStart();
								Point end = c.getEnd();
								Point middle = new Point((point.x + end.x)/2, (point.y + end.y)/2);
								contents.add(bp);
								contents.add(bp.getInConnection());
								bp.setLocation(middle);
								refresh();
							}
							
						});
					}
					else { 
					}
				}
			}
		}
		else {
			bendPointItem.dispose();
		}
		
		//------------------------------------------------------------------- Hide box
		hideItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				try{
					AAction action = new AHideAction(box.getEntityRepresentation(), box.getBounds());
					AModelImpl.hideEntityBox(box);
					box.hide();
					contents.remove(box);
					setChanged();
					notifyObservers(action);
				} catch (IllegalArgumentException e){
					e.printStackTrace();
				}
			}
			
		});	
		//-------------------------------------------------------------------Show implicit entities
		implicitItem.setText("Implicit");
		
		Menu subMenuI = new Menu(shell, SWT.DROP_DOWN);
		implicitItem.setMenu(subMenuI);
			
		Class entityClass = box.getEntityRepresentation().getClass();
		LinkedList <Class> implicitClasses = AModelImpl.getImplicitClasses(entityClass);
		int memCount = implicitClasses.size();
		
 		Set <Class> usedClasses = new HashSet <Class>();
 		if (memCount > 0){
			MenuItem[] subItemsI = new MenuItem[memCount]; 
			int index=0;
			for (final Class clazz: implicitClasses){
				if (! usedClasses.contains(clazz)){
					usedClasses.add(clazz);
					subItemsI[index] = new MenuItem(subMenuI, SWT.CASCADE);
					subItemsI[index].setText(clazz.getSimpleName().substring(1));
					subItemsI[index].addListener(SWT.Selection, new Listener(){

						@Override
						public void handleEvent(Event event) {
							
							AState.setActiveObject(box);
							try {
								AState.setCursorMode(CursorMode.IMPLICIT);
							} catch (AWrongFormatException e) {	e.printStackTrace();
							}
							setChanged();
							notifyObservers(clazz.getSimpleName().substring(1));

						}
							
					});
					index++;
				}
			}
 		}
 		else implicitItem.dispose();
		//------------------------------------------------------- Referenced 
 		referenceItem.setText("Referenced by");

		try {
			Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
			referenceItem.setMenu(subMenu);

			AEntity storage = ASdaiHandler.findEntityInstanceUsers(box.getEntityRepresentation());
 			memCount = storage.getMemberCount();
			
			SdaiIterator iterator = storage.createIterator();
			MenuItem[] subItems = new MenuItem[memCount]; 
			int i=0;
			while (iterator.next()){
				final EEntity current = storage.getCurrentMemberEntity(iterator);
				if (! AModelImpl.isEntityBox(current)){
					subItems[i] = new MenuItem(subMenu, SWT.CASCADE);
					subItems[i].setText(current.getPersistentLabel().concat(" ").concat(current.getClass().getSimpleName().substring(1)));
					subItems[i].addListener(SWT.Selection, new Listener(){

						@Override
						public void handleEvent(Event event) {	
							
							AState.setActiveObject(box);
							setChanged();
							notifyObservers(current);
							
						}
						
					});	
					i++;
				}
			} 
			
			if (i==0)	// No non-visible referenced items
				referenceItem.dispose();
			
		} catch (SdaiException se) { se.printStackTrace(); }	
		//----------------------------------------------- Short names
		nameItem.setText("Name");
		Menu nameSubMenu = new Menu(shell, SWT.DROP_DOWN);
		nameItem.setMenu(nameSubMenu);
		final String entityName = box.getEntityRepresentation().getClass().getSimpleName().substring(1).toLowerCase();
		LinkedList<String> names = AModelImpl.getShortName(entityName);
		if (names != null){
			MenuItem eItem = new MenuItem(nameSubMenu, SWT.CASCADE);
			eItem.setText(entityName);
			eItem.addListener(SWT.Selection, new Listener(){

				@Override
				public void handleEvent(Event event) {
				
						if (AState.labelType == LabelType.PERSISTENT)
							box.getLabel().setText(ASdaiHandler.getPersistantLabel(box.getEntityRepresentation()).concat(" : ").concat(entityName.substring(0,1).toUpperCase().concat(entityName.substring(1))));
						else box.getLabel().setText(entityName.substring(0,1).toUpperCase().concat(entityName.substring(1)));
					}

			});
			
			for(final String s: names){
				MenuItem sItem = new MenuItem(nameSubMenu, SWT.CASCADE);
				sItem.setText(s);
				
				sItem.addListener(SWT.Selection, new Listener(){

					@Override
					public void handleEvent(Event event) {
						box.setName(s);
						if (AState.labelType == LabelType.PERSISTENT)
							box.persistentName();
						else box.basicName();

					}
					
				});
			}

		}
		MenuItem aItem = new MenuItem(nameSubMenu, SWT.CASCADE);
		aItem.setText("Add...");
		aItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				// new GIAddName(display, entityName, view, model);
				
			}
			
		});
		//---------------------------------------------- Draw relationship
		drawItem.setText("Draw relationship");
		drawItem.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				try {
					PolylineConnection drawLine = new PolylineConnection();				
					Figure drawBox = new Figure();
					drawBox.setLocation(box.getLocation().getTranslated((int) box.getSize().width/2,box.getSize().height+40));			// GET MOUSE LOCATION!!
					
					AState.activeConnection = drawLine;	
					AState.setActiveObject(drawBox);
					ChopboxAnchor targetAnchor = new ChopboxAnchor(drawBox);		
					ChopboxAnchor sourceAnchor = new ChopboxAnchor(box);
					drawLine.setSourceAnchor(sourceAnchor);
					drawLine.setTargetAnchor(targetAnchor);
					drawLine.setConnectionRouter(null);
					PolygonDecoration decoration = ALayout.getDecoration();
					drawLine.setTargetDecoration(decoration);
					contents.add(drawBox);
					contents.add(drawLine);
					AState.setCursorMode(CursorMode.DRAW);	
					AState.matches = new LinkedList<EEntity>();
					for (AAttributeFigure figure: box.getAttributeFigures()){
						if (figure instanceof AEntityFigure){
							for (Class<?> clazz: figure.getAttribute().getReturnInfo().getReturnClasses()){
								AState.matches.addAll(ASdaiHandler.findAllInstances(clazz));							
							}
						}
					}
				} catch (AWrongFormatException e){
					displayErrorMessage(e);
				} catch (Exception e){ e.printStackTrace(); }
			}
			
		});
		//-----------------------------------------------------------------------------------------------
		editRelationship.setText("Edit attribute");
		Menu editSubMenu = new Menu(shell, SWT.DROP_DOWN);
		editRelationship.setMenu(editSubMenu);
		for (final AAttributeFigure figure: box.getAttributeFigures()){
			if (figure instanceof AEntityFigure){
				MenuItem item = new MenuItem(editSubMenu, SWT.CASCADE);
				item.setText(figure.getAttribute().getName());
				item.addListener(SWT.Selection,  new Listener(){

					@Override
					public void handleEvent(Event arg0) {
						setChanged();
						notifyObservers((AEntityFigure) figure);
					}
					
				});
			}
			else if (figure.getAttribute().getReturnInfo().isMultipleChoise()){
				Object value = figure.getAttribute().getAttributeValue();
				MenuItem item = new MenuItem(editSubMenu, SWT.CASCADE);
				item.setText(figure.getAttribute().getName());
				Menu itemMenu = new Menu(shell, SWT.DROP_DOWN);
				item.setMenu(itemMenu);
				int index = 1;
				for (String s: figure.getAttribute().getReturnInfo().getChoises()){
					if (s.equals("$"))
						continue;
					final MenuItem innerItem = new MenuItem(itemMenu, SWT.CHECK);
					innerItem.setText(s);
					if (value instanceof Integer && ((Integer)value) == index){
						innerItem.setSelection(true);
						innerItem.addListener(SWT.Selection, new Listener(){

							@Override
							public void handleEvent(Event event) {
								figure.setAttributeValue(null);	
							}
							
						});
					}
					else if (value instanceof Integer || value == null){
						final int aggIndex = index;
						innerItem.addListener(SWT.Selection, new Listener(){

							@Override
							public void handleEvent(Event event) {
								figure.setAttributeValue(aggIndex);
							}
							
						});
					}
					index ++;
				}
			}
		}
		if (editSubMenu.getItemCount() == 0)
			editRelationship.dispose();

		changeColorItem.setText("Color");
		Menu colorMenu = new Menu(shell, SWT.DROP_DOWN);
		MenuItem item = new MenuItem(colorMenu, SWT.CASCADE);
		item.setText("Active color");
		item.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				box.setBackgroundColor(bgBox.getBackgroundColor());
				
			}
			
		});
		new MenuItem(colorMenu, SWT.SEPARATOR);
		changeColorItem.setMenu(colorMenu);
		
		setCursorMode(CursorMode.MENU);
		canvas.setMenu(menu);
		canvas.layout();
		
		
	}

	public void removeFromContents(Figure figure) {
		try{ 
			contents.remove(figure);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public void removeFromPaintContents(Figure figure) {
		try{ 
			paintContents.remove(figure);
		} catch (Exception e) {e.printStackTrace();}
	}

	public void editAttribute(AValueFigure figure) {
		figure.edit();
		refresh();
		
	}

	public Display getDisplay() {
		return display;
	}

	public void fitView() {
		 contentsFigure.setScale(1);
		 int minX = Integer.MAX_VALUE; int minY = Integer.MAX_VALUE; int maxX = Integer.MIN_VALUE; int maxY = Integer.MIN_VALUE;;

		 for (Figure figure: backgroundFigures){
			 for (Object f: figure.getChildren()){
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

		 double diffX = maxX - minX + 50;
		 double diffY = maxY - minY + 50;
		 diffX = SCREEN_RESOLUTION.x/diffX;
		 diffY = SCREEN_RESOLUTION.y/diffY;
	 
		 minX = minX - 25;
		 minY = minY - 25;

		 contents.setBounds(contents.getBounds().getTranslated(minX*-1,minY*-1));
		 paintContents.setBounds(paintContents.getBounds().getTranslated(minX*-1,minY*-1));
		 double scale;
		 if (diffX > diffY)
			 scale = diffY;
		 else scale = diffX;
		 if (scale > 1)
			 return;
		 contentsFigure.setScale(scale);
	}

	public void adjustText(char character) {
		((AValueFigure)AState.getActiveObject()).adjustText(character);
		refresh();
	}
	
	public void eraseCharacter() {
		((AValueFigure)AState.getActiveObject()).eraseChar();
		refresh();
	}

	public void save() {
		PrintWriter writer = null;
		try {
			if (AState.filePath != null){
				String s = AState.getTempDir();
				File dir = new File(s);
				writer = new PrintWriter(s.concat("error_log.txt"), "UTF-8");

				if (!dir.canWrite()){
					displayErrorMessage(new AException("Unable to write in directory: ".concat(AState.filePath.replaceAll("[a-zA-Z_0-9]+\\.giz", "").concat(".\n Permission denied.")), "", 1)); //System.out.println("CANNOT WRITE IN DIRECTORY: " + AState.filePath.replaceAll("[a-z]+\\.giz", ""));
					writer.println("Write access denied.");
					writer.println();
				}
				else {
					writer.println("Write access OK.");
					writer.println();
				}
			
				File xml = AXMLParser.createXML(s.concat("graphics.xml"), contents, paintContents);
				writer.println("XML file created in path: " + xml.getAbsolutePath());
				
				
				String stepFile = s.concat("data");
	 			ASdaiHandler.saveModel(stepFile);
	 			//File stepFile = new File(AState.getTempDir().concat("data.stp"));
	 			
	 			writer.println("STEP file created.");
	 			
				try {
		 			final int BUFFER = 2048;
		 			FileOutputStream dest;
		 			
					dest = new FileOutputStream(AState.filePath);
					writer.println("Zipping....");
		   			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

		   			byte data[] = new byte[BUFFER];
		   			int nrFiles = 2;
		   			
//		            String files[] = new String[nrFiles];			            
//		            files[0] = stepFile.concat(".stp");
//		            files[1] = s.concat("graphics.xml");
//		            files[0] = s.concat("graphics.xml");
		            File[] files = new File[nrFiles];	
		            files[0] = new File(stepFile.concat(".stp"));
		            files[1] = xml;
		     //       files[2] = "short_names.sn";
		     //       files[2] = tempDir.concat(fileSep).concat("short_names.sn");
	
		            for (int i=0; i<files.length; i++) {
		               FileInputStream fi = new FileInputStream(files[i]);
		               BufferedInputStream bi = new BufferedInputStream(fi, BUFFER);
		               String name = dir.toURI().relativize(files[i].toURI()).getPath();
		               ZipEntry entry = new ZipEntry(name);
		               out.putNextEntry(entry);
		               int count;
		               while((count = bi.read(data, 0, BUFFER)) != -1) {
		                  out.write(data, 0, count);
		               }
			            bi.close();
		            }
		            out.close();
		            writer.println("Giz file created.");
		            setStatusMessage("File saved");
				} catch (FileNotFoundException e) {	
					writer.println(" --------------------- ERROR IN ZIPPING -------------------------- ");

					e.printStackTrace();
					e.printStackTrace(writer);
					writer.close();
					return;
				} catch (IOException e) { 
					writer.println(" --------------------- ERROR IN ZIPPING ------------------------- ");

					e.printStackTrace();
					e.printStackTrace(writer);
					writer.close();
					return;
	            }
	            new File("data.stp").delete(); 
	            new File("graphics.xml").delete();
	
	    		writer.close();
	    		new File(s.concat("error_log.txt")).delete();
	            
			} else {
				saveAs();
			}
		} catch (Exception e){
			e.printStackTrace();
			writer.println(" --------------------- ERROR IN CREATING GIZ-------------------------- ");
			e.printStackTrace(writer);
			writer.close();
			return;
		}		
	}
	
	private void saveAs() {
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
 	    fd.setText("Save as");
 	    String[] filterExt = { "*.giz" };
 	    fd.setFilterExtensions(filterExt);
 	    String selected = fd.open();
 	    if (selected != null){
 	      	AState.filePath = selected;
 	       	save();
     		shell.setText(NAME.concat(" [").concat(selected.replace(".xml", ".stp")).concat("]"));
 	    }
	}

	public boolean displayYesNoMessage(String title, String message){
		MessageBox msgBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		msgBox.setText(title);
		msgBox.setMessage(message);
		int response = msgBox.open();
		if (response == SWT.YES)
			return true;
		else return false;
	}


	public void displayErrorMessage(AException e) {
		MessageBox msgBox = new MessageBox(shell, e.getType() | SWT.OK);
		msgBox.setText(e.getTitle());
		msgBox.setMessage(e.getMessage());
		msgBox.open();
		if (AState.getActiveObject() instanceof AEditAttribute)
			((AEditAttribute)AState.getActiveObject()).focus();
	}
	
	public void displayInfoMessage(String title, String message) {
 	    MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION| SWT.OK);
 	    messageBox.setMessage(message);
 	    messageBox.setText(title);
 	    messageBox.open();
	}

	public void addToPaintContents(Shape figure) {
		Point location = null;
		if (paintContents.getChildren().contains(figure)){
			location = figure.getLocation();
			paintContents.remove(figure);
			paintContents.add(figure);	
			figure.setLocation(location);
			return;
		}
		if (AState.lPoint != null){	
			figure.setLocation(AState.lPoint);
			figure.setLineWidth(ALayout.borderWidth);
			figure.setForegroundColor(borderColors[0]);
			figure.setBackgroundColor(bgBox.getBackgroundColor());
			figure.setOpaque(true);
			AState.setActiveObject(figure);
		}
		paintContents.add(figure);	
	}
	
	public void addToPaintContents(ATextBox figure) {
		Point location = null;
		if (paintContents.getChildren().contains(figure)){
			location = figure.getLocation();
			paintContents.remove(figure);
			paintContents.add(figure);	
			figure.setLocation(location);
			return;
		}
		if (AState.lPoint != null){	
			figure.setLocation(AState.lPoint);
			figure.setForegroundColor(bgBox.getBackgroundColor());
			AState.setActiveObject(figure);
		}
		paintContents.add(figure);	
	}

	
	private void setFillCursor(Color color, Color borderColor, int width){
		  PaletteData palette = new PaletteData(new RGB [] {borderColor.getRGB(), color.getRGB(),});
		  ImageData sourceData = new ImageData(width, width, 1, palette);
		  for (int i = 1; i < width-1; i ++) 
		    for (int j = 1; j < width-1; j++) 
		      sourceData.setPixel(i, j, 1);
		  Cursor cursor = new Cursor(display, sourceData, width/2, width/2);
		  shell.setCursor(cursor);
	}
	
	public void unabelUndo(){
		
	}
	
	private void showSideMenu(boolean visible){
		if (visible)
			sash.setWeights(new int[] { 30, 70});
		else sash.setWeights(new int[] { 2, 98});
	}

	public Color getActiveColor() {
		return bgBox.getBackgroundColor();
	}
	
	public IFigure getPaintFigure(Point p){
		return paintContents.findFigureAt(p);
	}

	public void translateContents(Point p) {
		contents.setBounds(contents.getBounds().getTranslated(p));
		paintContents.setBounds(paintContents.getBounds().getTranslated(p));	
	}

	public void toggleSideMenu() {
		if (showSideMenu.getSelection())
			showSideMenu.setSelection(false);
		else showSideMenu.setSelection(true);
		showSideMenu(showSideMenu.getSelection());
		
	}

	public void nullMenu() {
		canvas.setMenu(null);
		canvas.layout();
	}
	
	public String getUserInput(AReturnInfo info){
		InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(),
		"Type select value", "Type value: (".concat(info.returntype).concat(")"), "", new ATypeValidator(info));
		if (dlg.open() == Window.OK) {
			return dlg.getValue();
		}
		return null;
	}
	
	public String showSizeDialog(Dimension d){
		String width = Integer.toString(d.width);
		String height = Integer.toString(d.height);
		InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(),
		"Size", "Size: (w, h) ", width.concat(",").concat(height), new AWidthHeightValidator());
		if (dlg.open() == Window.OK) {
			return dlg.getValue();
		}
		return null;
	}

	public Color getBorderColor() {
		return borderColors[0];
	}
	
	public int showOptionDialog(int count) {
		String options = "Choose which aggregate you wish to add the instance in. (1-".concat(Integer.toString(count+1).concat(")"));
		InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(),
		"Choose attribute", options, "1", new ABelowXValidator(count + 1));
		if (dlg.open() == Window.OK) {
			return Integer.parseInt(dlg.getValue().replace(" ", "")) - 1;
		}
		return -1;
	}

	public int showOptionDialog(LinkedList<AEntityFigure> figures) {
		String options = "Choose attribute to set: \n";
		for (int i = 0; i < figures.size(); i++){
			String name = Integer.toString(i+1).concat(": " ).concat(figures.get(i).getAttribute().getName()).concat("\n");
			options = options.concat(name);
		}
		InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(),
		"Choose attribute", options, "1", new ABelowXValidator(figures.size() + 1));
		if (dlg.open() == Window.OK) {
			return Integer.parseInt(dlg.getValue().replace(" ", "")) - 1;
		}
		return -1;
	}
	
	private void displayProgress() {
		sMessage = sMessage.concat(".");
		if (sMessage.endsWith("......."))
			sMessage = sMessage.replace(".......", "");
		manager.setMessage(sMessage);
		shell.update();		
		shell.redraw();
		display.update();
	}
	
	public void setStatusMessage(String message){
		sMessage = message;
		manager.setMessage(message);
	}

	/**
	 * Update the graphical representation to include changes made in the text file. 
	 * 
	 */
	public void updateGraphics() {
		try {
			textModified = false;
			Point cbounds = contents.getLocation();
			double scale = contentsFigure.getScale();
			
			String textloc = AState.getTempDir().concat("tempstp.stp");
     		FileWriter fw = new FileWriter(textloc);		
     		BufferedWriter br = new BufferedWriter(fw);
     		String[] text = textArea.getText().split("\n");
     		for(int l=0;l<text.length;l++){
     			br.write(text[l]);
     		}
     		br.close();
     		if (ASdaiHandler.getEntityCount() > 0){
         		if (!ASdaiHandler.isValidSTEP(textloc)){
             		setStatusMessage("Text is not a valid STEP file... ");
             		updateText();
         			return;
         		}
     		}
     		else return;

			File xmlfile = AXMLParser.createXML(AState.getTempDir().concat("tpxml.xml"), contents, paintContents); 
			
			contents.setLocation(new Point(0,0));
			paintContents.setLocation(new Point(0,0));
			contentsFigure.setScale(1);
			
			if (xmlfile != null){
	        	clean();
	        	AModelImpl.clearFileInfo();
				openStepFile(textloc);
				if (AXMLParser.readXML(xmlfile, this))
					xmlfile.delete();
 
			}
			contents.setBounds(contents.getBounds().getTranslated(cbounds));
			paintContents.setBounds(contents.getBounds().getTranslated(cbounds));
			contentsFigure.setScale(scale);
			setStatusMessage("Graphics synced with text file");
			refresh();
			
     	} catch (IOException e) { e.printStackTrace();
     	} catch (AEntitiesNotFoundException e) {e.printStackTrace();
		}
		
	}

	public void makeViewableAt(Rectangle bounds, org.eclipse.draw2d.MouseEvent event) {
		contents.setBounds(contents.getBounds().getTranslated(-1*(bounds.x - event.getLocation().x + bounds.width/2), -1*(bounds.y - event.getLocation().y + bounds.height/2)));
		paintContents.setBounds(paintContents.getBounds().getTranslated(-1*(bounds.x - event.getLocation().x + bounds.width/2), -1*(bounds.y - event.getLocation().y + bounds.height/2)));
	}

	public int search(String str, int index) {
        String s = textArea.getText();
        if (s.contains(str)){
        	int i=s.indexOf(str, index);
        	if (i==-1){
        		i = s.indexOf(str, 0);
        	}
            textArea.setSelection(i, i+str.length());
            textArea.showSelection();
            return (i+str.length());
        }
        else return -1;
	}
	
	/**
	 * 
	 * @param router
	 */
	public void setConnectionRouter(ConnectionRouter router){
		List<Figure> boxes = contents.getChildren();
		for (Figure box: boxes){
			List <Figure> children = box.getChildren();
			if (box instanceof AEntityBox){
				LinkedList <PolylineConnection> conn = ((AEntityBox) box).getConnections();
				for (PolylineConnection c: conn){
					if (! ((c.getSourceAnchor().getOwner() instanceof AConnectionShaper) && (AState.connection == AState.Connection.MANHATTAN) ))	
						c.setConnectionRouter(router);
				}
				for (Figure child: children){
					if (child instanceof AEntityBox){
						LinkedList <PolylineConnection> conn2 = ((AEntityBox) child).getConnections();
						for (PolylineConnection c: conn2){
							if (! ((c.getSourceAnchor().getOwner() instanceof AConnectionShaper) && (AState.connection == AState.Connection.MANHATTAN) ))	
								c.setConnectionRouter(router);
						}
					}
				}
			}
		}	
	}
	
	public Color getSValue(float sVal, RGB rgb){
		if (rgb.red == 255 && rgb.blue == 255 && rgb.green == 255){
			return ColorConstants.white;
		}
		float[] hsb = java.awt.Color.RGBtoHSB(rgb.red, rgb.green, rgb.blue, null);
		int rgbcol = java.awt.Color.HSBtoRGB(hsb[0], sVal, hsb[2]);
		java.awt.Color newColor = new java.awt.Color(rgbcol);
		return new Color(display, newColor.getRed(), newColor.getGreen(), newColor.getBlue());
	}
	
	public Color matchBrightness(Color color){
		RGB rgb = color.getRGB();
		return getSValue(ALayout.colorSValue, rgb);
	}

	public Figure getPaintContents() {
		return paintContents;
	}

	public void setCursor(int cursor) {
		shell.setCursor(display.getSystemCursor(cursor));		
	}

	public void uncheck(CursorMode cursorMode) {
		switch (cursorMode){
		case RECTANGLE:
			rectangle.setSelection(false);
			break;
		case CIRCLE:
			circle.setSelection(false);
			break;
		case FILL:
			fill.setSelection(false);
			break;
		case TEXTBOX:
			text.setSelection(false);
			break;
		default:
			break;
		}
		
	}
	
	public void hideAll(){
		try {
			ACompoundAction action = new ACompoundAction();
			LinkedList<AEntityBox> boxes = new LinkedList<>(AModelImpl.getBoxes());
			for (AEntityBox box: boxes){
				AAction a = new AHideAction(box.getEntityRepresentation(), box.getBounds());
				AModelImpl.hideEntityBox(box);
				box.hide();
				contents.remove(box);
				action.addAction(a);
			}
			setChanged();
			notifyObservers(action);
		} catch (Exception e){e.printStackTrace();}

	}

	public void setActiveColor(Color backgroundColor) {
		bgBox.setBackgroundColor(backgroundColor);
	}

	public void setUndoAction(boolean b) {
		undoItem.setEnabled(b);
	}
	
	public void setRedoAction(boolean b){
		redoItem.setEnabled(b);
	}

}

/**
 * Compares figures based on y-wise location
 * 
 * @author Annica
 *
 */
class TopComparator implements Comparator<Figure>{

	@Override
	public int compare(Figure o1, Figure o2) {
		if (o1.getLocation().y == o2.getLocation().y)
			return 0;
		else if (o1.getLocation().y > o2.getLocation().y)
			return 1;
		else return -1;
	}
	
}


class ABelowXValidator implements IInputValidator {
	
	private int roof;
	
	public ABelowXValidator(int i){
		roof = i;
	}
	
	public String isValid(String text) {
		text = text.replaceAll(" ", "");
		try{ 
			int index = Integer.parseInt(text);
			if (index <= 0 || index >= roof)
				return"Invalid input";
		}catch (Exception e){
			  return "Invalid input";
		}
	    return null;
	  }
}


class AWidthHeightValidator implements IInputValidator {
	
	public String isValid(String text) {
		text = text.replaceAll(" ", "");
		if (! text.contains(","))
			return "Invalid input";
		String[] wh = text.split(",");
		if (wh.length != 2)
			return "Invalid input";
		try{ 
			  Integer.parseInt(wh[0]);
			  Integer.parseInt(wh[1]);
		}catch (Exception e){
			  return "Invalid input";
		}
	    return null;
	  }
}


class ATypeValidator implements IInputValidator {
	
	private AReturnInfo info;
	
	public ATypeValidator(AReturnInfo info){
		this.info = info;
	}
	
	public String isValid(String text) {
		
		try{ 
			  info.fromString(text);
		}catch (AWrongFormatException e){
			  return "Invalid input";
		}
	    return null;
	  }
}
