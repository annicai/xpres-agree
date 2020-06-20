package view.edit.table;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ATableFilter extends ViewerFilter{
	
	private Class clazz;
	
	public ATableFilter(Class clazz) {
		    this.clazz = clazz;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (clazz == null)
			return true;
		else return ((ATableInstance) element).getEntityClass() == clazz;
	}

}
