package model.tree.structure;

public class AWaitingNode extends ATreeNode{

	private Class superClazz;
	private Class thisClazz;

	
	public AWaitingNode(String name, Class superClazz, Class thisClazz) {
		super(name);
		this.superClazz = superClazz;
		this.thisClazz = thisClazz;
	}
	
	public Class getThisClass(){
		return thisClazz;
	}
	
	public Class getSuperClass(){
		return superClazz;
	}

}
