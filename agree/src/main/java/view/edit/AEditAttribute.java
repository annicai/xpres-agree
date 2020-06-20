package view.edit;

import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import jsdai.lang.*;
import model.AModelImpl;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import actions.AAction;
import actions.ASetAction;
import sdai.ASdaiHandler;
import view.ALayout;
import view.box.attribute.*;
import view.box.figure.AEntityFigure;
import view.edit.table.ATableFilter;
import view.edit.table.ATableInstance;
import view.edit.table.ATableItem;
import view.edit.table.ATableSeparator;
import view.tree.display.*;
import controller.*;

public class AEditAttribute {
	
	private Shell shell;
	private Set <Class> instanceClasses = new HashSet<Class>();
	private boolean setSource = false;
	private Item dragItem;
	private AEntityFigure figure;
	private TableViewer tvAvailable, tvAvailableFind, tvSet, tvSetBack;
	private List<ATableInstance> itemsAvailable = new ArrayList<ATableInstance>();
	private List<ATableItem> itemsSet = new ArrayList<ATableItem>();
	private Map<String, ATableItem> indexMap = new HashMap<String, ATableItem>();
	private TabFolder tabs;
	private boolean mAggregate = false;
	
	public AEditAttribute(final Display display, final AControllerImpl controller, final AEntityFigure figure){
		   this.figure = figure; 
		   
		   if (figure.getAttribute().getReturnInfo().aggLevel >1)
			   mAggregate = true;
		   
		   shell = new Shell(display, SWT.DIALOG_TRIM | SWT.RESIZE);
		   shell.setText("Edit - ".concat(figure.getAttribute().getName()));
		   shell.setLayout(new FillLayout());
		   shell.setBackground(ColorConstants.white);
		   shell.setImage(controller.getView().getShell().getImage());
		   AState.setActiveObject(shell);
		   
		   tabs = new TabFolder(shell, SWT.NONE);
		   
		   TabItem settings = new TabItem(tabs,  SWT.NULL);
		   settings.setText("Filter");
		   
		   TabItem item = new TabItem(tabs, SWT.NULL);
		   item.setText("Existing instances");
		   
		   TabItem createItem = new TabItem(tabs, SWT.NULL);
		   createItem.setText("New instances");
		   
		   tabs.setSelection(1);
		   
		   Composite setAttributesBackground = new Composite(tabs, SWT.NONE);
		   item.setControl(setAttributesBackground);
		   setAttributesBackground.setLayout(new FillLayout());
		   
		   Composite filter = new Composite(tabs, SWT.NONE);
		   filter.setLayout(new FillLayout());
		   
		   
		   Composite filterTable = new Composite(filter, SWT.NONE);
		   filterTable.setLayout(new FillLayout());
		   
		   final Composite filterOptions = new Composite(filter, SWT.NONE);
		   filterOptions.setLayout(new GridLayout());

		   
		   Label label = new Label(filterOptions, SWT.NONE);
		   label.setText("Filter by entiy class:");
		   
		   final Combo combo = new Combo(filterOptions, SWT.NONE);
		   combo.add( "-- SHOW ALL -- ");
		   
		   final Label alabel = new Label(filterOptions, SWT.NONE);
		   alabel.setText("Filter by (string) attribute:");
		   alabel.setVisible(false);
		   
		   final Combo attCombo = new Combo(filterOptions, SWT.NONE);
		   attCombo.setVisible(false);
		   
		   final Label nlabel = new Label(filterOptions, SWT.NONE);
		   nlabel.setVisible(false);
		   
		   final Text text = new Text(filterOptions, SWT.SINGLE| SWT.BORDER);
		   
		   tvAvailableFind = new TableViewer(filterTable, SWT.MULTI | SWT.H_SCROLL
				      | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		   tvAvailableFind.setContentProvider(new ArrayContentProvider());
		   
		   tvAvailable = new TableViewer(setAttributesBackground, SWT.MULTI | SWT.H_SCROLL
				      | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		   tvAvailable.setContentProvider(new ArrayContentProvider());
	//	   tvAvailable.setSorter(new AEditSorter());
		   
		   tvSet = new TableViewer(setAttributesBackground, SWT.MULTI | SWT.H_SCROLL
				      | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		   
		   tvSet.getTable().setBackground(new Color(display, 153,255,153));
		   
		   tvSet.setContentProvider(new ArrayContentProvider());
		   
		   Composite composite = new Composite(tabs, SWT.NONE);
		   createItem.setControl(composite);
		   composite.setLayout(new FillLayout());

		   final AEditTreeViewer treeViewer = new AEditTreeViewer(composite, this);

		   ATreeViewerNode node = treeViewer.buildTree(figure);
		   treeViewer.setInput(node);
		   treeViewer.expandToLevel(2);
		   treeViewer.refresh();
		   
		   tvSetBack = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL
				      | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		   tvSetBack.getTable().setBackground(new Color(display, 153,255,153));
		   tvSetBack.setContentProvider(new ArrayContentProvider());
		   
		   
		   text.addModifyListener(new ModifyListener(){

			@Override
			public void modifyText(ModifyEvent arg0) {	
				if (tvAvailableFind.getFilters() != null){
					for (ViewerFilter f: tvAvailableFind.getFilters()){
						if (f instanceof AAttributeFilter){
							((AAttributeFilter) f).setFilterString(text.getText());
						}
					}
				}
				refresh();
			}
			   
		   });
		   text.setText("");
		   text.setVisible(false);
		   
		   attCombo.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg) {}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String item = attCombo.getItem(attCombo.getSelectionIndex());
				Method m = (Method) attCombo.getData(item);
				if (m != null){
					AAttributeFilter filter = new AAttributeFilter(m, "");
					tvAvailableFind.addFilter(filter);
					tvAvailable.addFilter(filter);
					text.setVisible(true);
					nlabel.setText(item.concat(" (contains) :"));
					nlabel.setVisible(true);
					filterOptions.layout();
				}
				else {
					for (ViewerFilter f: tvAvailable.getFilters()){
						tvAvailable.removeFilter(f);
						tvAvailableFind.removeFilter(f);
						
					}
					nlabel.setVisible(false);
					text.setVisible(false);
					text.setText("");
				}
				
			}
			   
		   });
		   
		   
		   combo.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String item = combo.getItem(combo.getSelectionIndex());
				Class clazz = (Class) combo.getData(item);
				for (int i=0; i < tvAvailable.getFilters().length; i++){
					tvAvailable.removeFilter(tvAvailable.getFilters()[i]);
					tvAvailableFind.removeFilter(tvAvailableFind.getFilters()[i]);	
				}
				if (clazz != null){
					ATableFilter filter = new ATableFilter(clazz);
					tvAvailable.addFilter(filter);
					tvAvailableFind.addFilter(filter);
					alabel.setVisible(true);
					List<Method> methods= ASdaiHandler.getStringAttributes(clazz);
					attCombo.removeAll();
					for (Method m: methods){
						attCombo.add(m.getName().substring(3));
						attCombo.setData(m.getName().substring(3), m);
					}
					attCombo.setVisible(true);
				}
				else {
					alabel.setVisible(false);
					attCombo.setVisible(false);
				}
				nlabel.setVisible(false);
				text.setVisible(false);
				text.setText("");
				refresh();
			}
			   
		   });;		   
		   
		   settings.setControl(filter);		   

		   ALayout.center(shell);
		   

		   Set<EEntity> set= markSetEntities();
		   addPossibleEntities(combo, set);

		   tvAvailable.setInput(itemsAvailable);
		   tvAvailableFind.setInput(itemsAvailable);
		   tvSet.setInput(itemsSet);
		   tvSetBack.setInput(itemsSet);

		   int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
		   Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		   
		   final DragSource sourceSet = new DragSource(tvSet.getTable(), operations);
		   sourceSet.setTransfer(types);
		   sourceSet.addDragListener(new DragSourceListener(){

			@Override
			public void dragStart(DragSourceEvent event) {
		        TableItem[] selection = tvSet.getTable().getSelection();
		        if (selection.length > 0) {
		          event.doit = true;
		          setDragItem(selection[0]);
		          setSource = true;
		        } else {
		          event.doit = false;
		        }
		       	
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = getDragItem().getText(); //table.getData(dragItem);
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
		        if (event.detail == DND.DROP_MOVE){
//		        	if (! disposeItem.isDisposed() && disposeItem != null)
//		        		disposeItem.dispose();
		        }
//		        disposeItem = null;
		        refresh();
			}
			   
		   });
		   DropTarget targetSet = new DropTarget(tvSet.getTable(), operations);
		   targetSet.setTransfer(types);
		   
		   DropTargetAdapter dropAdapter = new DropTargetAdapter(){
			   
			      public void dragOver(DropTargetEvent event) {
				        event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
				        if (event.item != null) {
				          TableItem item = (TableItem) event.item;
				          Point pt = display.map(null, tvSet.getTable(), event.x, event.y);
				          Rectangle bounds = item.getBounds();
				          if (pt.y < bounds.y + bounds.height / 3) {
				            event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
				          } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
				            event.feedback |= DND.FEEDBACK_INSERT_AFTER;
				          } else {
				            event.feedback |= DND.FEEDBACK_SELECT;
				          }
				     }
				  }
			      
			      public void drop(DropTargetEvent event) { 
			    	  try {
				    	  	EEntity entity = null;
					        if (getDragItem() == null || dragItem.getData() instanceof ATableSeparator) {
					          event.detail = DND.DROP_NONE;
					          return;
					        }

					        if (dragItem instanceof TreeItem){
					        	setSource = false;
					        	ATreeViewerNode node = (ATreeViewerNode) AModelImpl.getTreeViewerNode(dragItem.getText());
					        	if (node instanceof AInstanceNode)
					        		entity = AModelImpl.getEntity((AInstanceNode)node);
					        	else {
						        	entity = controller.createInstance(dragItem.getText(), true);
						        	if (entity == null){
						        		setDragItem(null);
						        		return;
						        	}

						        	Class clazz = entity.getClass();
									if (! instanceClasses.contains(clazz)){
										instanceClasses.add(clazz);
										combo.add(clazz.getSimpleName().substring(1).toUpperCase());
										combo.setData(clazz.getSimpleName().substring(1).toUpperCase(), clazz);
									}
					        	}
					        }
					        else if (dragItem.getData() instanceof ATableInstance){
					        	entity = ((ATableInstance) dragItem.getData()).getEntity();
					        }
					        boolean set = true;
					        if (setSource && mAggregate){		
							    	Point pt = display.map(null, tvSet.getTable(), event.x, event.y);
							    	String item = tvSet.getTable().getItem(pt).getText();

						        	ATableItem neighbourItem = indexMap.get(item);
						        	int idx = itemsSet.indexOf(neighbourItem);
						        	if (idx < 0){
						        		return;
						        	}
						        	
						        	ATableItem moveItem = indexMap.get(dragItem.getText());
						        	((AAAttribute)figure.getAttribute()).setIndex(moveItem.getIndex());
						        	figure.unsetAttribute(entity);
						        	

						        	moveItem.setIndex(neighbourItem.getIndex());
						        	itemsSet.remove(moveItem);
						        	if (itemsSet.size() < idx + 1)
						        		itemsSet.add(moveItem);
						        	else itemsSet.add(idx+1, moveItem);
						        	
						        	 ((AAAttribute)figure.getAttribute()).setIndex(neighbourItem.getIndex());
						        	
						        	figure.setAttributeValue(entity);
						        
					        }
					        else {
					        	if (figure.getAttribute().isAggregate()){
									AAction action = new ASetAction(entity,  figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
									controller.addToUndoStack(action);
					        	} else {
									AAction action = new ASetAction(figure.getAttribute().getAttributeValue(),  figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
									controller.addToUndoStack(action);
					        	}

					        	if (mAggregate){
					        		String item = null;
					        		if (tabs.getSelectionIndex() == 1){
								    	  Point pt = display.map(null, tvSet.getTable(), event.x, event.y);
								    	  TableItem ti = tvSet.getTable().getItem(pt);
								    	  if (ti != null)
								    		  item = ti.getText();
					        		}
					        		else {
								    	  Point pt = display.map(null, tvSetBack.getTable(), event.x, event.y);
								    	  TableItem ti = tvSetBack.getTable().getItem(pt);
								    	  if (ti!= null)
								    		  item = ti.getText();
					        		}

							    	int index = 1;	// Aggregate index
							    	int tableIdx = 1;		// Table index
							    	if (item != null){
								    	  ATableItem neighbourItem = indexMap.get(item);
								    	  if (neighbourItem != null){
								    		  index = neighbourItem.getIndex();	
								    		  tableIdx = itemsSet.indexOf(neighbourItem);
								    	  }
							    	}
							    	  
							    	((AAAttribute)figure.getAttribute()).setIndex(index);
							    	set = figure.setAttributeValue(entity);
								    if (set){
								    		ATableInstance ti = new ATableInstance(entity);
								    		ti.setIndex(index);
								    		indexMap.put(ti.toString(), ti);
								    		if (itemsSet.size() < tableIdx + 1)
								    			itemsSet.add(ti);
								    		else itemsSet.add(tableIdx + 1, ti);
								    }
					        	}
					        	else {
					        		set = figure.setAttributeValue(entity);
					        		
							        if (! figure.getAttribute().isAggregate()) {
							        	if (itemsSet.size() > 0){
							        		ATableInstance ti = (ATableInstance) itemsSet.get(0);
							        		itemsAvailable.add(ti);
							        		itemsSet.remove(ti);
							        	}
							        }
							        if (set){
							    		ATableInstance ti = new ATableInstance(entity);
							    		itemsSet.add(ti);
							        }
					        	}
					       
						        
						        if (!(dragItem instanceof TreeItem) && dragItem.getData() instanceof ATableInstance){
						        	itemsAvailable.remove(dragItem.getData());
						        }
					        }

					        
					        if (mAggregate && itemsSet.get(itemsSet.size()-1) instanceof ATableInstance){
					        	ATableItem ti = itemsSet.get(itemsSet.size()-1);
					        	ATableSeparator separator = new ATableSeparator(ti.getIndex() + 1);
					        	itemsSet.add(separator);
					        	indexMap.put(separator.toString(), separator);
					        }
					        
					        setDragItem(null);
			        		refresh();
			       

			    	  } catch (NullPointerException e){
			    		  e.printStackTrace();
			    	  } catch (Exception e){
			    		  e.printStackTrace();
			    	  }
			    	  	
			      }
			      
		   };
		   targetSet.addDropListener(dropAdapter);
		   
		   DropTarget aI2targetSet = new DropTarget(tvSetBack.getTable(), operations);
		   aI2targetSet.setTransfer(types);
		   aI2targetSet.addDropListener(dropAdapter);
		   
		   final DragSource sourceUnset = new DragSource(tvAvailable.getTable(), operations);
		   sourceUnset.setTransfer(types);
		   sourceUnset.addDragListener(new DragSourceListener(){

			@Override
			public void dragStart(DragSourceEvent event) {
		       TableItem[] selection = tvAvailable.getTable().getSelection();
		        if (selection.length > 0) {
		          event.doit = true;
		          setDragItem(selection[0]);
		          setSource = false;
		        } else {
		          event.doit = false;
		        }
		       
				
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = getDragItem().getText();
				
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				tvAvailable.refresh();
			}
		   });
		   
		   
		   DropTarget lowerTarget = new DropTarget(tvAvailable.getTable(), operations);
		   lowerTarget.setTransfer(types);
		   lowerTarget.addDropListener(new DropTargetAdapter() {
			   
			      public void dragOver(DropTargetEvent event) {
				        event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
				        if (event.item != null) {
				          TableItem item = (TableItem) event.item;
				          Point pt = display.map(null, tvAvailable.getTable(), event.x, event.y);
				          Rectangle bounds = item.getBounds();
				          if (pt.y < bounds.y + bounds.height / 3) {
				            event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
				          } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
				            event.feedback |= DND.FEEDBACK_INSERT_AFTER;
				          } else {
				            event.feedback |= DND.FEEDBACK_SELECT;
				          }
				     }
				  }
			      
			      public void drop(DropTargetEvent event) {
				        if (getDragItem() == null || dragItem.getData() instanceof ATableSeparator) {
				          event.detail = DND.DROP_NONE;
				          return;
				        }
				        ATableInstance droppedInstance = (ATableInstance) dragItem.getData();
				        EEntity entity = droppedInstance.getEntity();
				        
				        ATableInstance titem = new ATableInstance(entity);
				        itemsAvailable.add(titem);
				        
				        if (setSource){
				        	AAttribute a = figure.getAttribute();
				        	if (a instanceof AAAttribute){
				        		if (a.getReturnInfo().aggLevel > 1){
				        			int idx = droppedInstance.getIndex();
				        			((AAAttribute)figure.getAttribute()).setIndex(idx);
				        		}
				        		figure.unsetAttribute(entity);
				        	}
				        	else figure.unsetAttribute(null);
				        	
				        	AAction action = new ASetAction(entity,  figure.getBox().getEntityRepresentation(), figure.getAttribute().getName());
				        	controller.addToUndoStack(action);
				        	itemsSet.remove(droppedInstance);
				        }
				        else itemsAvailable.remove(dragItem.getData());

				        setDragItem(null);

			      }
			      
			   
		   });
		   
		   shell.setSize(650,350);

		   tvSet.refresh();
		   tvAvailable.refresh();
		   
		   if (itemsAvailable.size() == 0){
			   tabs.setSelection(2);
		   }
		   shell.redraw();
		   shell.open();
		   new Runnable(){

			@Override
			public void run() {
				   while (!shell.isDisposed()) {
						if (!display.readAndDispatch()) {
							display.sleep();
						}
				   }
			}
			   
		   };

	}


	private void addPossibleEntities(Combo combo, Set<EEntity> setEntities) {
		Set<EEntity> usedEntities = new HashSet<EEntity>();
		for (Class c: figure.getAttribute().getReturnInfo().returnclasses) {	 
			LinkedList<EEntity> entities = ASdaiHandler.findAllInstances(c);
			for (EEntity entity: entities){
				if (!usedEntities.contains(entity)){
					if (! setEntities.contains(entity)){
						ATableInstance titem = new ATableInstance(entity);
						itemsAvailable.add(titem);
					}
					usedEntities.add(entity);
				}
				Class clazz = entity.getClass();
				if (! instanceClasses.contains(clazz)){
					instanceClasses.add(clazz);
					combo.add(clazz.getSimpleName().substring(1).toUpperCase());
					combo.setData(clazz.getSimpleName().substring(1).toUpperCase(), clazz);
				}
			}
			
		}
	}


	private Set<EEntity> markSetEntities() {
		Set<EEntity> eset = new HashSet<EEntity>();
		try{
			Object retur = figure.getAttribute().getAttributeValue(); 
			if (mAggregate){
				if (retur != null){	
					SdaiIterator iterator = ((CAggregate) retur).createIterator();
					int idx = 1;
					while(iterator.next()){
						ATableSeparator ts = new ATableSeparator(idx);
						itemsSet.add(ts);
						indexMap.put(ts.toString(), ts);
						Object current = ((CAggregate) retur).getCurrentMemberObject(iterator);
						eset.addAll(addFromAggregate(current, idx));
						idx ++;
					}
					ATableSeparator ts = new ATableSeparator(idx);
					itemsSet.add(ts);
					indexMap.put(ts.toString(), ts);
				}
				else {
					ATableSeparator ts = new ATableSeparator(1);
					itemsSet.add(ts);
					indexMap.put(ts.toString(), ts);
				}
			} else {
				if (retur != null){	
					if (retur instanceof AEntity || (retur instanceof CAggregate) ){
						eset.addAll(addFromAggregate(retur, 0));
					}
					else {
						EEntity entity = (EEntity) retur;
						ATableInstance ti = new ATableInstance(entity);
						itemsSet.add(ti);
						eset.add(entity);
					}
				}
			}
		} catch (ClassCastException e) {e.printStackTrace(); } 
		catch (SdaiException e) { e.printStackTrace();
		}

		return eset;
	}
	
	private Set<EEntity> addFromAggregate(Object retur, int i) {
		Set<EEntity> eset = new HashSet<EEntity>();
		SdaiIterator iterator;
		if (retur instanceof AEntity){
			AEntity aentity = ((AEntity) retur);
			try {
				iterator = aentity.createIterator();
				while (iterator.next()){
					EEntity entity = aentity.getCurrentMemberEntity(iterator);
					ATableInstance ti = new ATableInstance(entity);
					ti.setIndex(i);
					itemsSet.add(ti);
					indexMap.put(ti.toString(), ti);
					eset.add(entity);
				}
			} catch (SdaiException e1) { e1.printStackTrace(); }
		}
		else {
			try {
				Aggregate entities = ((Aggregate) retur);
				iterator = entities.createIterator();
				while (iterator.next()){
					EEntity entity = (EEntity) entities.getCurrentMemberObject(iterator);
					ATableInstance ti = new ATableInstance(entity);
					itemsSet.add(ti);
					indexMap.put(ti.toString(), ti);
					eset.add(entity);
				}
			} catch (SdaiException e1) { e1.printStackTrace(); }
		}
		return eset;
	}


	private void refresh(){
		tvAvailable.refresh();
		tvAvailableFind.refresh();
		tvSet.refresh();
		tvSetBack.refresh();
		
	}
	
	public void focus(){
		shell.forceActive();
		shell.forceFocus();
	}
	
	public void close(){
		shell.dispose();
	}

	public Item getDragItem() {
		return dragItem;
	}

	public void setDragItem(Item dragItem) {
		this.dragItem = dragItem;
	}
	
}
