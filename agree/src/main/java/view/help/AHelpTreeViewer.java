package view.help;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import controller.AState;
import view.AView;

public class AHelpTreeViewer extends TreeViewer {
	
	private Map<String, AHelpItem> helpMap = new HashMap<String, AHelpItem>();
	private Browser control;

	public AHelpTreeViewer(Composite parent) {
		super(parent);
		
		addSelectionChangedListener(new ISelectionChangedListener(){

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				try {
					String s = getSelection().toString().substring(1,getSelection().toString().length()-1);
					if (! s.contains(",")){
						control.setUrl(AState.getTempDir().concat(s.toString().replaceAll(" ", "")).concat(".html"));
						control.update();
					}
				} catch (Exception e){
					e.printStackTrace();
				}
			}

        });            
		
	}

	public void addMapping(String string, AHelpItem item) {
		helpMap.put(string, item);
		
	}

	public void setControl(Browser browser) {
		this.control = browser;
		
	}
	
	
	

}
