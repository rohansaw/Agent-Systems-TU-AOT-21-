package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;

/**
 * TODO Implement this class.
 * 
 * You might also decide to split the logic of your bidder up onto several
 * agent beans, e.g. one for each type of auction. In this case, remember
 * to keep the agent's `Wallet` in synch between the different roles, e.g.
 * using the agent's memory, as seen for the auctioneer beans.
 */
public class BidderBean extends AbstractAgentBean {

	/*
	 * TODO
	 * add properties for e.g. the multicast message group, or the bidderID
	 * add getter methods for those properties so they can be set in the
	 * Spring configuration file
	 */
	
	/*
	 * TODO
	 * when the agent starts, use the action ICommunicationBean.ACTION_JOIN_GROUP
	 * to join the multicast message group "de.dailab.jiactng.aot.auction"
	 * for the final competition, or a group of your choosing for testing
	 * make sure to use the same message group as the auctioneer!
	 */

	/*
	 * TODO
	 * when the agent starts, create a message observer and attach it to the
	 * agent's memory. that message observer should then handle the different
	 * messages and send a suitable Bid in reply. see the readme and the
	 * sequence diagram for the expected order of messages.
	 */
	
	/*
	 * TODO You will receive your initial "Wallet" from the auctioneer, but
	 * afterwards you will have to keep track of your spendings and acquisitions
	 * yourself. The Auctioneer will do so, as well.
	 */

}
