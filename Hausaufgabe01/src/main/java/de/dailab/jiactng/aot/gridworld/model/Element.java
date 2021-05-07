package de.dailab.jiactng.aot.gridworld.model;

import java.io.Serializable;

/**
 * Parent-class for things positioned in the Grid; mainly consists of
 * and ID and a pair of coordinate.
 */
public abstract class Element implements Serializable {

	private static final long serialVersionUID = -934856881753550872L;

	
	/** the unique Id of this element */
	public String id;
	
	/** the current position of this element */
	public Position position;
	
}
