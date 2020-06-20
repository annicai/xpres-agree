package view.tree.display;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filter for filtering out nodes that don't contain any entity-instances.
 *
 */
public class AUsedOnlyFilter extends ViewerFilter{

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return ((element instanceof AInstanceNode) || ((ATreeViewerNode) element).hasEntityChild() || ((ATreeViewerNode) element).getParent().getParent() == null);
	}

}

