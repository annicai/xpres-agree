package view.help;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class AHelpContentProvider implements  ITreeContentProvider{

	@Override
	public Object[] getElements(Object inputElement) {
		return ((AHelpItem)inputElement).getChildren();
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	@Override
	public Object getParent(Object element) {
		if (element == null)
			return null;
		else return ((AHelpItem) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return ((AHelpItem)element).hasChildren();
	}

}
