package xml;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import model.AModelImpl;

import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import controller.AControllerImpl;
import controller.AState;
import exceptions.AEntitiesNotFoundException;
import sdai.structure.ASchema;
import view.ATextBox;
import view.AViewImpl;
import view.bendpoint.ABendPoint;
import view.bendpoint.AConnectionRouter;
import view.bendpoint.AConnectionShaper;
import view.box.AEntityBox;
import view.tree.display.ATreeViewerNode;


public class AXMLParser {
	
	private static DocumentBuilderFactory dbf;
	private static DocumentBuilder db;
	private static Document document;
	private static AControllerImpl controller;
	private static Element model;
	
	public static void init(AControllerImpl c){
		controller = c;
		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) { 
			e.printStackTrace();
		}
	}
	
	public static File createXML(String filePath, Figure contents, Figure paintContents){
		Point location = contents.getLocation();
		contents.setLocation(new Point(0,0));
		paintContents.setLocation(new Point(0,0));
		
		try {
					document = db.newDocument();
			
					Element graphical = document.createElement("graphical_instance");
					document.appendChild(graphical);

					Element schema = document.createElement("schema");
					graphical.appendChild(schema);
					schema.setAttribute("name", ASchema.getSchema());
					
					Element pages = document.createElement("pages");
					graphical.appendChild(pages);
					
					Element page = document.createElement("page");
					pages.appendChild(page);
					page.setAttribute("name", "Page 1");
					page.setAttribute("id", "0");
					
					model = document.createElement("model");
					graphical.appendChild(model);
					model.setAttribute("name", "");
					model.setAttribute("units", "pixels");
					

		/*			for (String key: controller.getModel().definedShortNames.keySet()){
						Element shortElement = document.createElement("shortname");
						shortElement.setAttribute("entity", key);
						String sns = "";
						for (String value: controller.getModel().definedShortNames.get(key)){
							sns = sns.concat("&&".concat(value));
						}
						
						shortElement.setAttribute("short", sns);
						model.appendChild(shortElement);
					}*/

					List<Figure> figures = contents.getChildren();
		    		for (Figure box: figures){
		    			if (box instanceof AEntityBox){ 
		    				AEntityBox entitybox = (AEntityBox) box;
		    				createElementInstance(entitybox);
		    			}

		    		}
					figures = paintContents.getChildren();
		    		for (Figure figure: figures){
						Element paintElement = document.createElement("figure");
						model.appendChild(paintElement);
						paintElement.setAttribute("type", figure.getClass().getSimpleName());
						paintElement.setAttribute("left", Integer.toString(figure.getLocation().x));
						paintElement.setAttribute("top", Integer.toString(figure.getLocation().y));
						paintElement.setAttribute("right", Integer.toString(figure.getLocation().x + figure.getSize().width));
						paintElement.setAttribute("bottom", Integer.toString((figure.getLocation().y + figure.getSize().height)));
						RGB rgb = figure.getBackgroundColor().getRGB();	// 2012-02-02
						if (figure.getClass() == ATextBox.class){
							paintElement.setAttribute("text", ((ATextBox) figure).getText());
							rgb = figure.getForegroundColor().getRGB();
						}
						String color = getColorString(rgb);
						paintElement.setAttribute("fill_color", color);	
						rgb = figure.getForegroundColor().getRGB();
						color = getColorString(rgb);
						
						paintElement.setAttribute("line_color", color);
						if (figure instanceof Shape){
							paintElement.setAttribute("line_width", Integer.toString(((Shape) figure).getLineWidth()));
						}
						else if (figure instanceof ATextBox){
							paintElement.setAttribute("text_size", Integer.toString(((Label) figure).getFont().getFontData()[0].getHeight()));
						}
		    		} 
		    		
		
			TransformerFactory transFac = TransformerFactory.newInstance();
			Transformer trans = transFac.newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "yes");

			File file = new File(filePath);
			StreamResult result = new StreamResult(file.toURI().getPath());
			DOMSource source = new DOMSource(document);
			trans.transform(source, result);
			contents.setLocation(location);
			paintContents.setLocation(location);
			return file;
			
		} catch (TransformerConfigurationException e) { e.printStackTrace();
		} catch (TransformerException e) { e.printStackTrace();
		}

		return null;
	}
	
	private static String getColorString(RGB rgb) {
		String red = Integer.toString(rgb.red);
		if (red.length()==1) red = "00".concat(red);
		else if (red.length()==2) red = "0".concat(red);
		String green = Integer.toString(rgb.green);
		if (green.length()==1) green = "00".concat(green);
		else if (green.length()==2) green = "0".concat(green);
		String blue = Integer.toString(rgb.blue);
		if (blue.length()==1) blue = "00".concat(blue);
		else if (blue.length()==2) blue = "0".concat(blue);
		return red.concat(green).concat(blue);
	}

	/**
	 * 
	 * @param entitybox
	 */
	private static void createElementInstance(AEntityBox entitybox) {
		Element instance = document.createElement("instance");
		model.appendChild(instance);
		
			try {
				instance.setAttribute("id", entitybox.getEntityRepresentation().getPersistentLabel().replace("#", ""));
				instance.setAttribute("left", Integer.toString(entitybox.getLocation().x));
				instance.setAttribute("top", Integer.toString(entitybox.getLocation().y));
				instance.setAttribute("right", Integer.toString(entitybox.getLocation().x + entitybox.getSize().width));
				instance.setAttribute("bottom", Integer.toString((entitybox.getLocation().y + entitybox.getSize().height)));
				
				String name =  entitybox.getLabel().getText().replace(" ", "").replace(":", "").replaceFirst("#[0-9]+", "");
				instance.setAttribute("name", name);
				
				RGB rgb = entitybox.getBackgroundColor().getRGB();
				String color = getColorString(rgb);
				instance.setAttribute("fill_color", color);	
				instance.setAttribute("line_color", "0");	
				for (PolylineConnection connection: entitybox.getConnections()){ // Outgoing connections 
						Element relationship = document.createElement("relationship");
						model.appendChild(relationship);
						
						String froms = entitybox.getEntityRepresentation().getPersistentLabel().replace("#", "");
						relationship.setAttribute("from", froms);

						relationship.setAttribute("name", ((Label) connection.getChildren().get(0)).getText());
						relationship.setAttribute("udistance", Integer.toString(AModelImpl.getLocator(((Label) connection.getChildren().get(0))).getUDistance()));
						
						
						int index=0;
						while (connection.getSourceAnchor().getOwner() instanceof AConnectionShaper){
							AConnectionShaper bp = (AConnectionShaper) connection.getSourceAnchor().getOwner();
							Element bendpoint = document.createElement("bendpoint");
							relationship.appendChild(bendpoint);
							bendpoint.setAttribute("top", Integer.toString(bp.getLocation().x));
							bendpoint.setAttribute("left", Integer.toString(bp.getLocation().y));
							bendpoint.setAttribute("index", Integer.toString(index));
							connection = (PolylineConnection) bp.getInConnection();
							index++;
						}
						
						if (connection.getSourceAnchor().getOwner() instanceof AConnectionRouter){
							AConnectionRouter router = (AConnectionRouter) connection.getSourceAnchor().getOwner();
							Element aggregate = document.createElement("aggregate");
							relationship.appendChild(aggregate);
							aggregate.setAttribute("id", Integer.toString(router.getIndex()));
							aggregate.setAttribute("top", Integer.toString(router.getLocation().x));
							aggregate.setAttribute("left", Integer.toString(router.getLocation().y));
						}

						if (connection.getTargetAnchor().getOwner() instanceof AEntityBox){ // && !(connection.getTargetAnchor().getOwner() instanceof GIGroupBox)  ){
							String s = ((AEntityBox) connection.getTargetAnchor().getOwner()).getEntityRepresentation().getPersistentLabel().replace("#", "");
							relationship.setAttribute("to", s);
						}				
						relationship.setAttribute("page", "0");
						relationship.setAttribute("line_color", "0");
					}
			} catch (DOMException e) {
				e.printStackTrace();
			} catch (SdaiException e) {
				e.printStackTrace();
			}
		
	}
	
	public static String getSchemaName(File file){
		  try {
			  document = db.parse(file);
			  NodeList nodeLst = document.getElementsByTagName("schema");
			  return (nodeLst.item(0).getAttributes().getNamedItem("name").getTextContent());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Method for reading and opening a saved file.
	 * Is compatible with xml:s created in EuroSteps GraphicalInstance.
	 * 
	 * @param file
	 * @throws AEntitiesNotFoundException 
	 */
	public static boolean readXML(File file, AViewImpl view) throws AEntitiesNotFoundException{
		
		Map <String, AEntityBox> boxMapping = new HashMap <String, AEntityBox>();
		
		try {
			  document = db.parse(file);
			  AState.lPoint = null;

		/*	  NodeList shrtLst = document.getElementsByTagName("shortname");
			  for (int i = 0; i < shrtLst.getLength(); i++) {

				    Node currentNode = shrtLst.item(i);
				    
				    if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				  
				      Element currentElement = (Element) currentNode;
				      NamedNodeMap attributes = currentElement.getAttributes();
				      String entity = attributes.getNamedItem("entity").getTextContent();
				      String sns = attributes.getNamedItem("short").getTextContent();
				      String [] snlist = sns.split("&&");
				      for (int s=0; s < snlist.length; s++){
				    	  if (controller.getModel().definedShortNames.containsKey(entity)){
					    	  controller.getModel().definedShortNames.get(entity).add(snlist[s]);
				    	  }
				    	  else {
				    		  LinkedList<String> ll = new LinkedList<String>();
				    		  ll.add(snlist[s]);
					    	  controller.getModel().definedShortNames.put(entity, ll);
				    	  }
				    	  if (controller.getModel().shortNames.containsKey(entity)){
					    	  controller.getModel().shortNames.get(entity).add(snlist[s]);
				    	  }
				    	  else {
				    		  LinkedList<String> ll = new LinkedList<String>();
				    		  ll.add(snlist[s]);
					    	  controller.getModel().shortNames.put(entity, ll);
				    	  }

				      }
				    }
			  } */

			  NodeList nodeLst = document.getElementsByTagName("instance");

			  for (int i = 0; i < nodeLst.getLength(); i++) {

			    Node currentNode = nodeLst.item(i);
			    
			    if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
			  
			      Element currentElement = (Element) currentNode;
			      NamedNodeMap attributes = currentElement.getAttributes();
			      String entityString = "#".concat(attributes.getNamedItem("id").getTextContent());
			      
			      ATreeViewerNode item = AModelImpl.entityMapping.get(entityString).getFirst();
			      EEntity entity = AModelImpl.modelMapping.get(item);
			      
			      int left =  Integer.parseInt(attributes.getNamedItem("left").getTextContent());
			      int top = Integer.parseInt(attributes.getNamedItem("top").getTextContent());
			      int right = Integer.parseInt(attributes.getNamedItem("right").getTextContent());
			      int bottom = Integer.parseInt(attributes.getNamedItem("bottom").getTextContent());
			      
			      String color = attributes.getNamedItem("fill_color").getTextContent();
			      int red; int green; int blue;
			      if (color.length() == 9){
			    	  red = Integer.parseInt(color.substring(0,3));
			    	  green = Integer.parseInt(color.substring(3,6));
			    	  blue = Integer.parseInt(color.substring(6,9));
			      }
			      else {
			    	  red=255;green=255;blue=206;
			      }
			      if (AModelImpl.modelMapping.containsValue(entity)){
				      AEntityBox box = controller.createEntityBox(entity, new Point(left, top), false);
				      box.setBounds(new Rectangle(left, top, (right-left), (bottom-top)));
				      box.setVisible(true);
				      box.setBackgroundColor(new Color(null, red, green, blue));				      
				      boxMapping.put(attributes.getNamedItem("id").getTextContent(), box);			      
			      }
			      else throw new AEntitiesNotFoundException(entityString);
			      
			    }
		    }
			      
		    NodeList relationLst = document.getElementsByTagName("relationship");
		    
			for (int i = 0; i < relationLst.getLength(); i++) {

			  Node currentNode = relationLst.item(i);
			  
			  if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				  
				  Element currentElement = (Element) currentNode;
				  NamedNodeMap attributes = currentElement.getAttributes();
				  AEntityBox box = boxMapping.get(attributes.getNamedItem("from").getTextContent());
				  String name = attributes.getNamedItem("name").getTextContent();

				  PolylineConnection connection = null;
				  for (PolylineConnection c: box.getConnections()){
					  if (((Label) c.getChildren().get(0)).getText().equals(name)){
						  connection = c;
						  try {
						      int udistance = Integer.parseInt(attributes.getNamedItem("udistance").getTextContent());
						      AModelImpl.getLocator(((Label) c.getChildren().get(0))).setUDistance(udistance);
						  }
						  catch (Exception e){ e.printStackTrace(); }
					  }
					  
					  NodeList bendPoints = currentElement.getElementsByTagName("bendpoint");
					  for (int j = 0; j < bendPoints.getLength(); j++) {
						  Node bp = null;
						  for (int k = 0; k < bendPoints.getLength(); k++) {
							  bp = bendPoints.item(k);
							  if (bp.getNodeType() == Node.ELEMENT_NODE){
								  Element bpElement = (Element) bp;
								  NamedNodeMap bpAttributes = bpElement.getAttributes();
								  if (Integer.parseInt(bpAttributes.getNamedItem("index").getTextContent()) == j){
									  ABendPoint cs = box.addBendpoint(connection);
									  cs.setBounds(new Rectangle(Integer.parseInt(bpAttributes.getNamedItem("top").getTextContent()), Integer.parseInt(bpAttributes.getNamedItem("left").getTextContent()),7,7));
									  view.addToContents(cs);
									  view.addToContents(cs.getInConnection());

								  }
							  }
						  }
					  }
					  NodeList aggregates = currentElement.getElementsByTagName("aggregate");
					  for (int k = 0; k < aggregates.getLength(); k++) {
						  Node agg = aggregates.item(k);
						  if (agg.getNodeType() == Node.ELEMENT_NODE){
							  Element aggElement = (Element) agg;
							  NamedNodeMap aggAttributes = aggElement.getAttributes();
							  try {
								  if (connection.getSourceAnchor().getOwner() instanceof AConnectionRouter){
									  AConnectionRouter cr = (AConnectionRouter) connection.getSourceAnchor().getOwner();
									  cr.setBounds(new Rectangle(Integer.parseInt(aggAttributes.getNamedItem("top").getTextContent()), Integer.parseInt(aggAttributes.getNamedItem("left").getTextContent()),7,7));
									  connection.layout(); 
								  }
							  } catch (NullPointerException e){} 
							  
						  }
					  }
				  }

				  
			  }
			}	
			NodeList figureLst = document.getElementsByTagName("figure");	
			for (int i = 0; i < figureLst.getLength(); i++) {
				Node currentNode = figureLst.item(i);
				
				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				      Element currentElement = (Element) currentNode;
				      NamedNodeMap attributes = currentElement.getAttributes();
				      
				      int left =  Integer.parseInt(attributes.getNamedItem("left").getTextContent());
				      int top = Integer.parseInt(attributes.getNamedItem("top").getTextContent());
				      int right = Integer.parseInt(attributes.getNamedItem("right").getTextContent());
				      int bottom = Integer.parseInt(attributes.getNamedItem("bottom").getTextContent());
				      String type = attributes.getNamedItem("type").getTextContent();
				      Figure rectangle = null;
				      if (type.equals("RoundedRectangle")){
				    	  rectangle = new RoundedRectangle();
				    	  ((RoundedRectangle) rectangle).setCornerDimensions(new Dimension(50,50));
				      }
				      else if (type.equals("RectangleFigure")){
				    	  rectangle = new RectangleFigure();
				      }
				      else if (type.equals("Ellipse")){
				    	  rectangle = new Ellipse();
				      }
				      else if (type.equals("ATextBox")){
				    	  String text = attributes.getNamedItem("text").getTextContent();
				    	  rectangle = new ATextBox();
				    	  ((ATextBox) rectangle).setText(text);
				      }
				      if (rectangle != null){
					      if (!type.equals("ATextBox"))
					    	  rectangle.setOpaque(true);
					      else rectangle.setOpaque(false);
					      String color = attributes.getNamedItem("fill_color").getTextContent();
					      int red; int green; int blue;
					      red = Integer.parseInt(color.substring(0,3));
					      green = Integer.parseInt(color.substring(3,6));
					      blue = Integer.parseInt(color.substring(6,9));
					      if (! type.equals("ATextBox")){
					    	  rectangle.setBackgroundColor(new Color(null, red, green, blue));
					    	  color = attributes.getNamedItem("line_color").getTextContent();
						      red = Integer.parseInt(color.substring(0,3));
						      green = Integer.parseInt(color.substring(3,6));
						      blue = Integer.parseInt(color.substring(6,9));
					    	  rectangle.setForegroundColor(new Color(null, red, green, blue));
					    	  int width = Integer.parseInt(attributes.getNamedItem("line_width").getTextContent());
					    	  ((Shape) rectangle).setLineWidth(width);
					      }
					      else {
					    	  rectangle.setForegroundColor(new Color(null, red, green, blue));
					    	  int height = Integer.parseInt(attributes.getNamedItem("text_size").getTextContent());
					    	  FontData data = new FontData();
							  data.setHeight(height);
							  Font font = view.getTextFont(height);
							  ((ATextBox) rectangle).setFont(font);
					      }
				    	  rectangle.setBorder(null); 
				    	  if (rectangle instanceof Shape)
				    		  controller.getView().addToPaintContents((Shape) rectangle);
				    	  else if (rectangle instanceof ATextBox)
				    		  controller.getView().addToPaintContents((ATextBox) rectangle);
				    	  rectangle.setBounds(new Rectangle(left, top, (right-left), (bottom-top)));
				    	  view.refresh();
				      }
				}
			}

			
		} catch (SAXException|IOException|NullPointerException e) {
			e.printStackTrace();
		}
		return true;
	}
}
