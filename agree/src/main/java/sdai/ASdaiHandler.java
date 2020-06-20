package sdai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.draw2d.Figure;

import controller.AState;
import exceptions.AAbstractEntityException;
import exceptions.AUnrecognizedSchemaException;
import model.AModelImpl;
import model.tree.structure.ATreeNode;
import sdai.structure.ASchema;
import sdai.structure.ASelectExtension;
import view.box.AEntityBox;
import view.box.attribute.extra.AReturnInfo;
import jsdai.dictionary.AExplicit_attribute;
import jsdai.dictionary.CReal_type;
import jsdai.dictionary.CString_type;
import jsdai.dictionary.EAttribute;
import jsdai.dictionary.EDefined_type;
import jsdai.dictionary.EEntity_definition;
import jsdai.dictionary.EEnumeration_type;
import jsdai.dictionary.ELogical_type;
import jsdai.lang.AEntity;
import jsdai.lang.ASdaiModel;
import jsdai.lang.A_string;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;
import jsdai.lang.SdaiModel;
import jsdai.lang.SdaiRepository;
import jsdai.lang.SdaiSession;
import jsdai.lang.Value;


public class ASdaiHandler {
	
	private static SdaiSession session;
	private static SdaiModel model;
	private static SdaiRepository repository;

	private static Set<String> basicEEntityMethods = new HashSet<String>(); 
	
	static {
		
		for (Method m: EEntity.class.getMethods()) {
			basicEEntityMethods.add(m.getName());
		}

	}

	public static void startSession(){
		java.util.Properties prop = new java.util.Properties();
		String tempDir = System.getProperty("java.io.tmpdir");
		if (! tempDir.endsWith(System.getProperty("file.separator"))){
			tempDir = tempDir.concat((System.getProperty("file.separator")));
		}
		
		String repo = tempDir.concat("repo".concat(Double.toString(Calendar.getInstance().getTimeInMillis())));
		prop.setProperty("repositories", repo);
		try {
			SdaiSession.setSessionProperties(prop);
			session = SdaiSession.openSession();
			session.startTransactionReadWriteAccess();
		} catch (SdaiException e) {
			e.printStackTrace();
		}
	}
	
	public static SdaiSession getSession(){
		return session;
	}
	
