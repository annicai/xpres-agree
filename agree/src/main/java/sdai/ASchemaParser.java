package sdai;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jsdai.dictionary.EAttribute;
import jsdai.dictionary.EDefined_type;
import jsdai.dictionary.EEntity_declaration;
import jsdai.dictionary.EEntity_definition;
import jsdai.dictionary.EExtended_select_type;
import jsdai.dictionary.EUsed_declaration;
import jsdai.lang.AEntity;
import jsdai.lang.AEntityExtent;
import jsdai.lang.ASdaiModel;
import jsdai.lang.CEntity;
import jsdai.lang.EEntity;
import jsdai.lang.SchemaInstance;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;
import jsdai.lang.SdaiModel;
import model.AModelImpl;
import model.tree.structure.ATreeNode;
import model.tree.structure.AWaitingNode;
import sdai.structure.ASchema;
import sdai.structure.ASelectExtension;
import util.Util;
import view.AResourceLoader;
import view.tree.display.ATreeViewerNode;

/**
 * Parses a STEP schema.
 *
 */
public class ASchemaParser {
	
	private static Map <Class, ATreeNode> classTree = new HashMap <Class, ATreeNode>();
	private static LinkedList <ATreeNode> trees = new LinkedList<ATreeNode>();
	
	private static ATreeViewerNode schemaRoot;

	private static LinkedList <ATreeNode> treesAbstract = new LinkedList<ATreeNode>();
	private static LinkedList <String> complexEntities = new LinkedList <String>();
	// Maps from the name of a defined type to its extensions
	public static Map<String, ASelectExtension> selectExtensions = new HashMap<String, ASelectExtension>();

	private static Set <EEntity_definition> abstractEntities = new HashSet <EEntity_definition>();

	
	public static ATreeViewerNode parseLoadedSchema(String schema){
		schema = schema.substring(0,1).toUpperCase().concat(schema.substring(1).toLowerCase());

		clear();
		
		ObjectInputStream ois;
		try {
			//try {
			URL fileURL = AResourceLoader.getResource(schema.concat(".txt"));
			
			ois = new ObjectInputStream(fileURL.openStream());
			//} catch (Exception ... Then try file saved in folder
			
	        ASchema.setSchemaMapping((HashMap <String, String>)  ois.readObject());
			AModelImpl.entityMapping = (HashMap <String, LinkedList<ATreeViewerNode>>)  ois.readObject();
			ASchema.setSchema(schema);
			ATreeViewerNode tRoot = new ATreeViewerNode(null);

			schemaRoot = new ATreeViewerNode(tRoot);
			schemaRoot.setText(schema.toUpperCase());
			schemaRoot.setAsRoot();
			tRoot.addChild(schemaRoot);
	        
	        for (LinkedList<ATreeViewerNode> twn: AModelImpl.entityMapping.values()){
				for (ATreeViewerNode node: twn){
					if (! node.toString().startsWith("$"))
						schemaRoot.addChild(node);
				}
			}
	        classTree = (HashMap <Class, ATreeNode>)  ois.readObject();
			AModelImpl.referenceMapping = (Map<Class, LinkedList<Class>>) ois.readObject();
	        selectExtensions = (Map<String, ASelectExtension>) ois.readObject();
	        
	        AModelImpl.addClassTree(classTree);
	        
			return tRoot;
		} catch (FileNotFoundException e1) { e1.printStackTrace();
		} catch (IOException e1) { e1.printStackTrace();
		} catch (ClassNotFoundException e) { e.printStackTrace();
		} catch (NullPointerException e) {e.printStackTrace();}
		return null;

	}
	
	public static ATreeViewerNode parseSchema(String schema){

		schema = schema.substring(0,1).toUpperCase().concat(schema.substring(1).toLowerCase());
		
		clear();

		ASchema.setSchema(schema);
		
		ATreeViewerNode tRoot = new ATreeViewerNode(null);
		ATreeNode root = new ATreeNode("Schema");
		
		schemaRoot = new ATreeViewerNode(tRoot);
		schemaRoot.setText(schema.toUpperCase());
		schemaRoot.setAsRoot();
		tRoot.addChild(schemaRoot);
		
		classTree.put(CEntity.class, root);
		trees.add(root);
		
		addTopLevelInstances();
		addUsedData();
		
		addAbstractSchemas();

		addAbstractEntitiesToTree();
		addComplexEntitiesToTree();
		
		buildStructureTree();	
		
		ASdaiHandler.newRepository(schema);
 
		saveSchemaInfo();
		
		AModelImpl.addClassTree(classTree);
		return tRoot;
		
	}

