package view.edit.table;

import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;

public class ATableInstance extends ATableItem{
	
	private EEntity entity;
	private int index = 1;
	
	public ATableInstance(EEntity entity){
		this.entity = entity;
	}
	
	public EEntity getEntity(){
		return entity;
	}
	
	public Class getEntityClass(){
		return entity.getClass();
	}
	
	public String toString(){
		try {
			return entity.getPersistentLabel().concat(": ").concat(entity.getClass().getSimpleName().substring(1));
		} catch (SdaiException e) {
			e.printStackTrace();
		}
		return "ERROR";
	}
}
