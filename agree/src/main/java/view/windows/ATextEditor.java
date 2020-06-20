package view.windows;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jsdai.lang.AEntity;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import exceptions.AEntitiesNotFoundException;
import sdai.ASdaiHandler;
import view.AViewImpl;
import xml.AXMLParser;


public class ATextEditor implements MouseListener {
	
	private Map<String, TreeItem> itemMap = new HashMap<String, TreeItem>();
	private Tree tree;
	private List implicitList, refList;
    private Map <Integer, EEntity> map = new HashMap<Integer, EEntity>();
    private TreeItem disposeItem;
    private LinkedList<TreeItem> allItems;

	public ATextEditor(final Display display, Image image, final AViewImpl view) { 

		final Shell shell = new Shell(display);
	    shell.setText("Text editor");
	    shell.setLayout(new FillLayout());
		shell.setImage(image);
	    tree = new Tree(shell, SWT.BORDER);
	    tree.addMouseListener(this);
	    Composite composite = new Composite(shell, SWT.NONE);
	    composite.setLayout(new GridLayout());
	    Group group1 = new Group(composite, SWT.NONE);
	    group1.setText("Referenced by...");
	    group1.setLayout(new FillLayout());
	    group1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	    Group group2 = new Group(composite, SWT.NONE);
	    group2.setText("Referencing...");
	    group2.setLayout(new FillLayout());
	    group2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	    
	    implicitList = new List(group1, SWT.BORDER);
	    int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
	    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
	    
	    final DragSource source1 = new DragSource(implicitList, operations);
	    source1.setTransfer(types);

	    source1.addDragListener(new DragSourceListener() {
		    String dragItem;
		    
			@Override
			public void dragStart(DragSourceEvent event) {
		        String[] selection = implicitList.getSelection();
		        if (selection.length > 0) {
		          event.doit = true;
		          dragItem = selection[0];
		        } else {
		          event.doit = false;
		        }
				
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = implicitList.getData(dragItem);
				
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
		        if (event.detail == DND.DROP_MOVE){
		        	if (disposeItem != null && ! disposeItem.isDisposed())
		        		disposeItem.dispose();
		        }
		        disposeItem = null;
				
			}
	    	
	    });
	    
	    refList = new List(group2, SWT.BORDER);
	    LinkedList<Integer> list = new LinkedList<Integer>();
	    
	    final DragSource source2 = new DragSource(refList, operations);
	    source2.setTransfer(types);

	    source2.addDragListener(new DragSourceListener() {
		    String dragItem;
		    
			@Override
			public void dragStart(DragSourceEvent event) {
		        String[] selection = refList.getSelection();
		        if (selection.length > 0) {
		          event.doit = true;
		          dragItem = selection[0];
		        } else {
		          event.doit = false;
		        }
				
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = refList.getData(dragItem);
				
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
		        if (event.detail == DND.DROP_MOVE){
		        	disposeItem.dispose();
		        }
				
			}
	    	
	    });
	    
	    AEntity step = ASdaiHandler.getEntities();
	    try {
	    	SdaiIterator iterator = step.createIterator();
	    	while (iterator.next()){
	    		int index = Integer.parseInt(((EEntity)step.getCurrentMemberEntity(iterator)).getPersistentLabel().substring(1));
	    		map.put(index, ((EEntity)step.getCurrentMemberEntity(iterator)));
	    		list.add(index);
	    	}
	    } catch (Exception e){
	    }
	    Collections.sort(list);
	    
	    for (Integer i: list){
    		TreeItem item = new TreeItem(tree, SWT.NONE);
    		EEntity entity = map.get(i);
    		item.setText(entity.toString());
    		item.setData(entity.toString(), entity);
    		try {
				item.setData("label", entity.getPersistentLabel());
				itemMap.put(entity.getPersistentLabel(), item);
			} catch (SdaiException e) {
				e.printStackTrace();
			}
	    }

	    final DragSource source = new DragSource(tree, operations);
	    source.setTransfer(types);
	    final TreeItem[] sourceItem = new TreeItem[1];
	    source.addDragListener(new DragSourceListener() {
	      public void dragStart(DragSourceEvent event) {
	        TreeItem[] selection = tree.getSelection();
	        if (selection.length > 0) {
	          event.doit = true;
	          sourceItem[0] = selection[0];
	        } else {
	          event.doit = false;
	        }
	      };

	      public void dragSetData(DragSourceEvent event) {
	        event.data = sourceItem[0].getText();
	      }

	      public void dragFinished(DragSourceEvent event) {
	        if (event.detail == DND.DROP_MOVE){
	        	sourceItem[0].dispose();
	        }
	        sourceItem[0] = null;
	      }
	    });

	    DropTarget target = new DropTarget(tree, operations);
	    target.setTransfer(types);
	    target.addDropListener(new DropTargetAdapter() {
	      public void dragOver(DropTargetEvent event) {
	        event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
	        if (event.item != null) {
	          TreeItem item = (TreeItem) event.item;
	          Point pt = display.map(null, tree, event.x, event.y);
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
	        if (event.data == null || event.item == null) {	//08-17 ev.item ad
	          event.detail = DND.DROP_NONE;
	          return;
	        }
	        
            if (event.data.toString().length() < 10){

				sourceItem[0] = ((TreeItem)itemMap.get(event.data));
				disposeItem = sourceItem[0];
            }

	        String text = sourceItem[0].getText();
	        
	        if (true){	//event.item instanceof TreeItem
	          TreeItem dropItem = (TreeItem) event.item;
	          if (sourceItem[0].getText().equals(dropItem.getText())){
	        	  sourceItem[0] = null;
		          event.detail = DND.DROP_NONE;
	        	  return;
	        	  
	          }
	          
	          Point pt = display.map(null, tree, event.x, event.y);
	          Rectangle bounds = dropItem.getBounds();
	          TreeItem parent = dropItem.getParentItem();
	          if (parent != null) {
	            TreeItem[] items = parent.getItems();
	            int index = 0;
	            for (int i = 0; i < items.length; i++) {
	              if (items[i] == dropItem) {
	                index = i;
	              }
	            }
	            TreeItem newItem;
	            if (pt.y < bounds.y + bounds.height / 3) {
	              newItem = new TreeItem(parent, SWT.NONE, index);
	            } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
	              newItem = new TreeItem(parent, SWT.NONE, index + 1);
	            } else {
	              newItem = new TreeItem(dropItem, SWT.NONE);
	            }
	            newItem.setText(text);
	    		newItem.setData(text, sourceItem[0].getData(text));

		        newItem.setData("label", sourceItem[0].getData("label"));
	    		
	    		
				itemMap.put((String) sourceItem[0].getData("label"), newItem);
				
				if (!addSubItems(newItem, sourceItem[0].getItems())){
	        		event.detail = DND.DROP_NONE;
	        		newItem.dispose();
				}
	          } else {
	            TreeItem[] items = tree.getItems();
	            int index = 0;
	            for (int i = 0; i < items.length; i++) {
	              if (items[i] == dropItem) {
	                index = i;
	                break;
	              }
	            }
	            TreeItem newItem;
	            if (pt.y < bounds.y + bounds.height / 3) {
	              newItem = new TreeItem(tree, SWT.NONE,  index);
	            } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
	              newItem = new TreeItem(tree, SWT.NONE, index + 1);
	            } else {
	              newItem = new TreeItem(dropItem, SWT.NONE);
	            }
	            newItem.setText(text);
	    		newItem.setData(text, sourceItem[0].getData(text));
	    		
	            newItem.setData("label", sourceItem[0].getData("label"));
				itemMap.put((String)sourceItem[0].getData("label"), newItem);
				
				if (!addSubItems(newItem, sourceItem[0].getItems())){
	        		event.detail = DND.DROP_NONE;
	        		newItem.dispose();
				}

	           }
	        }
	      }
	    });
	    Composite buttonComposite = new Composite(composite, SWT.NONE);
	    GridLayout grid2 = new GridLayout(SWT.CENTER, true);
	    grid2.numColumns = 2;
	    buttonComposite.setLayout(grid2);
	    final Button button = new Button(buttonComposite, SWT.NONE);
	    button.setText("Save as new STEP");
	    button.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
	    
