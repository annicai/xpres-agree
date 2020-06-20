package exceptions;

import org.eclipse.swt.SWT;

public class AAbstractEntityException extends AException{

	public AAbstractEntityException(String message) {
		super(message, "Abstract entity", SWT.ERROR);
	}

}
