package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Observable;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;

import controller.AState;
import exceptions.AAbstractEntityException;
import model.tree.structure.ATreeNode;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import sdai.ASdaiHandler;
import sdai.structure.ASchema;
import view.bendpoint.ABendPoint;
import view.bendpoint.AConnectionRouter;
import view.box.AEntityBox;
import view.box.attribute.extra.AButton;
import view.box.figure.AAttributeFigure;
import view.box.figure.AEntityFigure;
import view.tree.display.AInstanceNode;
import view.tree.display.ATreeViewerNode;

public class AModelImpl extends Observable{
	
	// Map between entity node and corresponding entity 
	public static Map <AInstanceNode, EEntity> modelMapping = new HashMap <AInstanceNode, EEntity>();
	// Map between entity name and the nodes in which it is represented
	public static Map <String, LinkedList<ATreeViewerNode>> entityMapping = new HashMap <String, LinkedList<ATreeViewerNode>>();
	public static Map <Class, LinkedList<Class>> referenceMapping = new HashMap <Class, LinkedList<Class>>();
	// Mapping from an entity to its corresponding entity-box
	private static Map <EEntity, AEntityBox> entityboxMapping = new HashMap <EEntity, AEntityBox>();
	private static  Map <EEntity, LinkedList<AButton>> buttonMapping = new HashMap <EEntity, LinkedList<AButton>>();
	private static Map <Class, ATreeNode> classTree = new HashMap <Class, ATreeNode>();
	
	private static Map <String, LinkedList<String>> shortNames = new HashMap<String, LinkedList<String>>();
	private static Map <String, LinkedList<String>> definedShortNames = new HashMap<String, LinkedList<String>>();
	
	private static Set<ABendPoint> bendPoints = new HashSet<ABendPoint>();
	
	private static Map<Label, ConnectionEndpointLocator> locatorMapping = new
												HashMap<Label, ConnectionEndpointLocator>();
	
	private static Map<AAttributeFigure, Map<Integer, AConnectionRouter>> aggregateMapping = new 
												HashMap<AAttributeFigure, Map<Integer, AConnectionRouter>>();
	

	
	/**
	 * Returns the AConnectionRouter for the attribute represented by @param figure and with index
	 * @param index. Returns null if this does not exist yet.
	 * 
	 */
	public static AConnectionRouter getConnectionRouter(AAttributeFigure figure, int index){
		if (aggregateMapping.containsKey(figure)){
			Map<Integer, AConnectionRouter> map = aggregateMapping.get(figure);
			if (map.containsKey(index))
				return map.get(index);
		}
		return null;
	}
	
	public static void addConnectionRouter(AAttributeFigure figure, int index, AConnectionRouter router){
		Map<Integer, AConnectionRouter> map;
		if (!aggregateMapping.containsKey(figure)){
			map = new HashMap<Integer, AConnectionRouter>();
			aggregateMapping.put(figure, map);
		}
		else map = aggregateMapping.get(figure);
		map.put(index, router);
	}
	
	public static void removeBendPoint(ABendPoint bp) {
		bendPoints.remove(bp);
		if (bp instanceof AConnectionRouter){
			AConnectionRouter cr = ((AConnectionRouter) bp);
			Figure figure = cr.getFigure();
			Map<Integer, AConnectionRouter> map = aggregateMapping.get(figure);
			map.remove(cr.getIndex());
		}
	}

	public static void clear() {
		entityMapping.clear();
		referenceMapping.clear();
	}	
	
	public static void clearFileInfo(){
		entityboxMapping.clear();
		buttonMapping.clear();
		modelMapping.clear();
		bendPoints.clear();
		removeTreeNodes();
	}
	
	public static void addBendpoint(ABendPoint bp){
		bendPoints.add(bp);
	}
	
	public static Set<ABendPoint> getBendpoints(){
		return bendPoints;
	}

	public void addEntity(AInstanceNode node, EEntity entity){
		modelMapping.put(node, entity);
		notifyObservers(node);
	}
	
	public static void addButtonMapping(EEntity entity, AButton button){
		entity = AState.getReplacementEntity(entity);
		if (buttonMapping.containsKey(entity)){
			buttonMapping.get(entity).add(button);
		}
		else {
			LinkedList<AButton> buttons = new LinkedList<AButton>();
			buttons.add(button);
			buttonMapping.put(entity, buttons);
		}
	}
	
	public void addEntityToTree(EEntity entity){
		try {
			String[] entityName = entity.getClass().getName().split("\\.");
			String label = entityName[entityName.length-1].substring(1);
			LinkedList <ATreeViewerNode> nodes = entityMapping.get(label);	
			LinkedList <ATreeViewerNode> entityNodes = new LinkedList <ATreeViewerNode>();
			for (ATreeViewerNode node: nodes){
				AInstanceNode entityNode = new AInstanceNode(node);
				entityNodes.add(entityNode);
				entityNode.setText(entity.getPersistentLabel());
				node.addChild(entityNode);
				modelMapping.put(entityNode, entity);
			}
			entityMapping.put(entity.getPersistentLabel(), entityNodes);
		}catch (Exception e){ e.printStackTrace(); }
	}

