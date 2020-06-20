package view.box.attribute;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import controller.AState;
import jsdai.dictionary.EDefined_type;
import jsdai.lang.AEntity;
import jsdai.lang.A_double;
import jsdai.lang.A_integer;
import jsdai.lang.A_string;
import jsdai.lang.Aa_double;
import jsdai.lang.Aa_integer;
import jsdai.lang.Aggregate;
import jsdai.lang.CAggregate;
import jsdai.lang.EEntity;
import jsdai.lang.SdaiException;
import jsdai.lang.SdaiIterator;

public class AAAttribute extends AAttribute {
	
	private int aggIndex = 1;

	public AAAttribute(String attribute_name, Object argument, EEntity entity) {
		super(attribute_name, argument, entity);
	}

	@Override
	public Object getAttributeValue() {
		Object o = null;
		Object arglist[] = new Object[1];
        arglist[0] = null;
		try {
			o = getMethod.invoke(entity, arglist);
		} catch (IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {}
		return o;
	}
	
	public int getAggregateCount(){
		if (returnInfo.aggLevel > 1){
			Object o = getAttributeValue();
			if (o instanceof Aggregate){
				try {
					return ((Aggregate) o).getMemberCount();
				} catch (SdaiException e) {
					e.printStackTrace();
				}
			}
		}
		return -1;
	}
	
	public void setAttributeList(List<?> value, Aggregate aggregate) {
		try {
			int index = 1;
			for(Object o: value){
				if (o instanceof List<?>){
					setAttributeList((List<?>) o, aggregate.createAggregateByIndex(index, null)); 
				}
				else {
					aggregate.addByIndex(index, o, null);
				}
				index ++;
			}
		} catch (SdaiException e) {	e.printStackTrace();
		} catch (IllegalArgumentException e) {	e.printStackTrace(); }

	}
	
	@Override
	public void setAttribute(Object value) {
		AState.hasChanged = true;
		if (value instanceof List<?>){
			Object arglist[] = new Object[1];
	        arglist[0] = null;
			Aggregate aggregate = null;
			try {
				aggregate = (Aggregate) setMethod.invoke(entity, arglist);
			} catch (IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {	e.printStackTrace(); }
			setAttributeList((List<?>) value, aggregate);
			return;
		}

		Object arglist[] = new Object[1];
        arglist[0] = null;
		if (returnInfo.aggLevel > 1){
			try {
				boolean notSet = false;
				try {
					Object o = getMethod.invoke(entity, arglist);	
				} catch(InvocationTargetException e){
					notSet=true;
				}
				if (notSet){ 
					CAggregate agg = (CAggregate) setMethod.invoke(entity, arglist);
					AEntity aentity = (AEntity) agg.addAggregateByIndex(1, null);
					aentity.addUnordered((EEntity) value);
					return;
				}
				else {	//FIXME: Levels > 2
					CAggregate  agg = (CAggregate) getMethod.invoke(entity, arglist);
					if (aggIndex > agg.getMemberCount()){
						AEntity aentity = (AEntity) agg.addAggregateByIndex(aggIndex, null);
						aentity.addUnordered((EEntity) value);
					}
					else {
						Object o = agg.getByIndexObject(aggIndex);
						if (o instanceof AEntity){
							((AEntity)o).addUnordered((EEntity) value);
						}
					}
				}
			} catch (Exception e){e.printStackTrace();}
		}
		else {
			boolean notSet = false;
			try {
				Object o = getMethod.invoke(entity, arglist);
			} catch(InvocationTargetException e){
				notSet=true;
			} catch (IllegalArgumentException|IllegalAccessException e) { e.printStackTrace();}
		    try {
		    	 if (notSet){ 
		    		 try { // Throws exception if Aggregate (type and not EEntity)
		    			 ((AEntity)setMethod.invoke(entity, arglist)).addUnordered((EEntity) value);
		    			 return;
		    		 } catch (ClassCastException e) {}
		    		 ((Aggregate) getMethod.invoke(entity, arglist)).addUnordered(value, null);
		 	     }
		 	    else {
		 	    	try {
		 	    		// Throws exception if Aggregate (type and not EEntity)
			 	        if (((AEntity) getMethod.invoke(entity, arglist)).isMember((EEntity) value)){
			 	        		return;	// Entity already exists in aggregate 
			 	        	}
			 	    	}catch (Exception e){}
		 	    	if (getMethod.invoke(entity, arglist) instanceof AEntity)
		 	    		((AEntity) getMethod.invoke(entity, arglist)).addUnordered((EEntity) value);
		 	    	else ((Aggregate) getMethod.invoke(entity, arglist)).addUnordered(value, null);
		 	    }	
			} catch (IllegalArgumentException|SdaiException|
					IllegalAccessException|InvocationTargetException e) { e.printStackTrace();

		}
	    
		}
	}
	
	public void unsetAttribute(EEntity rEntity) {
		Object arglist[] = new Object[1];
		arglist[0] = null;
		try {
			Object o = getMethod.invoke(entity, arglist);
			if (o instanceof AEntity){
				//removeOrdered(o, rEntity);
				((AEntity) o).removeUnordered(rEntity);
			}
			else if (returnInfo.aggLevel > 1 && o instanceof CAggregate){
				Object agg = ((CAggregate) o).getByIndexObject(aggIndex);
				if (agg instanceof AEntity){
					removeOrdered((CAggregate) agg, rEntity);
				}
			}
		//	else if (o instanceof CAggregate){
		//		CAggregate innerAgg = (CAggregate) o;
		//		removeFromInnerAggregate(innerAgg, rEntity);
		//	}
		} catch (IllegalArgumentException|SdaiException|
				IllegalAccessException|InvocationTargetException e) { 	e.printStackTrace();
		}
	}
	
/*	private void removeFromInnerAggregate(CAggregate innerAgg, EEntity rEntity) {
		SdaiIterator it;
		try {
			it = innerAgg.createIterator();
			while (it.next()){
				Object innerO = innerAgg.getCurrentMemberObject(it);
				if (innerO instanceof AEntity && ((AEntity) innerO).isMember(rEntity)){
					removeOrdered(innerO, rEntity);
				}
				else if (innerO instanceof CAggregate){
					removeFromInnerAggregate((CAggregate)innerO, rEntity);
				}
			}
		} catch (SdaiException e) { e.printStackTrace(); }
	}*/
	
	private void removeOrdered(Object innerO, EEntity rEntity) {
		int mc;
		try {
			mc = ((AEntity) innerO).getMemberCount();
			for (int i=1; i <= mc; i++){
				if (((AEntity) innerO).getByIndexEntity(i) == rEntity){
					((AEntity) innerO).removeByIndex(i);
					return;
				}
			}
		} catch (SdaiException e) {	e.printStackTrace();
		}
		
	}
	
	/**
	 * @return index of @param entity
	 */
	public int getSingleIndexOf(CAggregate aentity, EEntity entity){
		try {
			for (int i = 1; i <= aentity.getMemberCount(); i ++){
				if (aentity.getByIndexObject(i) == entity){
					return i;
				}
			}
		} catch (SdaiException e) {	e.printStackTrace();
		}
		return -1;
	}
	
	public int getIndexOf(CAggregate agg, EEntity entity){
		try {
			if (agg instanceof AEntity){
				return getSingleIndexOf((AEntity) agg, entity);
			}
			int aggSize = agg.getMemberCount();
			for (int i=1; i <= aggSize; i++){
				if (agg.getByIndexObject(i) instanceof AEntity && ((AEntity) agg.getByIndexObject(i)).isMember(entity)){
					return getSingleIndexOf((AEntity)agg.getByIndexObject(i), entity);
				}
				else if (agg.getByIndexObject(i) == entity){
					return i;
				}
			}
		} catch (SdaiException e) {	e.printStackTrace();
		} catch (NullPointerException e) { e.printStackTrace(); 
		}
		return -1;
	}
	
	
	/**
	 * Returns the index of the list in which an entity object is contained. (Aa's)
	 **/
	public int getListIndex(EEntity aEntity){
        Object arglist[] = new Object[1];
        arglist[0] = null;        ///TODO? Aaa... with selects!!!!
		CAggregate agg;
		try {
			agg = (CAggregate) getMethod.invoke(entity, arglist);
			int aggSize = agg.getMemberCount();
			for (int i=1; i <= aggSize; i++){
				if (agg.getByIndexObject(i) instanceof CAggregate && ((CAggregate) agg.getByIndexObject(i)).isMember(aEntity)){
					return i;
				}
			}
		} catch (IllegalArgumentException e) { e.printStackTrace();
		} catch (IllegalAccessException e) { e.printStackTrace();
		} catch (InvocationTargetException e) { e.printStackTrace();
		} catch (SdaiException e) { e.printStackTrace();
		}
		return -1;
	}
	
	@Override
	public String getToolTip(){
		return "Type: ".concat(returnInfo.returntype).concat("\nDouble-click and type a new value to set the attribute. \n"
				+ "Aggregates are grouped by parenthesises, eg ((A, B), (C, D)) for a double aggregate.");
	}
	
	@Override
	public boolean containsValue(Object value) {
		try {
			if (returnInfo.aggLevel > 1)			//FIXME! 
				return false;
			if (value instanceof EEntity){
	    		 try { // Throws exception if Aggregate (type and not EEntity)
	    			 return ((AEntity)getAttributeValue()).isMember((EEntity) value);
	    		 } catch (Exception e) {}
	    		 
	    		 try {
		    		 for (int i=1; i <=  ((Aggregate)getAttributeValue()).getMemberCount(); i++){
		    				 if (value == ((Aggregate)getAttributeValue()).getByIndexObject(i))
		    					 return true;
		    		 }
	    		 } catch (Exception e) {}
			}
			else return false;
		} catch (Exception e) {e.printStackTrace();}
	
		return false;
	}

	public void setIndex(int index) {
		aggIndex = index;
		
	}


}
