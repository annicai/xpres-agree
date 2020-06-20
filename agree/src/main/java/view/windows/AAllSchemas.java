package view.windows;

import java.util.Observable;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import controller.AState;
import sdai.structure.ASchema;
import view.AViewImpl;

public class AAllSchemas extends Observable{
	
	public AAllSchemas(Display display, Image image, final AViewImpl view){
		
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("Schema");
		shell.setBackground(ColorConstants.white);
		final Table table = new Table(shell, SWT.NONE);
		for (String s: ASchema.getAllSchemas()){
			final TableItem item = new TableItem(table, SWT.NONE | SWT.H_SCROLL | SWT.V_SCROLL);
			item.setText(s);
		}
		table.addListener(SWT.MouseDoubleClick, new Listener(){

			@Override
			public void handleEvent(Event arg0) {
				TableItem[] selection = table.getSelection();
				if (selection.length > 0){
					TableItem item = selection[0];
					String schema = item.getText();
					shell.close();
					view.openSchema(schema);
				}
			}
			
		});

		shell.open();
		shell.setSize(600,500);
		
	}

}
