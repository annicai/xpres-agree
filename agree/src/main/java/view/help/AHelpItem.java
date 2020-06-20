package view.help;

import java.util.LinkedList;
import java.util.List;

public class AHelpItem {

	private String name;
	private List<AHelpItem> children = new LinkedList<AHelpItem>();
	private AHelpItem parent;
	
	public AHelpItem(String name, AHelpItem parent) {
		this.name = name;
		this.parent = parent;
	}
	
	public String toString(){
		return name;
	}
	
	public void addChild(AHelpItem child){
		children.add(child);
	}

	public boolean hasChildren() {
		return (children.size() > 0);
	}

	public Object getParent() {
		return parent;
	}

	public Object[] getChildren() {
		return children.toArray();
	}


}
