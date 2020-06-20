package view.tree.display;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class AContentProvider implements  ITreeContentProvider  {

	public Object[] getElements(Object inputElement) {
		return ((ATreeViewerNode)inputElement).getChildren().toArray();
	}

	public void dispose() {}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	public Object getParent(Object element) {
		if( element == null) {
			return null;
		}
		return ((ATreeViewerNode)element).getParent();
	}

	public boolean hasChildren(Object element) {
		return ((ATreeViewerNode)element).getChildren().size() > 0;
	}
	

	
	
}
