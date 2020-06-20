package view.tree.display;

public class AInstanceNode extends ATreeViewerNode{

	public AInstanceNode(ATreeViewerNode parent) {
		super(parent);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AInstanceNode))
			return false;
		if (obj == this)
			return true;
		if (((AInstanceNode)obj).toString().equals(toString()))
			return true;
		return false;
	}
	
}
