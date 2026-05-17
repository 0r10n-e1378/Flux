/*
 * Vector.java
 *
 * Basic 2D vector math utility used for movement, steering, and physics.
 */
package math;

public class Vector {

	public double x, y;
	
	public Vector (double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public void add (Vector v) {
		this.x += v.x;
		this.y += v.y;
	}
	
	public void subtract (Vector v) {
	    this.x -= v.x;
	    this.y -= v.y;
	}

	public void multiply (double n) {
	    this.x *= n;
	    this.y *= n;
	}
	
	public void divide (double n) {
	    this.x /= n;
	    this.y /= n;
	}
	
	public double magnitude () {
	    return Math.sqrt(x * x + y * y);
	}
	
	public static double distance (Vector v1, Vector v2) {
	    double dx = v1.x - v2.x;
	    double dy = v1.y - v2.y;
	    return Math.sqrt(dx * dx + dy * dy);
	}
	
	public void normalize () {
		double m = magnitude();
		if (m != 0)
			divide(m);
	}
	
	public void recalculate (double max) {
	    if (magnitude() > max) {
	        normalize();
	        multiply(max);
	    }
	}
	
}
