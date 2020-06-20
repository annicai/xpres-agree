package util;

public class AVector {
	
	private double x;
	private double y;
	
	public AVector(double x, double y){
		this.x = x; this.y = y;
	}
	
	public double length(){
		return Math.sqrt(Math.pow(x,  2) + Math.pow(y,  2));
	}
	
	public AVector unit(){
		double length = length();
		return new AVector(x/length, y/length);
	}
	
	public double projectOnto(AVector v){
		AVector u = v.unit();
		AVector projection = new AVector(x*u.x, y*u.y);
		return projection.length();
	}
	
	public void print(){
		System.out.println("	X: " + x);
		System.out.println("	Y: " + y);
	}

}