	public static void newRepository(String schema){	
		schema = schema.substring(0,1).toUpperCase().concat(schema.substring(1).toLowerCase());

		try {
			if (repository != null){
				repository.deleteRepository();
			}
			repository = session.createRepository("", null);
			repository.openRepository();
			
			model = repository.createSdaiModel("model".concat(Double.toString(Calendar.getInstance().getTimeInMillis())), Class.forName("jsdai.S".concat(schema).concat(".S").concat(schema)));	
			model.startReadWriteAccess();

		} catch (SdaiException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void saveModel(String file_name){
		try {
			if (repository != null && repository.isActive())
				repository.exportClearTextEncoding(file_name.concat(".stp"));
		} catch (SdaiException e) {
			e.printStackTrace();
		}
	}
	
	public static EEntity createEntityInstance(EEntity_definition entityDef) throws SdaiException {
		EEntity entity = model.createEntityInstance(entityDef);
		AState.hasChanged = true;
		return entity;
	}
	

	public static EEntity createEntityInstance(long pLabel, String entity) {
		try {
			String location = AState.getTempDir().concat("tmp");
			saveModel(location);
			newRepository(ASchema.getSchema());
			open(location.concat(".stp"));
			repository.setNextPersistentLabel(pLabel);
			AState.hasChanged = true;
			return createEntityInstance(entity);

		} catch (SdaiException e) {	e.printStackTrace();
		} catch (AAbstractEntityException e) { e.printStackTrace();
		}
		return null;
	}
	
	public static EEntity createEntityInstance(String entityString) throws AAbstractEntityException{
			Class c = null;
			EEntity entity = null;
			try {									
				entityString = entityString.replaceAll(" ", "");
				if (ASchema.getSchema(entityString) != null){
					String className = "jsdai.S".concat(ASchema.getSchema(entityString)).concat(".C").concat(entityString);

					c = Class.forName(className);
					entity =  model.createEntityInstance(c);
				} 
				else {
					String errorMsg = getSubclasses(c);	
					throw new AAbstractEntityException(errorMsg); }
			} catch (SdaiException e) { e.printStackTrace();
				String errorMsg = getSubclasses(c);
				throw new AAbstractEntityException(errorMsg);
			} catch (ClassNotFoundException e) {	e.printStackTrace();
			} catch (NullPointerException e){	e.printStackTrace();
			}
			AState.hasChanged = true;
			return entity;
	}

	public static EDefined_type getDefinedType(String stype) throws SdaiException {
		return model.getUnderlyingSchema().getDefinedType(stype);

	}

	public static ASdaiModel getModels() throws SdaiException {
		return repository.getModels();
	}

	public static boolean hasCorrectSchema() throws SdaiException, AUnrecognizedSchemaException{
		if (repository == null){
			throw new AUnrecognizedSchemaException();
		}
		ASdaiModel models = repository.getModels();
		SdaiIterator modelIterator = models.createIterator();
		while(modelIterator.next()){
			SdaiModel model = models.getCurrentMember(modelIterator);
			if (ASchema.getSchema() != null && ASchema.getSchema().toUpperCase().equals(model.getUnderlyingSchemaString())){
				return true;
			}
		}
		return false;
	}
	
	public static String getSchema() throws SdaiException{
		ASdaiModel models = repository.getModels();
		SdaiIterator modelIterator = models.createIterator();
		while(modelIterator.next()){
			SdaiModel model = models.getCurrentMember(modelIterator);
				return model.getUnderlyingSchemaString();
		}
		return null;
	}
	
	public static void open(String step){	
		try {		
			repository = session.importClearTextEncoding("", step, null);
			if (!repository.isActive()){												
				repository.openRepository();
			}
			model = repository.getModels().getByIndex(1);
		} catch (SdaiException e) { 
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Whether or not this method is used to set/get an attribute. 
	 * 
	 * @return
	 */
	public static boolean isAttributeMethod(Method m) {
		return ! basicEEntityMethods.contains(m.getName()) ;
	}
	
	
	/**
	 * Whether or not this method is a SELECT / VALUE type, eg MEASUREVALUE(0.5)
	 * 
	 */
	public static boolean isSelect(Method m) {
		return m.getParameterTypes().length == 2;
	}
	
	/**
	 * Get all classes for entity @entity.
	 * If the entity is complex, a list with all classes that make it up are returned.
	 * 
	 */
	public static List<Class<?>> getClasses(EEntity entity) {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		String simpleName = entity.getClass().getSimpleName();
		
		if (simpleName.contains("$")){
			String[] complex = simpleName.substring(1).split("\\$");
			for (int i = 0; i < complex.length; i++ ) {
				String name = complex[i].substring(0,1).toUpperCase().concat(complex[i].substring(1));
				try {
					classes.add(Class.forName("jsdai.S".concat(ASchema.getSchema(name)).concat(".C").concat(name)));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			classes.add(entity.getClass());
		}
		return classes;
	}
	
	/**
	 * Get all declared fields for this class and all its superclass's.
	 */
	public static List<Field> getDeclaredFields(Class<?> c, String attribute_name){
		LinkedList <Field> declaredFields = new LinkedList<Field>();
		
		if (c == EEntity.class)
			return declaredFields;
		
		Field[] fields = ((Class<?>) c.getGenericInterfaces()[0]).getDeclaredFields();
		for (Field field: fields){
			if (field.getName().matches(String.format("^s%s[A-Z].*$", attribute_name))) {
				declaredFields.add(field);
			}					
		}
		
		Class<?>[] interfaces = c.getInterfaces();
		if (interfaces.length > 0) {
			for (Class<?> i: interfaces) {
				declaredFields.addAll(getDeclaredFields(i, attribute_name));
			}
			return declaredFields;
		}
		
		declaredFields.addAll(getDeclaredFields(c.getSuperclass(), attribute_name));
		return declaredFields;

	}
	

	public static AReturnInfo getReturnType(Method getMethod, String attribute_name, EEntity entity) {
			Map <EEntity_definition, LinkedList<EAttribute>> attributeMap = new HashMap<EEntity_definition, LinkedList<EAttribute>> ();
			
			AReturnInfo info = new AReturnInfo();
			Class<?> returnClass = getMethod.getReturnType();
			String returnType = getMethod.getReturnType().getSimpleName();

			LinkedList <Object> order = null;
			EEntity_definition entity_def = null;
			try {
				entity_def = model.getEntityDefinition(entity.getClass().getSimpleName().substring(1));
				order = getOrder(entity.getClass(), entity_def, attribute_name);
			} catch (SdaiException e2) {}
			
			info.setOrder(order);
			
			if (returnType.matches("Aa*(_integer|_string_|_double)")){
				info.aggregate = true;
				info.returnclasses.add(returnClass);
				info.returntype = returnType;
				info.aggLevel = returnType.indexOf("_");
			}
			else if (returnClass == int.class){
				info.returntype = "Integer";
				info.aggregate = false;
				info.returnclasses.add(int.class);	
				try {
					EAttribute attribute_def = null;
					try{
						attribute_def = entity.getAttributeDefinition(attribute_name.toLowerCase());
					} catch (Exception ex){
						String parent = getMethod.getParameterTypes()[0].getSimpleName().substring(1).toLowerCase();
						EEntity_definition ed = model.getEntityDefinition(parent);
						// FIXME: How to resolve this problem with increasing persistent labels?
						EEntity temp_entity = model.createEntityInstance(ed);
						attribute_def = temp_entity.getAttributeDefinition(attribute_name.toLowerCase());
						temp_entity.deleteApplicationInstance();

					}
					AEntity attribute_ref = attribute_def.getAllReferences();
					SdaiIterator refIterator = attribute_ref.createIterator();
					while(refIterator.next()){
						if (attribute_ref.getCurrentMemberEntity(refIterator) instanceof EDefined_type){
							EDefined_type defined_type = (EDefined_type) attribute_ref.getCurrentMemberEntity(refIterator);
							AEntity defined_ref = defined_type.getAllReferences();
							SdaiIterator defIt = defined_ref.createIterator();
							while(defIt.next()){
								if (defined_ref.getCurrentMemberEntity(defIt) instanceof EEnumeration_type){
										EEnumeration_type enum_type =((EEnumeration_type) defined_ref.getCurrentMemberEntity(defIt));
										info.returntype = defined_type.getName(null).substring(0,1).toUpperCase().concat(defined_type.getName(null).substring(1).concat(" (ENUM)"));
										info.returnclasses.add(enum_type.getClass());
										info.enumeration = true;
										A_string values = enum_type.getElements(null);
										SdaiIterator valueIterator = values.createIterator();
										while (valueIterator.next()){
											info.enumValues.add(".".concat(values.getCurrentMember(valueIterator).toUpperCase().concat(".")));
										}
								}
							}
						}
						else if (attribute_ref.getCurrentMemberEntity(refIterator) instanceof ELogical_type){
							ELogical_type logical_type =(ELogical_type) attribute_ref.getCurrentMemberEntity(refIterator);
							info.returntype = "Logical";
							info.returnclasses.add(logical_type.getClass());
						}			
					}	
					
				} catch (SdaiException e) {	e.printStackTrace();}
			
			}
			else if (returnClass == String.class || returnClass == Value.class || returnClass == double.class || returnClass == boolean.class ) {
				info.returntype = returnType.toUpperCase().substring(0,1).concat(returnType.substring(1));
				info.aggregate = false;
				info.returnclasses.add(returnClass);
			}
			else {
				info.returntype = returnType.substring(1);
				if (getMethod.getReturnType() != EEntity.class && !getMethod.getReturnType().getSimpleName().startsWith("A")){
					info.returnclasses.add(getMethod.getReturnType());
				}
				if (returnType.startsWith("A"))
				{	
					int index = 1;
					while(returnType.charAt(index) == 'a'){
						index ++;
					}
					info.aggLevel=index;
					info.returntype = returnType.substring(index);
					info.aggregate = true;
					if (AModelImpl.entityMapping.containsKey(returnType.substring(index))){
						try {
							if (!info.returnclasses.contains(Class.forName("jsdai.S".concat(ASchema.getSchema(returnType.substring(index))).concat(".E").concat(returnType.substring(index)))))
								info.returnclasses.add(Class.forName("jsdai.S".concat(ASchema.getSchema(returnType.substring(index))).concat(".E").concat(returnType.substring(index))));
						} catch (ClassNotFoundException e) { e.printStackTrace(); }
					}
					else {
						info.select = true;
						EDefined_type defined_type = null;
						LinkedList<String> selects = new LinkedList<String>();
						try { // Find Selects 
								
							// Add ordinary Selects
							defined_type = model.getUnderlyingSchema().getDefinedType(returnType.substring(index));
							selects.addAll(findSelects(defined_type, info));
							//  Add extended Selects
							if (ASchemaParser.selectExtensions.containsKey(defined_type.getName(null)))
								selects.addAll(ASchemaParser.selectExtensions.get(defined_type.getName(null)).getSelectExtensions());
			
							// Add to returnclassses
							for (String s: selects){	
								try {
									if (s.equals("Real_type"))
										info.returnclasses.add(CReal_type.class);
									else if (s.equals("String_type"))
										info.returnclasses.add(CString_type.class);
									else if (ASchema.getSchema(s) != null)
										info.returnclasses.add(Class.forName("jsdai.S".concat(ASchema.getSchema(s)).concat(".C").concat(s)));
								} catch (ClassNotFoundException e) { e.printStackTrace(); }
							}
						} catch (Exception e1) { e1.printStackTrace(); }
					}
					return info;
				}
				else if (returnType.startsWith("E")){	
					if (AModelImpl.entityMapping.containsKey(returnType.substring(1))){
						info.returntype = returnType.substring(1);
						info.aggregate=false;
						info.returnclasses.add(getMethod.getReturnType());
					}
					else {	
						info.aggregate= false;
						info.select = true;
						try { 
							EAttribute attribute = null;
							try {
								attribute = entity.getAttributeDefinition(attribute_name.toLowerCase());
							}
							catch (Exception e){
								// Probably "double" attribute, has > 1 attribute with same name, use declared class 
								String parent = getMethod.getParameterTypes()[0].getSimpleName().substring(1).toLowerCase();
								EEntity_definition ed = model.getEntityDefinition(parent);
								//FIXME: Do not create new entities! Why does not ed.getAttribute.. not work?
								if (attributeMap.containsKey(ed)){	
									for (EAttribute a :attributeMap.get(ed)){
										if (a.getName(null).equals(attribute_name.toLowerCase())){
											attribute = a;
										}
									}
								}
								else {
									LinkedList<EAttribute> attributes = new LinkedList<EAttribute>();
									attributeMap.put(ed, attributes);
								}
								
								if (attribute == null){
									//FIXME: Problem with increasing PL's
									EEntity temp_entity = model.createEntityInstance(ed);
									attribute = temp_entity.getAttributeDefinition(attribute_name.toLowerCase());
									temp_entity.deleteApplicationInstance();
									attributeMap.get(ed).add(attribute);
								}
							}
							
							AEntity att_references = attribute.getAllReferences();
							SdaiIterator siter = att_references.createIterator();
							while(siter.next()){
								EEntity current = att_references.getCurrentMemberEntity(siter);

								if (current instanceof EDefined_type){
									LinkedList<String> selects = new LinkedList<String>();
									try {
										ASelectExtension se = ASchemaParser.selectExtensions.get(((EDefined_type) current).getName(null).toLowerCase());
										if (se != null)
											selects = se.getSelectExtensions();
									} catch (NullPointerException e){ 
										selects = new LinkedList<String>();
										e.printStackTrace();
									}

									selects.addAll(findSelects((EDefined_type) current, info));
				
									info.returntype = ((EDefined_type)current).getName(null).substring(0,1).toUpperCase().concat(((EDefined_type)current).getName(null).substring(1,((EDefined_type)current).getName(null).length()).concat(" (T) "));
									for (String s: selects){
										try {	
											if (s.equals("Real_type"))
												info.returnclasses.add(CReal_type.class);
											else if (s.equals("String_type"))
												info.returnclasses.add(CString_type.class);
											else {
												if (ASchema.getSchema(s) != null)
													info.returnclasses.add(Class.forName("jsdai.S".concat(ASchema.getSchema(s)).concat(".C").concat(s)));
											}
										} catch (ClassNotFoundException e) { e.printStackTrace();}
									}
								}
								else if (current instanceof EEntity_definition){
									if (! entity.isKindOf((EEntity_definition) current)){
										String s = ((EEntity_definition) current).getName(null).substring(0,1).toUpperCase().concat(((EEntity_definition) current).getName(null).substring(1));
										try {
											info.returntype = s;
											info.returnclasses.add(Class.forName("jsdai.S".concat(ASchema.getSchema(s)).concat(".C").concat(s)));
										} catch (ClassNotFoundException e) { e.printStackTrace();}
									}
								}
							}
						} catch (SdaiException|NullPointerException e) { e.printStackTrace();}
					}
				}
				}
			return info;
		}
	
	/**
	 * Find selects based on a given defined_type
	 * 
	 * @param defined_type
	 * @param info
	 * @return
	 */
	private static LinkedList<String> findSelects(EDefined_type defined_type, AReturnInfo info) {
		LinkedList<String> select_list = new LinkedList<String>();
		try {
			if (ASchemaParser.selectExtensions.containsKey(defined_type.getName(null).toLowerCase())){
				ASelectExtension se = ASchemaParser.selectExtensions.get(defined_type.getName(null).toLowerCase());
				for (String exst: se.getSelectExtensions()){
					select_list.add(exst);
					}
				}
						
				AEntity references = defined_type.getAllReferences();
				SdaiIterator refIterator = references.createIterator();
						
				while (refIterator.next()){
					AEntity selects = (references.getCurrentMemberEntity(refIterator)).getAllReferences();
					SdaiIterator selIterator = selects.createIterator();
					if (selects.getMemberCount() == 0){
						if (defined_type.toString().contains("#3);")){
							select_list.add("Real_type");
							info.selectMap.put(CReal_type.class, defined_type.getName(null).substring(0,1).toUpperCase().concat(defined_type.getName(null).substring(1).concat(" (T) ")));
						}
						else if (defined_type.toString().contains("#7);")){ 
							select_list.add("String_type");
							info.selectMap.put(CString_type.class, defined_type.getName(null).substring(0,1).toUpperCase().concat(defined_type.getName(null).substring(1).concat(" (T) ")));
						}
					}
				while (selIterator.next()){
						if (selects.getCurrentMemberEntity(selIterator) instanceof EEntity_definition){
							String first = ((EEntity_definition)selects.getCurrentMemberEntity(selIterator)).getName(null).substring(0, 1).toUpperCase();
							String returnst = first.concat(((EEntity_definition)selects.getCurrentMemberEntity(selIterator)).getName(null).substring(1));
							select_list.add(returnst);
						}
						else if (selects.getCurrentMemberEntity(selIterator) instanceof EDefined_type) {
							EDefined_type def_type = (EDefined_type) selects.getCurrentMemberEntity(selIterator);
							String key = def_type.getName(null).substring(0,1).toUpperCase().concat(def_type.getName(null).substring(1).concat(" (T) "));
							
							EDefined_type nxt = (EDefined_type) selects.getCurrentMemberEntity(selIterator);
							if (! info.selectName.contains(key)){
								info.selectName.add(key);
								info.selectNameMap.put(key, defined_type.getName(null).substring(0,1).toUpperCase().concat(defined_type.getName(null).substring(1).concat(" (T) ")));
								LinkedList <String> recursive = findSelects(nxt, info);
								String s = ((EDefined_type)selects.getCurrentMemberEntity(selIterator)).getName(null).substring(0,1).toUpperCase().concat(((EDefined_type)selects.getCurrentMemberEntity(selIterator)).getName(null).substring(1,((EDefined_type)selects.getCurrentMemberEntity(selIterator)).getName(null).length()));
								for (String string: recursive){
										if ((! string.equals("Real_type")) && (!string.equals("String_type"))){
											info.selectMap.put(Class.forName("jsdai.S".concat(ASchema.getSchema(string)).concat(".C").concat(string)), s.concat(" (T) "));
										}
									}
									select_list.addAll(recursive);
								}
							
							}
						}
					}
				} 
				catch (SdaiException e) {} 
				catch (Exception e){}
					
				return select_list;	
			}
	
	
	private static LinkedList<Object> getOrder(Class<? extends EEntity> entity, EEntity_definition entity_definition, String attribute_name) {
		LinkedList<Object> order = new LinkedList<Object>();
		try {
			AExplicit_attribute exp_attributes = entity_definition.getExplicit_attributes(null);
			SdaiIterator iter = exp_attributes.createIterator();
			while(iter.next()){
				EAttribute attribute = exp_attributes.getCurrentMember(iter);
			
				if (attribute.getName(null).equals(attribute_name.toLowerCase())){
					try {
						order.add(attribute.getOrder(null));
					} 	catch (Exception e){}
				}
			}
			for(Class c: entity.getInterfaces()){
				if (c.getInterfaces().length >= 1){
					for(Class superCl: c.getInterfaces()){	
						if (superCl != EEntity.class){
							try {
								LinkedList <Object> next = new LinkedList <Object>();
								EEntity_definition entity_def = model.getEntityDefinition(superCl.getSimpleName().substring(1));
								next = getOrder(superCl, entity_def, attribute_name);
								if (next.size() > 0){
									order.add(superCl.getSimpleName().substring(1));
									order.addAll(next);
									return order;
								}
							}
							catch (SdaiException se){}
						}
					}
				}
			}
		}
		catch (Exception e){ e.printStackTrace(); }
		return order;
	}

	public static AEntity findEntityInstanceUsers(EEntity entity) {
		AEntity storage = new AEntity();
		try {
			entity.findEntityInstanceUsers(repository.getModels(), storage);
		} catch (SdaiException e) { 	e.printStackTrace(); }
		return storage;
	}


	public static LinkedList<EEntity> findAllInstances(Class<?> clazz) {
		LinkedList<EEntity> entities = new LinkedList<EEntity>();
		ASdaiModel models;
		try {
			models = repository.getModels();
			SdaiIterator it = models.createIterator();
			while(it.next()){
				SdaiModel m = models.getCurrentMember(it);
				AEntity toAdd = m.getEntityExtentInstances(clazz);
				SdaiIterator iterator = toAdd.createIterator();
				while (iterator.next()){
					entities.add(toAdd.getCurrentMemberEntity(iterator));
				}
			}
		} catch (SdaiException e) {
		//	e.printStackTrace(); 
		}
		return entities;
	}
	
	public static List<Method> getStringAttributes(Class<?> clazz){
		List<Method> methods = new LinkedList<Method>();
		for (Method m: clazz.getMethods()){
			if (m.getParameterTypes().length == 1 && m.getName().startsWith("get")){
				if (m.getReturnType() == String.class){
					methods.add(m);
				}
			}
		}
		return methods;
	}
	
	public static EEntity replaceEntity(EEntity oentity, Class<?> nclass) throws AAbstractEntityException{
		EEntity entity = null;
		try {
			entity = model.substituteInstance(oentity, nclass);
		} catch (SdaiException e) {
			e.printStackTrace();
			throw new AAbstractEntityException("Entity definition is abstract and cannot be used to create entity instances.");
		}
		return entity;
		
	}

	public static void deleteEntityInstance(EEntity entity) {
		try {
			entity.deleteApplicationInstance();
			AState.hasChanged = true;
		} catch (SdaiException e) {
			e.printStackTrace();
		}
	}

	public static String getSubclasses(Class c) {
		String error = "Entity definition is abstract and cannot be used to create entity instances.";
		if (AModelImpl.getTreeNode(c) != null){
			error = error.concat("\nDirect known subclasses: ");
			for (ATreeNode n: AModelImpl.getTreeNode(c).getChildren()){
				if (! error.contains(n.getName().concat(", ")))
					error = error.concat(n.getName()).concat(", ");
			}
			error = error.substring(0, error.length()-2);
			if (AModelImpl.getTreeNode(c).getChildren().size() == 0){
				error = "Entity definition is abstract and cannot be used to create entity instances.\nNo direct known subclasses were found in the imported schema.";
			}
		}
		return error;
	}

	public static boolean isValidSTEP(String string) {
		try {					
			repository = session.importClearTextEncoding("", string, null);
			if (!repository.isActive())											
				repository.openRepository();
		} catch (SdaiException e) {	e.printStackTrace();
			return false;
		}
		return true;
	}

	
	public static AEntity replicateEntities(LinkedList<Figure> figures) {
		AEntity entities = new AEntity();
		AEntity result = new AEntity();
		try {
			for (Figure figure: figures){
				if (figure instanceof AEntityBox){
					EEntity entity = ((AEntityBox)figure).getEntityRepresentation();
					entities.addUnordered(entity);
				}
			}
			result = model.copyInstances(entities);
		} catch (SdaiException e) {	e.printStackTrace(); }
		return result;
	}

	public static AEntity getEntities() {
		try {
			if (model != null)
				return model.getInstances();
		} catch (SdaiException e) {
			e.printStackTrace();
		}
		return new AEntity();
	}

	public static int getEntityCount() {
		try {
			return getEntities().getMemberCount();
		} catch (SdaiException e) {	e.printStackTrace();
		return 0;
		}
	}

	public static String getPersistantLabel(EEntity entity) {
		try {
			return entity.getPersistentLabel();
		} catch (SdaiException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * 
	 * @param entity
	 * @return
	 */
	public static boolean isEntity(EEntity entity) {
		try {
			return model.getInstances().isMember(entity);
		} catch (SdaiException e) {	e.printStackTrace();}
		return false;
	}

    public static String getEntityAsString(EEntity entity){
    	String className = entity.getClass().getName();
    	String[] entityName = className.split("\\.");
    	return entityName[entityName.length-1].substring(1);
    }




}
