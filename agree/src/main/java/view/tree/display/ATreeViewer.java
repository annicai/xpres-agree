package view.tree.display;

import java.util.Collections;
import java.util.LinkedList;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

public class ATreeViewer extends TreeViewer {

	public ATreeViewer(Composite parent) {
		super(parent);
	}

	
	public ATreeViewer(Composite parent, int i) {
		super(parent, i);
	}

	
	/**
	 * Returns the treeViewer-selection as a valid entity (i e complex entities must be sorted alphabetically
	 * 
	 * @param string
	 * @return
	 */
	protected String getSelectionString() {
		String string = getSelection().toString().substring(1,getSelection().toString().length()-1);
		String s;
		if (string.contains("$")){
			LinkedList <String> parts = new LinkedList <String>();
			TreeItem[] treeItems = getTree().getSelection(); 
			while (string.startsWith("$")){
				parts.addLast(string.substring(1));
				treeItems[0] = treeItems[0].getParentItem();
				string = treeItems[0].getText();
			}
			parts.addLast(string.toLowerCase());
			Collections.sort(parts);

			s = parts.getFirst().substring(0,1).toUpperCase().concat(parts.getFirst().substring(1).toLowerCase());
			for (int j=1; j< parts.size(); j++){
				s = s.concat("$".concat(parts.get(j).toLowerCase()));								
			}
		}
		else s = string;
		return s;
	}

}
