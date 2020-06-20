package view.windows;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import view.ALayout;
import view.AResourceLoader;
import view.AViewImpl;

public class AAbout {

	public AAbout(Display display, Image im, AViewImpl view){
		final Shell shell = new Shell(display, SWT.CLOSE);
		shell.setText("About");
		shell.setSize(350,270);
		shell.setImage(im);
		GridLayout l = new GridLayout();
		l.numColumns = 1;
		l.marginTop = 20;
		l.marginLeft = 40;
		l.marginRight = 40;
		l.marginBottom = 20;
		shell.setLayout(l);
		
		Composite text = new Composite(shell, SWT.BORDER);
		GridLayout l2 = new GridLayout();
		l2.numColumns = 1;
		text.setLayout(l2);
		GridData gridData = new GridData(GridData.BEGINNING);
		gridData.horizontalIndent = 1;
		text.setLayoutData(gridData);
		text.setBounds(50, 20, 200, 220);
		Composite imComp = new Composite(text, SWT.NONE);
		GridLayout g2 = new GridLayout();
		g2.numColumns = 2;
		g2.marginTop = 5;
		imComp.setLayout(g2);
		Label image = new Label(imComp,SWT.PUSH);

		image.setImage(im);
		image.setLayoutData(new GridData(SWT.CENTER, SWT.END, false, false));
		Label logo = new Label(imComp, SWT.NONE);
		logo.setText("Agree - A Graphical Express editor\n"
				+ "Version: 1.0.8\n"
				+ "Developed by: KTH Xpress");
		
		Label licence = new Label(text, SWT.NONE);
		licence.setText("License: BSD");

		StyledText tex = new StyledText(text, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        String str = "";
		try {
	        URL textURL = AResourceLoader.getResource("licence.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(textURL.openStream()));
		    try {
		        String line = br.readLine();

		        while (line != null) {
		        	str = str.concat(line).concat("\n");
		            line = br.readLine();
		        }
				tex.setText(str);
		    } finally {
		        br.close();
		    }
		} catch (IOException e){ e.printStackTrace(); }
		tex.setEditable(false);
		tex.setSize(20,20);
		
		Link contact = new Link(text, SWT.NONE);
		contact.setText("Contact: <a>aivert@kth.se</a>");
		contact.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				try {
					Desktop.getDesktop().mail(java.net.URI.create("mailto:aivert@kth.se"));
				} catch (Exception e) {	e.printStackTrace(); }
				
				
			}
			
		});
		shell.pack();
		shell.open();
		ALayout.center(shell);
	}

}