	private static void clear() {
		classTree.clear();
		trees.clear();
		treesAbstract.clear();
		complexEntities.clear();
		selectExtensions.clear();
		abstractEntities.clear();
		AModelImpl.clear();
		ASchema.clear();
	}

	/**
	 * Insert the smaller trees at the correct places to build the large one.	
	 */
	private static void buildStructureTree() {
		
		for (int i = trees.size()-1;i > 0 ;i--){
			try {
				AWaitingNode node = (AWaitingNode) trees.get(i);
				ATreeNode tn = classTree.get(node.getThisClass());
				classTree.get(node.getSuperClass()).addChild(tn);
			}
			catch (Exception e){
				treesAbstract.add(trees.get(i));	
			}
			trees.remove(i);
		}
	}

	/**
	 * Saves the parsed schema information so that it can be retrieved quickly the next time the schema
	 * is loaded.
	 * 
	 */
	private static void saveSchemaInfo() {
		try {
			FileOutputStream fos = new FileOutputStream(ASchema.getSchema().concat(".txt"));
			ObjectOutputStream schemaInfo = new ObjectOutputStream(fos);
			schemaInfo.writeObject(ASchema.getSchemaMapping());
			schemaInfo.writeObject(AModelImpl.entityMapping);
			schemaInfo.writeObject(classTree);
			schemaInfo.writeObject(AModelImpl.referenceMapping);
			schemaInfo.writeObject(selectExtensions);
			schemaInfo.flush();
			schemaInfo.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private static void addTopLevelInstances() {
		List<String> cls = Util.getClassInPackage("jsdai.S".concat(ASchema.getSchema()));

		for (String className : cls){
			String[] entityName = className.split("\\.");
			LinkedList <Class> superClasses = new LinkedList <Class>();
			if (entityName[entityName.length-1].charAt(0)=='C'){
				if (entityName[entityName.length-1].endsWith("_DICTIONAR"))
					continue;
				try {
					Class clazz = Class.forName(className);
					Class subclazz;
					if (className.contains("$")){							
						String[] oneOfThem = className.split("\\$");
						subclazz = Class.forName(oneOfThem[0]);		// Common supertype
					}													
					else subclazz = Class.forName(className);

					Method[] methods = clazz.getMethods();
					for (int i =0; i< methods.length; i++){
						Class returnType = methods[i].getReturnType();
						if (returnType.getSimpleName().startsWith("A") || returnType.getSimpleName().startsWith("E")){
								try{
									returnType = Class.forName(returnType.getName().replace(returnType.getSimpleName(), "C".concat(returnType.getSimpleName().substring(1))));
									if (AModelImpl.referenceMapping.containsKey(returnType)){
										if (! AModelImpl.referenceMapping.get(returnType).contains(clazz)){
											AModelImpl.referenceMapping.get(returnType).add(clazz);
										}
									}
									else {
										LinkedList<Class> ll = new LinkedList<Class>();
										ll.add(clazz);
										AModelImpl.referenceMapping.put(returnType, ll);
									}
								}catch (Exception e ){ }
						}
					}					
					// Interfaces give all supertypes TODO: Replace with addClassToTree
					for(Class c: subclazz.getInterfaces()){	
						if (c.getInterfaces().length > 1){
							for(Class superCl: c.getInterfaces()){	
								String superClass = superCl.getSimpleName().substring(1);
								try{							
									superClasses.add(Class.forName("jsdai.S".concat(ASchema.getSchema(superClass)).concat(".C").concat(superClass)));
								} 	catch (Exception e){ } 
							}
						}
						else superClasses.add(subclazz.getSuperclass());
					}
					// Create classTree, with super-subtype relationships
					for (Class superClazz: superClasses){
						if (classTree.containsKey(superClazz)){
							ATreeNode node;
							if (classTree.containsKey(clazz))
								node = classTree.get(clazz);
							else  {
								node = new ATreeNode(clazz.getSimpleName().substring(1));
								classTree.put(clazz, node);
							}
							classTree.get(superClazz).addChild(node);
						}
						else {
							if (! classTree.containsKey(clazz)){
								ATreeNode node = new ATreeNode(clazz.getSimpleName().substring(1));
								classTree.put(clazz, node);
							}
							AWaitingNode wnode = new AWaitingNode(clazz.getSimpleName().substring(1), superClazz, clazz);
							trees.add(wnode);
						}
					}
				} catch (ClassNotFoundException e) { }				
			}

			
			if (entityName[entityName.length-1].charAt(0)=='C'){
				Class c = null;
				String entity = entityName[entityName.length-1].substring(1);
				if (! AModelImpl.entityMapping.containsKey(entity)){
					if (! entity.contains("$")){
						ATreeViewerNode item = new ATreeViewerNode(schemaRoot);
						schemaRoot.addChild(item);
						item.setText(entity);
						LinkedList<ATreeViewerNode> nodes = new LinkedList<ATreeViewerNode>();
						nodes.add(item);
						AModelImpl.entityMapping.put(entity, nodes);
						ASchema.addSchemaMapping(entity, ASchema.getSchema());
					}
					else {
						complexEntities.add(entity);
					}
				}
			}
		}
		
	}
	
	
	private static void addUsedData() {
		AEntity entity_declaration = null;
		SdaiModel dictionaryModel = null;	
		SchemaInstance dictionaryData;

		try {
			dictionaryData = ASdaiHandler.getSession().getDataDictionary();
			ASdaiModel dictionaryModels = dictionaryData.getAssociatedModels();
			SdaiIterator iterator = dictionaryModels.createIterator();
			while (iterator.next()) {
				dictionaryModel = dictionaryModels.getCurrentMember(iterator);
				AEntityExtent asso = null;
				try {
					 asso = dictionaryModel.getPopulatedFolders();	 
				} catch (Exception e){ }
				if (asso != null) {
					SdaiIterator assoIt = asso.createIterator();
					while(assoIt.next()){
						try {
							if(asso.getCurrentMember(assoIt).getDefinitionString().contains("extended_select_type")){							
								AEntity selects = asso.getCurrentMember(assoIt).getInstances();
								SdaiIterator selIterator = selects.createIterator();
								while (selIterator.next()){
									if (selects.getCurrentMemberEntity(selIterator) instanceof EExtended_select_type){
										EExtended_select_type extended = ((EExtended_select_type) selects.getCurrentMemberEntity(selIterator));
										EDefined_type dt = extended.getIs_based_on(null);
										if (selectExtensions.containsKey(dt.getName(null))){
											selectExtensions.get(dt).addExtendedSelectType(extended);
										}
										else {
											ASelectExtension se = new ASelectExtension(dt.getName(null));
											se.addExtendedSelectType(extended);
											selectExtensions.put(dt.getName(null), se);
										}
									}
								}
							}									
							if (asso.getCurrentMember(assoIt).getDefinitionString().equals("entity_declaration")){
								entity_declaration = asso.getCurrentMember(assoIt).getInstances();
								try {
									iterateEntities(entity_declaration);
								} catch (Exception e){}	
							}
							if (asso.getCurrentMember(assoIt).getDefinitionString().equals("used_declaration")){
								AEntity ent = asso.getCurrentMember(assoIt).getInstances();
								SdaiIterator entIt = ent.createIterator();

								while(entIt.next()){
										EUsed_declaration ud = (EUsed_declaration) ent.getCurrentMemberEntity(entIt);
										EEntity_definition entityDef = null;
										try {
											entityDef = ((EEntity_definition) ud.getDefinition(null));
										} catch (ClassCastException e){ }

										try {
											if (entityDef != null){
												EEntity entity = ASdaiHandler.createEntityInstance(entityDef);
												addClassToTree(entity.getClass().getName());												
												
												Method[] methods = entity.getClass().getMethods();
												for (int i =0; i< methods.length; i++){
													Class<?> returnType = methods[i].getReturnType();
														if (returnType.getSimpleName().startsWith("A") || returnType.getSimpleName().startsWith("E")){
														try{
															returnType = Class.forName(returnType.getName().replace(returnType.getSimpleName(), "C".concat(returnType.getSimpleName().substring(1))));
															if (AModelImpl.referenceMapping.containsKey(returnType)){
																if (! AModelImpl.referenceMapping.get(returnType).contains(entity.getClass())){
																	AModelImpl.referenceMapping.get(returnType).add(entity.getClass());
																}
															}
															else {
																LinkedList<Class> ll = new LinkedList<Class>();
																ll.add(entity.getClass());
																AModelImpl.referenceMapping.put(returnType, ll);
															}
														}catch (Exception e ){ }
	
													}
												}
												entity.deleteApplicationInstance();
											}
										}
										catch (SdaiException ex){
											String entity = entityDef.getName(null);

											if (! AModelImpl.entityMapping.containsKey(entity.substring(0,1).toUpperCase().concat(entity.substring(1)))){
												LinkedList <ATreeViewerNode> nodes = new LinkedList <ATreeViewerNode>();
												ATreeViewerNode item = new ATreeViewerNode(schemaRoot);
												nodes.add(item);
												item.setText(entity.substring(0,1).toUpperCase().concat(entity.substring(1)).concat(" (ABSTRACT) "));
												schemaRoot.addChild(item); //Referenced data should not be shown in the tree
												AModelImpl.entityMapping.put(entity.substring(0,1).toUpperCase().concat(entity.substring(1)), nodes);
												
												abstractEntities.add(entityDef);		
											}
										}
								}
							}
						}
						catch (Exception e){ }
					}
				}
			}
			
			iterateEntities(entity_declaration);
			
		} catch (SdaiException e) {}
		catch (Exception e) {}
		
	}
	
	private static void addAbstractSchemas() {
		for (EEntity_definition entityDef: abstractEntities){
			try{
				String entity = entityDef.getName(null);
				String name = entity.substring(0,1).toUpperCase().concat(entity.substring(1));
				String absname = name.concat(" (ABSTRACT) ");
				for (String sss: ASchema.getAllSchemas()){	// Will not work if there exists a schema with only abstract entities... 
					try {
						Class c = Class.forName("jsdai.S".concat(sss).concat(".C").concat(name));			
						ASchema.addSchemaMapping(name, sss);
						ASchema.addSchemaMapping(absname, sss);
						addToClassTree("jsdai.S".concat(ASchema.getSchema(name).concat(".C").concat(name)));
						break;
					} catch (Exception e){}
				}
				if (! AModelImpl.entityMapping.containsKey(name)){
					LinkedList <ATreeViewerNode> nodes = new LinkedList <ATreeViewerNode>();
					ATreeViewerNode item = new ATreeViewerNode(schemaRoot);
					nodes.add(item);
					item.setText(entity.substring(0,1).toUpperCase().concat(entity.substring(1)).concat(" (ABSTRACT) "));
					AModelImpl.entityMapping.put(name, nodes);
				}
			}
			catch (SdaiException se){}
		}
	}
	
	/**
	 * Iterates through a list of entity declarations.
	 * 
	 * @param entity_declaration
	 * @throws SdaiException
	 */
	private static void iterateEntities(AEntity entity_declaration) throws SdaiException {

		SdaiIterator entIt = entity_declaration.createIterator();
		while(entIt.next()){
				EEntity_declaration ud = (EEntity_declaration) entity_declaration.getCurrentMemberEntity(entIt);
				try {
					EEntity_definition entityDef = ((EEntity_definition) ud.getDefinition(null));
					if (entityDef != null){
						EEntity instance = null;
						try {
							instance = ASdaiHandler.createEntityInstance(entityDef);
						} catch (Exception e){
							// Add abstract?
							// addClassToTree(entityDef.getName(null));
							return;
						}
						
						String className = instance.getClass().getName();
						String[] entityName = className.split("\\.");
						if (entityName[entityName.length-1].charAt(0)=='C'){
							addClassToTree(className);
						}						
					
							Method[] methods = instance.getClass().getMethods();
							for (int i =0; i< methods.length; i++){
								Class returnType = methods[i].getReturnType();
								try {
									EDefined_type defined_type = ASdaiHandler.getDefinedType(returnType.getSimpleName().substring(1));
									LinkedList <Class> possSelect= addPossibleSelects(defined_type, 0);
									if (possSelect != null){
										for (Class clazz: possSelect){
											if (AModelImpl.referenceMapping.containsKey(clazz)){
												if (! AModelImpl.referenceMapping.get(clazz).contains(instance.getClass())){
													AModelImpl.referenceMapping.get(clazz).add(instance.getClass());
												}
											}
											else {
												LinkedList<Class> ll = new LinkedList<Class>();
												ll.add(instance.getClass());
												AModelImpl.referenceMapping.put(returnType, ll);
											}
										}
									
									}
								} catch (Exception exp) {}
								
								try {
									String attribute_name = methods[i].getName().substring(3, methods[i].getName().length());
									EAttribute attribute = instance.getAttributeDefinition(attribute_name.toLowerCase());
									AEntity att_references = attribute.getAllReferences();
									SdaiIterator siter = att_references.createIterator();
									while(siter.next()){
										EEntity current = att_references.getCurrentMemberEntity(siter);
										if (current instanceof EDefined_type){
											LinkedList <Class> possSelect= addPossibleSelects((EDefined_type) current, 0);

											if (possSelect != null){
												for (Class clazz: possSelect){
													if (AModelImpl.referenceMapping.containsKey(clazz)){
														if (! AModelImpl.referenceMapping.get(clazz).contains(instance.getClass())){
															AModelImpl.referenceMapping.get(clazz).add(instance.getClass());
														}
													}
													else {
														LinkedList<Class> ll = new LinkedList<Class>();
														ll.add(instance.getClass());
														AModelImpl.referenceMapping.put(returnType, ll);
													}
												}
											}
										}
										else if (current instanceof EEntity_definition){
											entityDef = (EEntity_definition) current;
											instance = ASdaiHandler.createEntityInstance(entityDef);
											className = instance.getClass().getName();
											addClassToTree(className);
											instance.deleteApplicationInstance();
										}
							}}
							catch (Exception e){}
						} 	
						instance.deleteApplicationInstance();
					}
				}
				catch (SdaiException sd) {}
				
				catch (Exception ex){ 
	/*				if ( entityDef != null && !abstractEntities.contains(entityDef)){
						abstractEntities.add(entityDef);
						System.out.println("Entity def to string: " + entityDef.toString());
					}
			//		ex.printStackTrace();*/
				}		
			
		}
		
	}
	/**
	* Returns a list of entity-classes that can be chosen form the SELECT
	**/
	private static LinkedList<Class> addPossibleSelects(EDefined_type defined_type, int depth){
			LinkedList<Class> select_list = new LinkedList<Class>();
			if (depth == 5) return select_list;		// FIXME: SMT WR
			try {
				AEntity references = defined_type.getAllReferences();
				SdaiIterator refIterator = references.createIterator();
				while (refIterator.next()){
					AEntity selects = (references.getCurrentMemberEntity(refIterator)).getAllReferences();
					SdaiIterator selIterator = selects.createIterator();
					while (selIterator.next()){
							if (selects.getCurrentMemberEntity(selIterator) instanceof EEntity_definition){
								EEntity_definition entityDefinition = ((EEntity_definition)selects.getCurrentMemberEntity(selIterator));
								EEntity ent = ASdaiHandler.createEntityInstance(entityDefinition);
								select_list.add(ent.getClass());
								ent.deleteApplicationInstance();
							}
							else {
								EDefined_type def_type = (EDefined_type) selects.getCurrentMemberEntity(selIterator);
								LinkedList <Class> recursive = addPossibleSelects(def_type, depth++);
								select_list.addAll(recursive);
							}
						}
					}
			} 
			catch (SdaiException e) { } 
			catch (Exception e){ }
			return select_list;	
		}
		
	/**
	 * Adds a node in the TreeViewer tree representation
	 * 
	 * @param className
	 */
	private static void addClassToTree(String className){
		String[] entityName = className.split("\\.");
		if (entityName[entityName.length-1].charAt(0)=='C'){
			Class c = null;
			String entity = entityName[entityName.length-1].substring(1);
			if (! AModelImpl.entityMapping.containsKey(entity)){
				if (! entity.contains("$")){
					LinkedList <ATreeViewerNode> nodes = new LinkedList <ATreeViewerNode>();
					ATreeViewerNode item = new ATreeViewerNode(schemaRoot);
					nodes.add(item);
					schemaRoot.addChild(item);
					item.setText(entity);
					AModelImpl.entityMapping.put(entity, nodes);
					
					String nschema = entityName[entityName.length-2].substring(1);
					
					ASchema.addSchemaMapping(entity, nschema);
					addToClassTree(className);
				}
				else {
					complexEntities.add(entity);
					String nschema = entityName[entityName.length-2].substring(1);
					ASchema.addSchemaMapping(entity, nschema);
//					addToClassTree(className);
				}			
			}
		}
	}
	
	/**
	 * Adds a class to the tree that contains information about the structure of the entities, 
	 * i e which entities inherit from which etc.
	 * 
	 * @param className
	 */
	private static void addToClassTree(String className) {
		LinkedList <Class> superClasses = new LinkedList <Class>();
		try {
			Class clazz = Class.forName(className);
			if (classTree.containsKey(clazz)){
				return;
			}
			Class subclazz;
			if (className.contains("$")){	
				String[] oneOfThem = className.split("\\$");
				oneOfThem[0] = oneOfThem[0].split("\\.")[2].substring(1);
				subclazz = Class.forName("jsdai.S".concat(ASchema.getSchema(oneOfThem[1].substring(0, 1).toUpperCase().concat(oneOfThem[1].substring(1))).concat(".C").concat(oneOfThem[1].substring(0,1).toUpperCase()).concat(oneOfThem[1].substring(1))));		// Common supertype, use one
			
				for (String scz: oneOfThem){
				Class superClazzz = Class.forName("jsdai.S".concat(ASchema.getSchema(scz.substring(0, 1).toUpperCase().concat(scz.substring(1))).concat(".C").concat(scz.substring(0,1).toUpperCase()).concat(scz.substring(1))));
					if (classTree.containsKey(superClazzz)){
						ATreeNode node;
						if (classTree.containsKey(clazz))
							node = classTree.get(clazz);
						else  {
							node = new ATreeNode(clazz.getSimpleName().substring(1));
							classTree.put(clazz, node);
						}
						classTree.get(superClazzz).addChild(node);
					}
					else {
						if (! classTree.containsKey(clazz)){
							ATreeNode node = new ATreeNode(clazz.getSimpleName().substring(1));
							classTree.put(clazz, node);
						}
						AWaitingNode wnode = new AWaitingNode(clazz.getSimpleName().substring(1), superClazzz, clazz);
						trees.add(wnode);
					}
			
				}			
			}													
			else {
				subclazz = Class.forName(className);
			}
			for(Class c: subclazz.getInterfaces()){
				if (c.getInterfaces().length > 1){
					for(Class superCl: c.getInterfaces()){
						String superClass = superCl.getSimpleName().substring(1);	
						String superClazz = superCl.getName().replace("E".concat(superClass), "C".concat(superClass));
						try{						
							superClasses.add(Class.forName(superClazz));
						}		
						catch (Exception e){}								
					}
				}
				else {
					superClasses.add(subclazz.getSuperclass());
				}
			}
			if (classTree.containsKey(clazz))
					return;
			for (Class superClazz: superClasses){
					if (classTree.containsKey(superClazz)){
						ATreeNode node;
						if (classTree.containsKey(clazz))
							node = classTree.get(clazz);
						else  {
							node = new ATreeNode(clazz.getSimpleName().substring(1));
							classTree.put(clazz, node);
						}
						classTree.get(superClazz).addChild(node);
					}
					else {
						if (! classTree.containsKey(clazz)){
							ATreeNode node = new ATreeNode(clazz.getSimpleName().substring(1));
							classTree.put(clazz, node);
						}
						AWaitingNode wnode = new AWaitingNode(clazz.getSimpleName().substring(1), superClazz, clazz);
						trees.add(wnode);
					}
				}
			} catch (ClassNotFoundException e) {}

	}
	
	private static void addAbstractEntitiesToTree() {
		for (int i = trees.size()-1;i > 0 ;i--){
			try {
				AWaitingNode wn = (AWaitingNode)trees.get(i);
				ATreeNode tn = classTree.get(wn.getThisClass());
				classTree.get(wn.getSuperClass()).addChild(tn);
			} catch (Exception e){
				treesAbstract.add(trees.get(i));
			}
		}
		if (treesAbstract.size()==0)
			return;
		for (ATreeNode wn: treesAbstract){
			addToClassTree(((AWaitingNode)wn).getSuperClass().getName());
		}
		treesAbstract.clear();
		addAbstractEntitiesToTree();
	}
	
	private static void addComplexEntitiesToTree() {
		for (String entity: complexEntities){
			String[] complex_parts = entity.split("\\$");
			for (String complexnode: complex_parts){
				LinkedList<ATreeViewerNode> parentNodes = AModelImpl.entityMapping.get(complexnode.substring(0,1).toUpperCase().concat(complexnode.substring(1)));
				allPossibilities(parentNodes, complex_parts, complexnode, entity);
			}
			if (! ASchema.getSchemaMapping().containsKey(entity)){
				ASchema.addSchemaMapping(entity, ASchema.getSchema(complex_parts[0]));
			}
			addToClassTree("jsdai.S".concat(ASchema.getSchema(complex_parts[0]).concat(".C").concat(entity)));
		}	
	}
	
	/**
	 *      Creates all combinations of a complex entity
	**/
	private static void allPossibilities(LinkedList<ATreeViewerNode> parentNodes, String[] complex_p, String remove, String entity){
		try{
			String[] complex_parts = new String[complex_p.length-1];
			int index = 0;
			for(int i =0; i< complex_p.length; i++){
				if (complex_p[i] != remove){
					complex_parts[index] = complex_p[i];
					index++;
				}
			}
			for (ATreeViewerNode parentNode: parentNodes){
				for (String complex: complex_parts){
					if (complex_parts.length==1){
						boolean exists = false;
						for (ATreeViewerNode node: parentNode.getChildren()){
							if (node.toString().equals("$".concat(complex.substring(0,1).toLowerCase().concat(complex.substring(1))))){
								exists=true;
								if (AModelImpl.entityMapping.containsKey(entity)){
									AModelImpl.entityMapping.get(entity).add(node);
								}
								else {		
									LinkedList<ATreeViewerNode> nodes = new LinkedList <ATreeViewerNode>();
									nodes.add(node);
									AModelImpl.entityMapping.put(entity, nodes); 
								}
							}
						}
						if (!exists){
							ATreeViewerNode item = new ATreeViewerNode(parentNode);
							item.setText("$".concat(complex.substring(0,1).toLowerCase().concat(complex.substring(1))));
							parentNode.addChild(item); 
							if (AModelImpl.entityMapping.containsKey(entity)){
								AModelImpl.entityMapping.get(entity).add(item);
							}
							else {		
								LinkedList<ATreeViewerNode> nodes = new LinkedList <ATreeViewerNode>();
								nodes.add(item);
								AModelImpl.entityMapping.put(entity, nodes);		 
							}
						}
					}
					else {
						boolean exists = false;
						for (ATreeViewerNode node: parentNode.getChildren()){
							if (node.toString().equals("$".concat(complex.substring(0,1).toLowerCase().concat(complex.substring(1))))){
								exists=true;
								LinkedList<ATreeViewerNode> parent = new LinkedList<ATreeViewerNode>();
								parent.add(node);
								allPossibilities(parent, complex_parts, complex, entity);
							}
						}
						if (!exists){
							ATreeViewerNode item = new ATreeViewerNode(parentNode);
							item.setText("$".concat(complex.substring(0,1).toLowerCase().concat(complex.substring(1))));
							parentNode.addChild(item); 
							LinkedList<ATreeViewerNode> parent = new LinkedList<ATreeViewerNode>();
							parent.add(item);
							allPossibilities(parent, complex_parts, complex, entity);
						}
					}
				}
			}
		}
		catch(Exception e){ e.printStackTrace();  }
	}
	
	
	
	
}
