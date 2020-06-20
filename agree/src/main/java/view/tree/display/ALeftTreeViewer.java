package view.tree.display;

import java.util.LinkedList;

import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import model.AModelImpl;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;

import sdai.ASdaiHandler;
import view.box.AEntityBox;
import controller.AState;
import controller.AState.CursorMode;


public class ALeftTreeViewer extends ATreeViewer {


	public ALeftTreeViewer(Composite parent) {
		super(parent);
		setLabelProvider(new LabelProvider());	
		setContentProvider(new AContentProvider());
		setSorter(new AViewerSorter());	
		addListeners();
	}
	
	public void addListeners() {

		getTree().addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 127){			// DELETE
					String s = getSelection().toString().substring(1,getSelection().toString().length()-1);				
					LinkedList<ATreeViewerNode> nodes = AModelImpl.getTreeViewerNodes(s);
					
					for (ATreeViewerNode n: nodes){

						if (n instanceof AInstanceNode){
							EEntity entity = AModelImpl.getEntity((AInstanceNode) n);
							AEntityBox entitybox = (AEntityBox) AModelImpl.getEntityBox(entity);
							String entityString;

							entityString = ASdaiHandler.getPersistantLabel(entity);
					//			MessageBox box = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					//			box.setMessage("Do you really wish to delete entity instance " + entityString + " ?");
					//			box.setText("Delete");
					//			int response = box.open();
					//			if (response == SWT.YES){
						/*			if (entitybox != null)
										entitybox.hide();		//FIXME etc etc etc...
									if (model.buttonMapping.containsKey(entity)){
										LinkedList<GIButton> buttons = model.buttonMapping.get(entity);
										for (GIButton button: buttons){
											((GIEntityAttribute)button.getParent()).entities.remove(button);
											button.getParent().remove(button);
										}
										model.buttonMapping.remove(entity);
									}
									entity.deleteApplicationInstance();
									n.parent.children.remove(n); 
									model.entityMapping.remove(entityString);
									treeViewer.refresh();
								} */
							
						}
					}	
				}
			} 

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
			}
        	
        });

		try {
	        
	        addSelectionChangedListener(new ISelectionChangedListener(){

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					try{
						String s = getSelectionString();
						ATreeViewerNode n;
						if (AModelImpl.entityMapping.containsKey(s)){
							n = AModelImpl.entityMapping.get(s).getFirst();	
						}
						else {
							s = s.split("\\s")[0];
							if (AModelImpl.entityMapping.containsKey(s)){
								n = AModelImpl.entityMapping.get(s).getFirst();	
							}
							else return;
						}
						
						if (n instanceof AInstanceNode){
							AState.setCursorMode(CursorMode.DRAGDROP);
							AState.setActiveObject((AInstanceNode) n);
						}
						else if (!n.isRoot()){
							AState.setCursorMode(CursorMode.DRAGDROP);
							AState.setActiveObject(s);
						}
					
					} catch (Exception e){
						e.printStackTrace();
					}
				}

	        });            
	
		} catch (Exception e) {
			e.printStackTrace();
		}

        refresh();
		
	}
	

}
