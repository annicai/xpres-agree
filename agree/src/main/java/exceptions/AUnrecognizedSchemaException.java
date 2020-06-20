package exceptions;

import org.eclipse.swt.SWT;

public class AUnrecognizedSchemaException extends AException{

	public AUnrecognizedSchemaException() {
		super("Unable to open STEP file.\nSchema not recognized." , "Unidentified schema." , SWT.ERROR);
	}

} 
