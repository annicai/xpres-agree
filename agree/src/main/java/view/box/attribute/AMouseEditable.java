package view.box.attribute;

/**
 * Attributes that are editable by clicking on the entitybox. 
 *
 */
public interface AMouseEditable {

	public abstract String next();

	public abstract String previous();

	public abstract String setCurrent();
}
