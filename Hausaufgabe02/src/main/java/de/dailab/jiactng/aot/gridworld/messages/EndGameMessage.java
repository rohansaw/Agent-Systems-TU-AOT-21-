package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Message sent to Brokers when the game is over.
 */
public class EndGameMessage extends GameMessage {
	
	private static final long serialVersionUID = -4357871144383933999L;

	
	/** the broker this message is intended for (not really necessary) */
	public String brokerId;
	
	/** whether the receiver of this message is the winner (only relevant for multiplayer) */
	public Boolean winner;
	
	/** total reward of the Broker this message is sent to */
	public Double totalReward;

	
	@Override
	public String toString() {
		return String.format("EndGameMessage(game=%d, broker=%s, winner?=%s, reward=%.2f)", gameId, brokerId, winner, totalReward);
	}
}
