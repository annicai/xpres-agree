package view.edit;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class AEditSorter extends ViewerSorter {
	
	@Override
	public int compare(Viewer viewer,  Object e1,  Object e2){
		try {
			String s1 = e1.toString();
			s1 = s1.substring(s1.indexOf("#")+1, s1.indexOf(":"));
			String s2 = e1.toString();
			s2 = s2.substring(s2.indexOf("#")+1, s2.indexOf(":"));
			if (Integer.parseInt(s1) < Integer.parseInt(s2))
				return 1;
			else return -1;
		} catch (Exception e){e.printStackTrace();}
		return 0;
	}

}
