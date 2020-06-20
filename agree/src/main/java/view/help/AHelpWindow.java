package view.help;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import controller.AState;
import view.tree.display.AContentProvider;

public class AHelpWindow {
	
    private Browser browser;
	private AHelpTreeViewer helpViewer;
	
	public AHelpWindow(final Display display) {
		  Image image = display.getShells()[0].getImage();
		  final Shell shell = new Shell(display, SWT.DIALOG_TRIM | SWT.RESIZE);
		  shell.setText("Help");
		  shell.setLayout(new FillLayout());
		  shell.setBackground(ColorConstants.white);
		  shell.setImage(image);
		  shell.setLayout(new FillLayout());
		  
		  helpViewer = new AHelpTreeViewer(shell);
		  helpViewer.setContentProvider(new AHelpContentProvider());
	  
		  helpViewer.setInput(createTree());
		  helpViewer.expandToLevel(2);
		  helpViewer.refresh();
		  
	      try {
		        browser = new Browser(shell, SWT.BORDER);
		        
		        URL cssURL = this.getClass().getResource("/html/style.css");
		        
				BufferedReader reader = new BufferedReader(new InputStreamReader(cssURL.openStream()));

  		        BufferedWriter writer = new BufferedWriter(new FileWriter(AState.getTempDir().concat("style.css")));
  		        String line;
			   	while ((line = reader.readLine()) != null){
			   		writer.write(line);
			   		writer.newLine();
			   	}
			   		   	
			    reader.close();
				writer.close();
		        
		        
				loadPage("index.html");
				loadPage("Gettingstarted.html");
				loadPage("Controls.html");
				loadPage("Editattributes.html");
				
				
				
				browser.setUrl(AState.getTempDir().concat("index.html"));
				browser.update();
		         
		  } catch (SWTError e) {
			  e.printStackTrace();
		  } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		  
		  helpViewer.setControl(browser);
		  
		  shell.setSize(800,300);
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

	private void loadPage(String page) throws IOException {
		URL url = this.getClass().getResource("/html/".concat(page));
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

		BufferedWriter writer = new BufferedWriter(new FileWriter(AState.getTempDir().concat(page)));
        String line = "";
	   	while ((line = reader.readLine()) != null){
	   	   line = line.replaceAll("style.css", AState.getTempDir().concat("style.css"));
	   	   writer.write(line);
	   	   writer.newLine();
	   	}
	   		   	
	    reader.close();
		writer.close();
		
	}

	private Object createTree() {
		  AHelpItem root = new AHelpItem("Root", null);
		  AHelpItem item  = new AHelpItem("Index", root);
		  root.addChild(item);
		  
		  AHelpItem gettingStarted = new AHelpItem("Getting started", item);

		  AHelpItem controls = new AHelpItem("Controls", item);
		  
		  AHelpItem editAttributes = new AHelpItem("Edit attributes", item);
		//  AHelpItem editAttributes = new AHelpItem("Edit attributes", item);
		//  AHelpItem editAttributes = new AHelpItem("Edit attributes", item);
		  item.addChild(gettingStarted);
		  item.addChild(editAttributes);
		  item.addChild(controls);

		  
	/*	  AHelpItem entityAttributes = new AHelpItem("Entity", editAttributes);
		  AHelpItem enumAttributes = new AHelpItem("Enumeration", editAttributes);
		  AHelpItem aentityAttributes = new AHelpItem("Aggregate", editAttributes);
		  editAttributes.addChild(aentityAttributes);
		  editAttributes.addChild(entityAttributes);
		  editAttributes.addChild(enumAttributes);*/
		  
		  return root;
	}
}
