package de.dailab.jiactng.aot.gridworld.model;

import java.io.Serializable;

/**
 * Representation for a Worker, including its remaining fuel and already taken steps.
 */
public class Worker extends Element implements Serializable {

	private static final long serialVersionUID = -5075001263858129924L;

	
	/** fuel this Worker has left (not yet relevant) */
	public Integer fuel = Integer.MAX_VALUE;
	
	/** total number of steps this Worker has taken so far */
	public Integer steps = 0;

	/** turn when this worker performed its last action */
	public Integer lastTurn = -1;
	
	
	@Override
	public String toString() {
		return String.format("W(%s, %s)", id, position);
	}
}
