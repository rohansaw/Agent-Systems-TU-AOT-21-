package de.dailab.jiactng.aot.gridworld.messages;

/**
 * Sent from Server back to Broker to indicate whether the Order could be
 * taken. Orders may only be accepted up to a certain number of turns after
 * their creation (see property in ServerBean), otherwise a Broker could
 * wait until the last minute before accepting an Order.
 */
public class TakeOrderConfirm extends GameMessage {

	private static final long serialVersionUID = -2333739106327468731L;

	
	/** the ID of the order */
	public String orderId;
	
	/** the ID of the broker */
	public String brokerId;

	/** whether the server accepted taking the order */
	public Result state;
	
	
	@Override
	public String toString() {
		return String.format("TakeOrderConfirm(game=%d, order=%s, broker=%s, %s)", gameId, orderId, brokerId, state);
	}
	
}
