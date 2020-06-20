package view.tree.display;


import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filter for filtering out nodes that don't contain any entity-instances.
 *
 */
public class AAbstractFilter extends ViewerFilter{

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return (!((ATreeViewerNode) element).toString().contains("(ABSTRACT)"));
	}

}
