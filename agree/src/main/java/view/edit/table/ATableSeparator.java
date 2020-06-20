package view.edit.table;


public class ATableSeparator extends ATableItem{
	
	private boolean existing = true;
	
	public ATableSeparator(int idx) {
		index = idx;
	}
	
	public void setExisting(boolean b){
		existing = b;
	}
	
	public String toString(){
		if (existing)
			return "~~~~~~~~~~~~~~~~~~ ".concat(Integer.toString(index)).concat(" ~~~~~~~~~~~~~~~~~~");
		else return "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~";
	};

}
