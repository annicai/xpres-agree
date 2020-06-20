package main;
import java.io.File;
import java.net.URL;

import sdai.ASdaiHandler;
import sdai.structure.ASchema;
import view.AViewImpl;
import xml.AXMLParser;
import model.AModelImpl;
import controller.AControllerImpl;

public class Main {
	
	public static void main(String[] args){
		
		ASchema.addSchemas();		
		ASdaiHandler.startSession();
		
		AControllerImpl controller = new AControllerImpl();
		AXMLParser.init(controller);
		AModelImpl model = new AModelImpl();
		AViewImpl view = new AViewImpl();
		view.setModel(model);
		controller.setModel(model);
		controller.setView(view);
		view.addObserver(controller);
		model.addObserver(view);
		try {
			URL textURL = Main.class.getResource(File.separator.concat("resources").concat(File.separator).concat("basic_short_names.sn"));
			model.readShortNames(textURL);
		} catch (Exception e){
			//e.printStackTrace();
		}
		
		view.build();	
		controller.addListeners();	
		try {
			view.display();
		} catch (java.lang.OutOfMemoryError oomError){
			System.out.println("Out of memory exception: " + oomError.getMessage());
			oomError.printStackTrace();
		}

	}

}