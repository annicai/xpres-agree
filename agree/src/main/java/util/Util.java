package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import sdai.structure.ASchema;

public class Util{
 
	 private static String[] CLASS_PATH_PROP = { "java.class.path", "java.ext.dirs", "sun.boot.class.path"};
   
     private static List<File> CLASS_PATH_ARRAY = getClassPath();
     
     private static List<File> getClassPath() {
    	 
         List<File> ret = new ArrayList<File>();   
         String delim = ":";
         if (System.getProperty("os.name").indexOf("Windows") !=-1) {
             delim = ";"; 
         }
         for (String pro : CLASS_PATH_PROP) {
        	 String[] pathes = System.getProperty(pro).split(delim);
             for(String path : pathes)
            	 ret.add(new File(path));
         }
         return ret;   
     }

     public static List<String> getClassInPackage(String pkgName) {
    	 
    	 List<String> ret = new ArrayList<String>();
         String rPath = pkgName.replace('.', '/') + "/";
         try {
        	 
        	 
        	 for (String dynamicSchema: ASchema.dynamicSchemas){
        		 
        		 if (pkgName.equals(dynamicSchema)){
        			 
        			 FileInputStream fis = new FileInputStream(ASchema.dynamicSchemaPaths.get(dynamicSchema));
 		             JarInputStream jis = new JarInputStream(fis, false);
 		             JarEntry e = null;
 		             while ((e=jis.getNextJarEntry()) != null) {
 		            	   String eName = e.getName();
 		            	   if(eName.startsWith(rPath) && !eName.endsWith("/")){
 		            		   ret.add(eName.replace('/','.').substring(0,eName.length()-6));
 		            	   }
 		            	   jis.closeEntry();
 		              }
 		              jis.close();
 		              return  ret;
    	    	  }
            }            

	        for (File classPath : CLASS_PATH_ARRAY) {
	        	if (!classPath.exists()) {
	        		continue;
	        	}
			    if (classPath.isDirectory()) {
			    	
			    	File dir = new  File(classPath, rPath);
			        if (!dir.exists()) continue;
				    for (File file : dir.listFiles()) {
				    	if (file.isFile()) {
				    		String clsName = file.getName();
						    clsName = pkgName + "." + clsName.substring(0, clsName.length() - 6);
						    ret.add(clsName);
						}
				    }
		        } else {
		        	
		        	FileInputStream fis = new FileInputStream(classPath);
			        JarInputStream jis = new JarInputStream(fis, false);
			        JarEntry e = null;
			        while ((e=jis.getNextJarEntry())!=null) {
			        	
			        	String eName = e.getName();
			            if (eName.startsWith(rPath) && !eName.endsWith("/")) {
			            	ret.add(eName.replace('/','.').substring(0,eName.length()-6));
			            }
			            jis.closeEntry();
			        }
			        jis.close();
		         }
	        	}
            } catch(Exception e) {
            	throw new RuntimeException(e);
            }
            return  ret;      
    }
     
	 public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
		    byte[] buffer = new byte[1024];
		    int len;
	
		    while((len = in.read(buffer)) >= 0)
		      out.write(buffer, 0, len);
	
		    in.close();
		    out.close();
	 }
}