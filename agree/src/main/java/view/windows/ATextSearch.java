package view.windows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import view.AViewImpl;


public class ATextSearch {
	
	private int index;
	private Label found;
	
	public ATextSearch(Display display, Image image, final AViewImpl view){
		index = 0;
		final Shell shell = new Shell(display, SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
		shell.setText("Search");
		shell.setImage(image);
		GridLayout layout = new GridLayout();
		layout.numColumns=4;
		shell.setLayout(layout);
		
		
		GridData gridData1 = new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData1.horizontalSpan=1;
		Label label = new Label(shell, SWT.FILL);
		label.setText("Find: ");
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.horizontalSpan=3;
		final Text text = new Text(shell, SWT.BORDER | SWT.FILL);
		text.setLayoutData(gridData);
		text.setSize(200, 25);
		Button find = new Button(shell, SWT.NONE);
		find.setText("Find next");
		find.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				index = view.search(text.getText(), index);
				if (index==-5){
					found.setVisible(true);
				}
				else found.setVisible(false);
			}
			
		});
		found  = new Label(shell, SWT.FILL);
		found.setText("String not found!");
		found.setVisible(false);
		
		shell.pack();
		shell.open();
	}

}
