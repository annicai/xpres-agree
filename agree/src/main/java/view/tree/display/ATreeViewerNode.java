package view.tree.display;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A node in the tree, either an entity definition or an instance of an entity.
 * Superclass of AInstanceNode that represents instances. 
 *
 */

public class ATreeViewerNode implements Serializable{

	protected ATreeViewerNode parent;
	protected ArrayList<ATreeViewerNode> children = new ArrayList<ATreeViewerNode>();
	protected String label;
	private boolean root = false;

	public ATreeViewerNode(ATreeViewerNode parent) {
		setParent(parent);
	}	
	
	public boolean hasChildren(){
		if (getChildren().size() > 0)
			return true;
		else return false;
	}
	
	public boolean hasEntityChild(){
		for (ATreeViewerNode child: getChildren()){
			if (child instanceof AInstanceNode){
				return true;
			}
			else if (child.hasEntityChild()){
				return true;
			}
		}
		return false;
	}
	
	public void setText(String s){
		label=s;
	}
	
	public String toString() {
		return label;
	}

	public ArrayList<ATreeViewerNode> getChildren() {
		return children;
	}

	public void setChildren(ArrayList<ATreeViewerNode> children) {
		this.children = children;
	}

	public ATreeViewerNode getParent() {
		return parent;
	}

	public void setParent(ATreeViewerNode parent) {
		this.parent = parent;
	}

	public void addChild(ATreeViewerNode child) {
		if (! children.contains(child))
			children.add(child);
		
	}
	
	public void setAsRoot(){
		root = true;
	}

	public boolean isRoot() {
		return root;
	}

	public void removeChild(ATreeViewerNode node) {
		children.remove(node);
		
	}
}