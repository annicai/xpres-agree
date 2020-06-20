package sdai.structure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import util.Util;


public class ASchema {
	
	// Map entity -> corresponding schema
	private static Map<String, String> schemaMapping = new HashMap<String, String>();	
	private static LinkedList<String> allSchemas = new LinkedList<String>();
	private static LinkedList<String> apSchemas = new LinkedList<String>();
	private static String currentSchema;
	public static LinkedList<String> dynamicSchemas = new LinkedList<String>();
	public static Map<String, String> dynamicSchemaPaths = new HashMap<String, String>();
	
	/**
	 * Dynamically add a schema. 
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static String addDynamicSchema(String path) throws IOException{
		File file = new File(path);

		URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Method method;
		URL url;
		try {
			url = file.toURI().toURL();
			method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(classLoader, url);
		} catch (NoSuchMethodException|SecurityException|MalformedURLException|
				IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
			e.printStackTrace();
		} 
		
		String loadedSchemaName = "";
		
        FileInputStream fis = new FileInputStream(path);
        JarInputStream jis = new JarInputStream(fis, false);
        JarEntry e = null;
        while((e=jis.getNextJarEntry())!=null){
        	String[] schemaName = e.getName().split("/");
        	if (schemaName.length > 1 && schemaName[0].equals("jsdai") && schemaName[1].startsWith("S")){

            	allSchemas.add(schemaName[1].substring(1));
            	String fullSchemaPath = schemaName[0].concat(".").concat(schemaName[1]);
            	dynamicSchemas.add(fullSchemaPath);            
            	dynamicSchemaPaths.put(fullSchemaPath , path);
            	
            	loadedSchemaName = schemaName[1].substring(1);
            	
            	break;
        	}
     	    jis.closeEntry();
        }
        jis.close();
        
        Collections.sort(allSchemas);
		return loadedSchemaName;
	}
		
	public static void addSchemas() {
		String lastSchema = "";
		List<String> allschemas = Util.getClassInPackage("jsdai");	
		
		for (String schemaName : allschemas){
			if (schemaName.startsWith("jsdai.S") && ! lastSchema.equals(schemaName.substring(7,schemaName.indexOf(".", 7)))){
				String s = schemaName.substring(7,schemaName.indexOf(".", 7));
				if (! allSchemas.contains(s)){
					allSchemas.add(s);
					if (s.matches("Ap[0-9]+[A-Za-z0-9_]+")){
						apSchemas.addFirst(s);
					}
					else if (s.startsWith("Automotive_design") || s.startsWith("Integrated_cnc")){
						apSchemas.add(s);
					}
				}
				lastSchema = s;
			}
		}
		Collections.sort(allSchemas);
		Collections.sort(apSchemas);
		
	}
	
	public static LinkedList<String> getApSchemas() {
		return apSchemas;
	}
	
	public static LinkedList<String> getAllSchemas(){
		return allSchemas;
	}
	
	public static void addSchemaMapping(String entity, String schema){
		schemaMapping.put(entity, schema);
	}
	
	public static String getSchema(String entity){
		return schemaMapping.get(entity);
	}
	
	public static void setSchemaMapping(HashMap<String, String> map){
		schemaMapping = map;
	}
	
	public static Map<String, String> getSchemaMapping(){
		return schemaMapping;
	}

	public static void clear() {
		schemaMapping.clear();
		
	}
	
	public static void setSchema(String schema){
		currentSchema = schema;
	}
	
	public static String getSchema(){
		return currentSchema;
	}
	
	public static Class getClass(String name) {
		if (getSchema(name) == null)
			return null;
		Class clazz = null;
		try {
			clazz = Class.forName("jsdai.S".concat(getSchema(name)).concat(".C").concat(name));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return clazz;
	}
	
}
