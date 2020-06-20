package exceptions;

import org.eclipse.swt.SWT;

public class AMissingEntitiesException extends AException{

	public AMissingEntitiesException(String message) {
		super(message, "Missing entities", SWT.ICON_WARNING);
	}

}
