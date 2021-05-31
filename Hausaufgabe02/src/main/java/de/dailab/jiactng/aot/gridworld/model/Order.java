package de.dailab.jiactng.aot.gridworld.model;

import java.io.Serializable;

/**
 * Representation for an order, including where the bot has to go, the 
 * value of the order and the penalty for each turn that passes, the turn
 * the Order was created, and the turn by which it has to be completed.
 */
public class Order extends Element implements Serializable {

	private static final long serialVersionUID = 3087404960264087159L;

	
	/** value of this Order, will be awarded to successful Broker */
	public Integer value;

	/** turn when the Order was created or announced */
	public Integer created;
	
	/** penalty to be subtracted from value in each turn before completion */
	public Integer turnPenalty;
	
	/** turn when the Order will have failed */
	public Integer deadline;
	
	/** turn when the Order was completed, or -1 */
	public Integer completed = -1;

	
	/**
	 * Get the current reward for the order at given game turn.
	 * 
	 * - not completed? MINUS value if past deadline, otherwise 0
	 * - completed? value minus turn penalty times turns, but not less than 0
	 */
	public int getReward(int turn) {
		return completed == -1
				? turn > deadline ? - value : 0
				:  Math.max(value - (completed - created) * turnPenalty, 0);
	}
	
	@Override
	public String toString() {
		return String.format("O(%s, %s, cr=%d, dl=%d, cp=%d, val=%d, tp=%d)",
				id, position, created, deadline, completed, value, turnPenalty);
	}
}