	public void addEntityBox(AEntityBox box, EEntity entity){
			entityboxMapping.put(entity, box);
		    setChanged();
			notifyObservers(box);
	}

	public static boolean isEntityBox(EEntity entity) {
		if (entityboxMapping.containsKey(entity))
			return true;
		return false;
	}

	public static AEntityBox getEntityBox(EEntity entity) {
		return entityboxMapping.get(entity);
	}

	public static void deleteEntity(EEntity entity){
		if (buttonMapping.containsKey(entity)){
			for (AButton button: buttonMapping.get(entity)){
				((AEntityFigure) button.getParent()).removeButton(button);
			}
			buttonMapping.remove(entity);
		}
	}
	
	public static boolean hasButtons(EEntity entity) {
		return (buttonMapping.containsKey(entity) && buttonMapping.get(entity).size() > 0);
	}
	
	public static LinkedList<AButton> getButtons(EEntity entity){
		return buttonMapping.get(entity);
	}
	
	public static void hideEntityBox(AEntityBox box){
		EEntity entity = box.getEntityRepresentation();
		entityboxMapping.remove(entity);
		if (buttonMapping.containsKey(entity)){
			for (AButton button: buttonMapping.get(entity)){
				button.toButton();
			}
		}
	}

	public static void removeButtonMapping(AButton button) {
		buttonMapping.get(button.getEntityRepresentation()).remove(button);
	}
	
	public static ATreeNode getTreeNode(Class<?> clazz){
		ATreeNode node = classTree.get(clazz);
		if (node == null && clazz.getSimpleName().contains("$")){
			int dollarIdx = clazz.getSimpleName().indexOf("$");
			node = AModelImpl.getTreeNodeByName(clazz.getSimpleName().substring(1, dollarIdx));
		}
		return node;
	}
	
	public static ATreeNode getTreeNodeByName(String name){
		for (ATreeNode node: classTree.values()){
			if (node.getName().equals(name)){
				return node;
			}
		}
		return null;
	}
	
	
	public static void addClassTree(Map<Class, ATreeNode> ct){
		classTree = ct;
	}
	
	public static LinkedList<Class> getImplicitClasses(Class entityClass){
		LinkedList <Class> implicitClasses = new LinkedList <Class>();
		try {
			if (AModelImpl.referenceMapping.containsKey(entityClass))
				implicitClasses.addAll(referenceMapping.get(entityClass));
		}
 		catch (Exception e1){e1.printStackTrace();}
 		ATreeNode node = AModelImpl.getTreeNode(entityClass);
 		implicitClasses = addAllImplicit(node, entityClass, implicitClasses); 
 		LinkedList <String> alphabetical = new LinkedList <String>();
 		Map <String, Class> alphaMap = new HashMap <String, Class>();
 		ListIterator<Class> itr = implicitClasses.listIterator();
 		while(itr.hasNext())
 	    {
 			Class clazz = ((Class) itr.next());
 			alphabetical.add(clazz.getSimpleName());
 			alphaMap.put(clazz.getSimpleName(), clazz);
 	    }
 		Collections.sort(alphabetical);
 		ListIterator<String> itr2 = alphabetical.listIterator();
 		LinkedList <Class> sortedClasses = new LinkedList <Class>();
 		while(itr2.hasNext())
 	    {
 			sortedClasses.addLast(alphaMap.get(itr2.next()));
 	    }
 		return sortedClasses;
	}
	
	private static LinkedList<Class> addAllImplicit(ATreeNode node, Class entityClass, LinkedList<Class> implicitClasses) {
		if (node.getParents() != null){		//FIXME! For complex entities 
 			for (ATreeNode parent : node.getParents()){
 				if (! parent.getName().equals("Schema")){
 	     			try {		
 						Class parClass = Class.forName("jsdai.S".concat(ASchema.getSchema(parent.getName())).concat(".C").concat(parent.getName()));
 						LinkedList <Class> parImplicit = AModelImpl.referenceMapping.get(parClass);
 						if (parImplicit != null){
 							implicitClasses.addAll(parImplicit);
 						}
 	     			} catch (ClassNotFoundException e1) { e1.printStackTrace(); 
 	     			} catch (Exception e2) { e2.printStackTrace();
 	     			}
 	         		addAllImplicit(parent, entityClass, implicitClasses);
 				}
 			}
 		}
 		return implicitClasses;
	}

	public static Collection<AEntityBox> getBoxes() {
		return entityboxMapping.values();
	}
	