	    final Button update = new Button(buttonComposite, SWT.NONE);
	    update.setText("Update current file");
	    update.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
	    

	    Listener buttonListener = new Listener(){

			@Override
			public void handleEvent(Event event) {
				String selected = "tempiii.stp";
     	        StringBuilder xmlString = new StringBuilder();
     	        
				if (event.widget == button){
	       			FileDialog fd = new FileDialog(shell, SWT.SAVE);
	     	        fd.setText("Save as");
	     	        String[] filterExt = { "*.stp" };
	     	        fd.setFilterExtensions(filterExt);
	     	        selected = fd.open();
	     	        if (selected == null)
	     	        	return;
				}
				else {
	    			try {
	         	        File xmlFile = 	AXMLParser.createXML("tempXml.xml", view.getContents(), view.getPaintContents());	//CONTENTS RIGHT?
	        			BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
	        			String nextLine = null;
						while (( nextLine = reader.readLine()) != null){
						      xmlString.append(nextLine);
						      xmlString.append(System.getProperty("line.separator"));
						}
		    		    reader.close();
		     	        
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
     	      
				String xmlAsString = xmlString.toString();
    		    
				String string = "";
				TreeItem[] items = tree.getItems();
				allItems = new LinkedList<TreeItem>();
				for(TreeItem i: items){
					allItems.addLast(i);
					string = string.concat(i.getText()).concat(File.separator);
					if (i.getItemCount()> 0){
						string = string.concat(addChildren(i)).concat(File.separator);	
					}
				}
				String split[] = string.split("\\".concat(File.separator));
				int nl = 0;
				for (int i=0; i< split.length; i++){
					try {
						while (split[i].length() < 2){
							i++;
							nl++;
						}
						String idx = Integer.toString(i+1-nl);
						string = string.replaceAll("#".concat(idx).concat("="), "#old".concat(idx).concat("="));
						string = string.replaceAll("#".concat(idx).concat(","), "#old".concat(idx).concat(","));
						string = string.replaceAll("#".concat(idx).concat("\\)"), "#old".concat(idx).concat(")"));
						string = string.replaceAll("#".concat(idx).concat("]"), "#old".concat(idx).concat("]"));
						if (itemMap.containsKey("#".concat(idx))){
							itemMap.get("#".concat(idx)).setData("label", "#old".concat(idx));
						}
						String replaceMe = allItems.get(i-nl).getData("label").toString();
						string = string.replaceAll(replaceMe.concat("="), "#".concat(idx).concat("="));
						string = string.replaceAll(replaceMe.concat(","), "#".concat(idx).concat(","));
						string = string.replaceAll(replaceMe.concat("\\)"), "#".concat(idx).concat(")"));
						string = string.replaceAll(replaceMe.concat("]"), "#".concat(idx).concat("]"));
					
						String xmlId = replaceMe.substring(1);
						if (event.widget == update){
							xmlAsString = xmlAsString.replace("id=\"".concat(idx).concat("\""), "id=\"old".concat(idx).concat("\""));
							xmlAsString = xmlAsString.replace("from=\"".concat(idx).concat("\""), "from=\"old".concat(idx).concat("\""));		
							xmlAsString = xmlAsString.replace("to=\"".concat(idx).concat("\""), "to=\"old".concat(idx).concat("\""));	
//			Pattern.compile(items[i].getData("label").toString().concat("[^0-9]")).matcher(string).replaceAll("#".concat(Integer.toString(i+1)));
							xmlAsString = xmlAsString.replace("id=\"".concat(xmlId).concat("\""), "id=\"".concat(idx).concat("\""));
							xmlAsString = xmlAsString.replace("from=\"".concat(xmlId).concat("\""), "from=\"".concat(idx).concat("\""));		
							xmlAsString = xmlAsString.replace("to=\"".concat(xmlId).concat("\""), "to=\"".concat(idx).concat("\""));	
						}
					}
					catch (Exception iob){
						iob.printStackTrace();
					}
				}
				try {
					string = string.replaceAll("\\\\", "\n");
					
					ASdaiHandler.saveModel(selected.replace(".stp", ""));
					
					
					BufferedReader input =  new BufferedReader(new FileReader(selected));
					String oldStep ="";
				      try {
				        String line = null;
				        while (( line = input.readLine()) != null){
				        	oldStep = oldStep.concat(line);
				        	oldStep = oldStep.concat(System.getProperty("line.separator"));
				        }
				      }
				      finally {
				    input.close();
				    }
				    int ind1 = oldStep.indexOf("#", 0); 
				    int ind2 = oldStep.indexOf("ENDSEC", ind1); 
				    string = oldStep.substring(0, ind1).concat(string);
				    string = string.concat(oldStep.substring(ind2, oldStep.length()));
					File file = new File(selected);
					FileWriter fw = new FileWriter(file);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write(string);
					bw.close();
					if (event.widget == update){
						File xmlFile = new File("xmltext.xml");
						FileWriter xmlFw = new FileWriter(xmlFile);
						BufferedWriter xmlBw = new BufferedWriter(xmlFw);
						xmlBw.write(xmlAsString);
						xmlBw.close();
						
						view.clean();
						
						view.openStepFile(selected);

						
						try {
							AXMLParser.readXML(xmlFile, view);
						} catch (AEntitiesNotFoundException e) {
							e.printStackTrace();
						}				
						
						file.delete();
						
						view.fitView(); 	// TODO: instead of fitView() restore last position
											// TODO: create a new thread for all this... 
					}

				} catch (IOException e) {	e.printStackTrace();
				}
			shell.dispose();
			}
			
	    
	    };
	    
	    update.addListener(SWT.Selection, buttonListener); 
	    button.addListener(SWT.Selection, buttonListener); 
	    
	    shell.setSize(800, 500);
	    shell.open();
	    while (!shell.isDisposed()) {
	      if (!display.readAndDispatch())
	        display.sleep();
	    }
	  
	}
	
	protected String addChildren(TreeItem i) {
		String string = "";
		for(TreeItem item: i.getItems()){
			allItems.addLast(item);
			string = string.concat(item.getText()).concat(File.separator);
			if (item.getItemCount()>0){
				string = string.concat(addChildren(item));
			}
		}
		return string.concat("");
	}

	public boolean addSubItems(TreeItem parent, TreeItem[] subItems){
        for (int i=0; i<subItems.length; i++){
    		TreeItem newItem = new TreeItem(parent, SWT.NONE);
    		newItem.setText(subItems[i].getText());
    		itemMap.put((String) subItems[i].getData("label"), newItem);
    		newItem.setData(subItems[i].getText(), subItems[i].getData(subItems[i].getText()));
	        newItem.setData("label", subItems[i].getData("label"));
    		addSubItems(newItem, subItems[i].getItems());
    		
        }
        return true;
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {	
		TreeItem item = tree.getItem(new Point(e.x, e.y));
		if (item != null && item.getData(item.getText()) instanceof EEntity) {
			implicitList.removeAll();
			EEntity entity = (EEntity) item.getData(item.getText());

     		AEntity storage = ASdaiHandler.findEntityInstanceUsers(entity);
     		try{
         		SdaiIterator iterator = storage.createIterator();
         		while (iterator.next()){
         			implicitList.add(storage.getCurrentMemberEntity(iterator).toString());
         			implicitList.setData(storage.getCurrentMemberEntity(iterator).toString(), storage.getCurrentMemberEntity(iterator).getPersistentLabel());
         		}
     		} catch (SdaiException ex){
     			ex.printStackTrace();
     		}

			implicitList.getParent().layout();
			
			refList.removeAll();
			
			String string = entity.toString().substring(5).replace("(", "").replace(")", "");
			String s[] = string.split("#[0-9]+");
			for (int i = 0; i < s.length; i++){
				string = string.replace(s[i], "text");
			}
			String s2[] = string.split("text");
			for (int i = 0; i < s2.length; i++){
				if (s2[i].length() > 1 && s2[i].matches("#[0-9]+")){
					refList.add(itemMap.get(s2[i]).getText());
					refList.setData(itemMap.get(s2[i]).getText(), s2[i]);
				}
			}
			
			refList.getParent().layout();
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {}

	@Override
	public void mouseUp(MouseEvent e) { }

}
