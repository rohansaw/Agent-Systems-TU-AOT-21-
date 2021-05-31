package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Message sent to Brokers when the game is over.
 */
public class WorkerScoreMessage extends GameMessage {
	
	private static final long serialVersionUID = 9056149156638952482L;

	
	/** the worker this message is about */
	public String workerId;
	
	/** whether the worker of this message is the winner */
	public Boolean winner;
	
	/** total reward of the worker this message is about */
	public Integer totalReward;

	
	@Override
	public String toString() {
		return String.format("WorkerScoreMessage(game=%d, worker=%s, winner?=%s, orders completed=%d)", gameId, workerId, winner, totalReward);
	}
}
