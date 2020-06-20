package sdai.structure;

import java.io.Serializable;
import java.util.LinkedList;

import jsdai.dictionary.EEntity_definition;
import jsdai.dictionary.EExtended_select_type;
import jsdai.lang.AEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;

/**
 * Contains the extensions to a EExtensible_select_type.
 *
 */
public class ASelectExtension implements Serializable{
	
	private LinkedList<String> extensions = new LinkedList<String>();
	private String select_type;
	
	public ASelectExtension(String name) {
		select_type = name;
	}
	
	public LinkedList<String> getSelectExtensions(){
		return extensions;
	}

	public void addExtendedSelectType(EExtended_select_type est){
		AEntity selects;
		try {
			selects = est.getLocal_selections(null);
			SdaiIterator selIterator = selects.createIterator();
			while (selIterator.next()){
				if (selects.getCurrentMemberEntity(selIterator) instanceof EEntity_definition){
					String entity_def = ((EEntity_definition) selects.getCurrentMemberEntity(selIterator)).getName(null).substring(0, 1).toUpperCase();
					String entity = entity_def.concat(((EEntity_definition)selects.getCurrentMemberEntity(selIterator)).getName(null).substring(1));
					if (! extensions.contains(entity))
						extensions.add(entity);
				}
			}
		} catch (SdaiException e) {
			e.printStackTrace();
		} 

	}

}
