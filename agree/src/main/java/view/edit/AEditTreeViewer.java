package view.edit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import jsdai.dictionary.CReal_type;
import jsdai.dictionary.CString_type;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import model.AModelImpl;
import model.tree.structure.ATreeNode;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

import sdai.ASdaiHandler;
import view.box.attribute.ASelectAttribute;
import view.box.attribute.extra.AReturnInfo;
import view.box.attribute.extra.ASelectMethod;
import view.box.figure.AAttributeFigure;
import view.tree.display.AContentProvider;
import view.tree.display.ATreeViewer;
import view.tree.display.ATreeViewerNode;
import view.tree.display.AViewerSorter;

public class AEditTreeViewer extends ATreeViewer {
	
	private Map <String, ATreeViewerNode> entitySelectionMap = new HashMap <String,  ATreeViewerNode>();
	private AEditAttribute parentFigure;
	
	
	public AEditTreeViewer(final Composite c, final AEditAttribute parentFigure) {
		super(c, SWT.BORDER | SWT.H_SCROLL |SWT.V_SCROLL);
		this.parentFigure = parentFigure;
		setLabelProvider(new LabelProvider());				
		setContentProvider(new AContentProvider());
		setSorter(new AViewerSorter());	

		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		final DragSource sourceSet = new DragSource(this.getTree(), DND.DROP_MOVE |DND.DROP_COPY | DND.DROP_LINK);
		sourceSet.setTransfer(types);
		sourceSet.addDragListener(new DragSourceListener(){
			
			@Override
			public void dragStart(DragSourceEvent event) {
		        TreeItem[] selection = getTree().getSelection();
		        if (selection.length > 0) {
		          event.doit = true;
		          parentFigure.setDragItem(selection[0]);
		        } else {
		          event.doit = false;
		        }
				
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				if (event.detail == DND.DROP_MOVE){
				//	event.data = parent.getDragItem().getText();
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = parentFigure.getDragItem().getText();				
			}
			   
		   });

	}

	public ATreeViewerNode buildTree(AAttributeFigure figure) {
		ATreeViewerNode tRoot = new ATreeViewerNode(null);
		ATreeViewerNode root = new ATreeViewerNode(tRoot);
		root.setAsRoot();
		tRoot.addChild(root);
		root.setText(figure.getAttribute().getReturnInfo().returntype);		
		
		AReturnInfo info = figure.getAttribute().getReturnInfo(); 
		setData(info.returntype, root);
		
		
		if (figure.getAttribute() instanceof ASelectAttribute){
		 	entitySelectionMap.put(figure.getAttribute().getReturnInfo().returntype, root);
	    	for (Class c: figure.getAttribute().getReturnInfo().returnclasses){
	    		buildTree(root, c);
	    	}
			Map<String, ATreeViewerNode> abstractSelects = new HashMap <String, ATreeViewerNode>();
			ATreeViewerNode parent;
	    	//		addAbstractSelects(parent, info);
		    for (Class c: info.returnclasses){
		    	if (info.selectMap.containsKey(c)){
		    		String name = info.selectMap.get(c);
		    		if (!abstractSelects.containsKey(name)){
		    			if (!root.toString().equals(name)){
				    		parent = new ATreeViewerNode(root);
				    		parent.setText(name);
				    		abstractSelects.put(name, parent);
		    			}
		    			else {
		    				parent = root;
		    				abstractSelects.put(name, parent);
		    			}
		    		}
		    		else parent = abstractSelects.get(name);
		    	}
		    	else parent = root;				    			
		    			
		    	// Makes sure that select types are added before the entities 
		    	// that are below in tree
		    	addAbstractSelects(parent, info);
		    	ATreeViewerNode item;
		    	if (c.getSimpleName().substring(1).equals(root.toString())){
		    		item = parent;
		    	}
		    	else {
					item = new ATreeViewerNode(parent);
					item.setText(c.getSimpleName().substring(1));
		    	}
				entitySelectionMap.put(c.getSimpleName().substring(1), item);
				if (c != CReal_type.class && c != CString_type.class)
					buildTree(item, c);
		    	}	
		    }
		   else{
			   entitySelectionMap.put(figure.getAttribute().getReturnInfo().returntype, root);
			   if (info.returnclasses.size() == 1 && info.returnclasses.iterator().next().getSimpleName().substring(1).equals(info.returntype)){
				   tRoot.removeChild(root);
				   buildTree(tRoot, info.returnclasses.iterator().next());
			   }
			   else {;
				   for (Class c: figure.getAttribute().getReturnInfo().returnclasses){		   
					   buildTree(root, c);
				   }
			   }
		   }
			//    tRroot.setExpanded(true);
		return tRoot;
	}

	private void addAbstractSelects(ATreeViewerNode parent, AReturnInfo info) {
		// TODO Auto-generated method stub
		
	}

	private void buildTree(ATreeViewerNode root, Class c) {
		try {
			Class cClass = Class.forName(c.getName().replace(c.getSimpleName(), "C".concat(c.getSimpleName().substring(1))));
			if (AModelImpl.getTreeNode(cClass) != null){
				ATreeNode tn = AModelImpl.getTreeNode(cClass);
				ATreeViewerNode nItem = new ATreeViewerNode(root);
				nItem.setText(tn.getName());
				setData(tn.getName(), nItem);
				root.addChild(nItem);
				if (tn.hasChildren())
					recursiveBuild(nItem, tn, "");
			}
			else {
				// Select!
			}

		} catch (ClassNotFoundException e) { e.printStackTrace(); }
	
	}
	
	public void recursiveBuild(ATreeViewerNode parent, ATreeNode node, String debug){
		Set<String> children = new HashSet<String>();	
		if (node.hasChildren()){
			for (ATreeNode n: node.getChildren()){
				if (! children.contains(n.getName())){
					ATreeViewerNode nItem = new ATreeViewerNode(parent);
					nItem.setText(n.getName());
					setData(n.getName(), nItem);
					parent.addChild(nItem);
					children.add(n.getName());
					recursiveBuild(nItem, n, debug.concat("\t"));
				}	

			}
		}
	}
	
	public ATreeViewerNode getNode(String s){
		return entitySelectionMap.get(s);
	}
	


}
