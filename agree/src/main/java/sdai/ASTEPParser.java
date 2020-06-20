package sdai;

import java.util.LinkedList;

import exceptions.AMissingEntitiesException;
import jsdai.lang.AEntity;
import jsdai.lang.ASdaiModel;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;
import jsdai.lang.SdaiModel;
import model.AModelImpl;
import view.tree.display.AInstanceNode;
import view.tree.display.ATreeViewerNode;

public class ASTEPParser {
	
	public static void parseSTEP(AModelImpl amodel) throws SdaiException, AMissingEntitiesException{
		String errorMessage = "";

		ASdaiModel models = ASdaiHandler.getModels();
		SdaiIterator modelIterator = models.createIterator();
		while(modelIterator.next()){
			SdaiModel model = models.getCurrentMember(modelIterator);
			AEntity entities = model.getInstances();
			SdaiIterator entityIterator = entities.createIterator();
			while (entityIterator.next()){
					EEntity current = entities.getCurrentMemberEntity(entityIterator);
					String className = current.getClass().getName();
					String[] entityName = className.split("\\.");
					String entity = entityName[entityName.length-1].substring(1);
					LinkedList<ATreeViewerNode> nodes = AModelImpl.entityMapping.get(entity);
					if (nodes != null){
						LinkedList<ATreeViewerNode> entityNodes = new LinkedList<ATreeViewerNode>();					
						for(ATreeViewerNode node: nodes){
							AInstanceNode entityNode = new AInstanceNode(node);
							entityNode.setText(current.getPersistentLabel());
							entityNodes.add(entityNode);
							node.addChild(entityNode);
							AModelImpl.modelMapping.put(entityNode, current);
						}
						AModelImpl.entityMapping.put(current.getPersistentLabel(), entityNodes);
					}
					else { 
						errorMessage = errorMessage.concat("\n" + current + " could not be added since entity class E" + entity + " is missing.");
					}
			}
		}
		if (errorMessage.length() > 1){
			throw new AMissingEntitiesException(errorMessage);
		}
	}

}
