package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Sent from Broker back to Server if the Broker wants to execute an Order.
 */
public class TakeOrderMessage extends GameMessage {

	private static final long serialVersionUID = -3920274569654016426L;


	/** the ID of the order */
	public String orderId;
	
	/** the ID of the broker */
	public String brokerId;

	
	@Override
	public String toString() {
		return String.format("TakeOrderMessage(game=%d, order=%s, broker=%s)", gameId, orderId, brokerId);
	}
	
}
