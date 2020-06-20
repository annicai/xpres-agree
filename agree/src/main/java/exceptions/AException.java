package exceptions;

public class AException extends Exception{
	
	private int type;
	private String title;

	public AException(String message, String title,  int type) {
		super(message);
		this.title = title;
		this.type = type;
	}
	
	public String getTitle(){
		return title;
	}
	
	public int getType(){
		return type;
	}
	
}
