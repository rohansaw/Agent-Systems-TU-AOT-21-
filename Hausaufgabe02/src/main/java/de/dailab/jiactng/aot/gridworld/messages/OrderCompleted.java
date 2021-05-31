package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Sent from Server to Broker that previously took an order if that Order is completed.
 * A completed order can either be successfully completed, or failed.
 */
public class OrderCompleted extends GameMessage {

	private static final long serialVersionUID = -2322171511045441308L;

	
	/** the ID of the order */
	public String orderId;
	
	/** whether the order has been completed or failed */
	public Result state;
	
	/** the reward awarded for the completed order */
	public Integer reward;

	
	@Override
	public String toString() {
		return String.format("OrderCompleted(game=%d, order=%s, %s, reward=%d)", gameId, orderId, state, reward);
	}
}
