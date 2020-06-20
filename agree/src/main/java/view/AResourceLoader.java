package view;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import controller.AState;

public class AResourceLoader {
	
	public static Image getSmallAgreeLogo() {
    	return AResourceLoader.getImage("logo.png");
	}
	
	public static Image getBigAgreeLogo() {
		return AResourceLoader.getImage("agree_big.png");
	}
	
	public static Image getSplash() {
    	return AResourceLoader.getImage("splash.png");
	}
	
	public static Image getExpandImage() {
		return AResourceLoader.getImage("expand.png");
	}
	
	public static Image getImage(String resource) {
    	URL imgURL = AResourceLoader.getResource(resource);
		ImageDescriptor descriptor = ImageDescriptor.createFromURL(imgURL);
		return descriptor.createImage();
	}
	
	public static File getFile(String resource) throws IOException {
		URL url = AResourceLoader.getResource(resource);
			
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        BufferedWriter writer = new BufferedWriter(new FileWriter(AState.getTempDir().concat(resource)));
        String line;
	   	while ((line = reader.readLine()) != null){
	   		writer.write(line);
	   		writer.newLine();
	   	}
	   		   	
	    reader.close();
	    writer.close();
			
		return new File(AState.getTempDir().concat(resource));
	}
	
	public static File getPdfFile(String resource) throws IOException {
        InputStream fis = AResourceLoader.class.getClassLoader().getResourceAsStream(resource);
        
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         byte[] buf = new byte[1024];
         try {
                for (int readNum; (readNum = fis.read(buf)) != -1;) {
                    bos.write(buf, 0, readNum);
                }
         } catch (IOException ex) { }
         byte[] bytes = bos.toByteArray();
         File pdfFile = File.createTempFile(resource.split("\\.")[0], ".pdf");
         FileOutputStream fos = new FileOutputStream(pdfFile);
         fos.write(bytes);
         fos.flush();
         fos.close();
         return pdfFile;
	}
	
	
	public static URL getResource(String name) {
			return AResourceLoader.class.getClassLoader().getResource(name);
	}

}
