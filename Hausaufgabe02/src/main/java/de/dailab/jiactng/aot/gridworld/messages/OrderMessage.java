package de.dailab.jiactng.aot.gridworld.messages;

import de.dailab.jiactng.aot.gridworld.model.Order;

/**
 * Sent from Server to Broker to announce a new Order, which is the main payload.
 */
public class OrderMessage extends GameMessage {

	private static final long serialVersionUID = 997518886924486005L;

	
	/** the order to be performed */
	public Order order;

	
	@Override
	public String toString() {
		return String.format("OrderMessage(game=%d, order=%s)", gameId, order);
	}
}