	private static void removeTreeNodes() {
		LinkedList <String> keysToRemove = new LinkedList <String>();
		for (Iterator<Entry<String, LinkedList<ATreeViewerNode>>> iterator = entityMapping.entrySet().iterator(); iterator.hasNext(); ) {
			Entry<String, LinkedList<ATreeViewerNode>> entries = iterator.next();
				for (ATreeViewerNode node: entries.getValue()){
					if (node instanceof AInstanceNode){
						node.getParent().removeChild(node);
						keysToRemove.add(entries.getKey());
					
					}
				}
			 }
		 for (String key: keysToRemove)
			  entityMapping.remove(key);
	}

	public static EEntity getEntity(AInstanceNode node) {
		return modelMapping.get(node);
	}

	public static ATreeViewerNode getTreeViewerNode(String text) {
		if (entityMapping.containsKey(text) && entityMapping.get(text).size()>0)
			return entityMapping.get(text).getFirst();
		else return null;
	}
	
	public static LinkedList<ATreeViewerNode> getTreeViewerNodes(String text) {
		if (entityMapping.containsKey(text))
			return entityMapping.get(text);
		else return new LinkedList<ATreeViewerNode>();
	}
	
	public static void readShortNames(String sFile, boolean defined) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(sFile));
			readFile(reader, defined);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void readFile(BufferedReader reader, boolean defined) {
		String line;
		try {
			while ((line = reader.readLine()) != null){
				String[] words = line.split(",");
				if (words.length > 1){
					if (shortNames.containsKey(words[0])){
						LinkedList<String> sns = shortNames.get(words[0]);
						for (int i = 1; i < words.length; i++)
							sns.add(words[i]);
					}
					else{
						LinkedList<String> sns = new LinkedList<String>();
						for (int i = 1; i < words.length; i++)
							sns.add(words[i]);
						shortNames.put(words[0], sns);
					}
					if (defined){
						if (definedShortNames.containsKey(words[0])){
							LinkedList<String> sns = definedShortNames.get(words[0]);
							for (int i = 1; i < words.length; i++)
								sns.add(words[i]);
						}
						else{
							LinkedList<String> sns = new LinkedList<String>();
							for (int i = 1; i < words.length; i++)
								sns.add(words[i]);
							definedShortNames.put(words[0], sns);
						}
					}
				}
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void readShortNames(URL textURL) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(textURL.openStream()));
            readFile(reader, false);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static LinkedList<String> getShortName(String entityName) {
		return shortNames.get(entityName);
	}

	public static void putInLocatorMapping(Label label, ConnectionEndpointLocator locator) {
		locatorMapping.put(label, locator);
		
	}

	public static ConnectionEndpointLocator getLocator(Object label) {
		return locatorMapping.get(label);
		
	}

	public static void removeEntity(EEntity entity) {
		if (entityboxMapping.containsKey(entity)){
			entityboxMapping.remove(entity);
		}
		
		if (buttonMapping.containsKey(entity)){
			for (AButton b: buttonMapping.get(entity)){
				b.remove();
			}
		}
		buttonMapping.remove(entity);
		
		String str = ASdaiHandler.getPersistantLabel(entity);
		if (entityMapping.containsKey(str)){
			for (ATreeViewerNode node : entityMapping.get(str)){
				node.getParent().removeChild(node);
			}
			entityMapping.remove(str);
		}
	}
	
	public static void addToClassTree(Class clazz, ATreeNode node) {
		classTree.put(clazz,  node);		
	}

	
	/**
	 * Replace entity in box with @nEntity
	 * 
	 */
	public EEntity replaceEntity(AEntityBox box, String nEntity){
		if (ASchema.getSchema(nEntity) == null)
			return null;
		
		String pl = "";
		try {
			pl = box.getEntityRepresentation().getPersistentLabel();
		} catch (SdaiException e) {	e.printStackTrace(); }
		
		EEntity oldEntity = box.getEntityRepresentation();
		LinkedList<AButton> buttons = buttonMapping.get(oldEntity);
		
		Class clazz = ASchema.getClass(nEntity);
		
		EEntity newEntity = null;
		try {
			newEntity = ASdaiHandler.replaceEntity(box.getEntityRepresentation(), clazz);
		} catch (AAbstractEntityException e) {
			e.printStackTrace();
			setChanged();
			notifyObservers(e);
			return null;
		}
		
		AState.lPoint = box.getLocation();
		AState.setActiveObject(box.getSize());

		hideEntityBox(box);
		box.hide(); 
		
		LinkedList<AButton> nButtons = new LinkedList<AButton>();
		if (buttons != null){
			for (AButton button: buttons){
				button.setEntityRepresentation(newEntity);
				if (button.isContained()){
					nButtons.add(button);
				}
				else button.remove();
			}

			buttonMapping.put(newEntity, nButtons);
		}


		if (entityMapping.containsKey(pl)){ 
			for (ATreeViewerNode node: entityMapping.get(pl)){
				node.getParent().removeChild(node);
			}
		}
		else System.err.println("Entity " + pl + " not contained in entityMapping");

		addEntityToTree(newEntity);		
		
		return newEntity;
	}





}
