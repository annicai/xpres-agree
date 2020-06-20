package model.tree.structure;

import java.io.Serializable;
import java.util.LinkedList;

public class ATreeNode implements Serializable{
	
	private LinkedList <ATreeNode> parents = new LinkedList <ATreeNode> ();
	private LinkedList <ATreeNode> children;
	private String name;
	
	public ATreeNode(String name){
		children = new LinkedList <ATreeNode>();
		this.name = name;
	}
	
	public boolean hasChildren(){
		if (children.size() != 0){
			return true;
		}
		else return false;
	}
	public String getName(){
		return name;
	}
	public void addChild(ATreeNode node){
		node.addParent(this);
		children.add(node);
	}
	
	public void removeChild(ATreeNode node){
		try{
			node.removeParent(this);
		} catch (Exception e){ e.printStackTrace(); }
		children.remove(node);
	}
	
	public void removeParent(ATreeNode node) {
			parents.remove(node);
	}
	
	public void addParent(ATreeNode node){
		parents.add(node);
	}

	public LinkedList<ATreeNode> getChildren(){
		return children;	
	}

	public LinkedList<ATreeNode> getParents() {
		return parents;
	}

}
