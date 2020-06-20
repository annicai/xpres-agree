package exceptions;

import org.eclipse.swt.SWT;

public class AWrongFormatException extends AException {
	
	public AWrongFormatException(String returntype){
		super("Unable to parse input to \"".concat(returntype).concat( "\"."), 
				"Wrong format", SWT.ERROR);
	}

}
