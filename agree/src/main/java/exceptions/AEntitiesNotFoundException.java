package exceptions;

import org.eclipse.swt.SWT;

public class AEntitiesNotFoundException extends AException {
		
	public AEntitiesNotFoundException(String entities){
		super(entities.concat(" cannot be found in the imported STEP file and will not be parsed.\nThis can cause problems if other entities are related to ").concat(entities).concat("."), 
					"Entity not found", SWT.ICON_WARNING);
	}
}
