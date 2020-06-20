package view.edit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jsdai.lang.EEntity;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import view.edit.table.ATableInstance;

public class AAttributeFilter  extends ViewerFilter{

	private Method method;
	private String filter;
	
	public AAttributeFilter(Method method, String filter) {
		    this.method = method;
		    this.filter = filter; 
	}
	
	public void setFilterString(String filter){
		this.filter = filter;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (method == null || filter.length() == 0)
			return true;
		else {
			EEntity entity = ((ATableInstance) element).getEntity();
		    Object args[] = new Object[1]; 
		    args[0] = null; 
		    Object value = null;
			try {
				value = method.invoke(entity, args);
			} catch (IllegalArgumentException e) {e.printStackTrace();}
			catch (IllegalAccessException e) { e.printStackTrace(); }
			catch (InvocationTargetException e) { e.printStackTrace(); }
			if (value == null)
				return false;
			else return ((String) value).contains(filter);
		}
	}



}
