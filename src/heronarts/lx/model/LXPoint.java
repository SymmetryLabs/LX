/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 * All Rights Reserved
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.lx.model;

/**
 * A point is a node with an immutable position in space and a location in  
 */
public class LXPoint {
	
	static int counter = 0;
	
	/**
	 * x coordinate of this point
	 */
	public final float x;
	
	/**
	 * y coordinate of this point
	 */
	public final float y;
	
	/**
	 * z coordinate of this point
	 */
	public final float z;
	
	/**
	 * Index of this point in the colors array
	 */
	public final int index;
	
	/**
	 * Construct a point in 2-d space, z-val is 0
	 * 
	 * @param x
	 * @param y
	 */
	public LXPoint(float x, float y) {
		this(x, y, 0);
	}
	
	/**
	 * Construct a point in 3-d space
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public LXPoint(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.index = counter++;
	}

}
