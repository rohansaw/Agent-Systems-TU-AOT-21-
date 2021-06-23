package de.dailab.jiactng.aot.auction.client;

import com.sun.org.apache.xml.internal.security.Init;
import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.auction.onto.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BidderBeanA extends AbstractAgentBean {

	int turn = 0;
	String bidderId;
	String groupToken;
	String groupName = "someGroupName";
	IGroupAddress groupAddress;
	HashMap<Integer, StartAuction.Mode> auctioneerModes = new HashMap<>();
	HashMap<StartAuction.Mode, Integer> auctioneerIds = new HashMap<>();
	HashMap<StartAuction.Mode, ICommunicationAddress> auctioneerAddresses = new HashMap<>();

	Wallet wallet;
	private ArrayList<Resource> resourceNames = new ArrayList<>(
			Resource.A,
			Resource.B,
			Resource.C,
			Resource.D,
			Resource.E,
			Resource.F,
			Resource.G,
			Resource.J,
			Resource.K
		);
	private HashMap<Resource, Double> resourceValues;
	private HashMap<ArrayList<Resource>, Double> purchasePrices;

	public void doStart() throws Exception {
		memory.attach(new MessageObserver(), new JiacMessage());

		groupAddress = CommunicationAddressFactory.createGroupAddress(groupName);
		Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
		invoke(joinAction, new Serializable[]{groupAddress});
	}

	@Override
	public void execute() {
		turn++;
		if(wallet == null) return;
	}

	private void handleMessage(JiacMessage message) {
		Object payload = message.getPayload();
		if(payload instanceof StartAuctions) {
			register(message.getSender());
		}
		if(payload instanceof InitializeBidder) {
			initialize((InitializeBidder) payload);
		}
		if(payload instanceof StartAuction) {
			handleStartAuction((StartAuction) payload, message.getSender());
		}
		if(payload instanceof CallForBids) {
			handleCFB((CallForBids) payload);
		}

	}

	private void handleStartAuction(StartAuction message, ICommunicationAddress sender) {
		auctioneerModes.put(message.getAuctioneerId(), message.getMode());
		auctioneerIds.put(message.getMode(), message.getAuctioneerId());
		auctioneerAddresses.put(message.getMode(), sender);
		// ToDo: Maybe also save num items etc from message
	}

	private void handleCFB(CallForBids cfb) {
		if(auctioneerModes.get(cfb.getAuctioneerId()) == StartAuction.Mode.A) {
			Double bid = calculateBid(cfb);
			if(bid >= 0) {
				sendBid(bid, cfb.getCallId());
			}
		}
	}
	private void calculateResourceValues() {
		for(Resource resource : resourceNames) {
			if (resource.equals(Resource.G)) {
				resourceValues.put(resource, -20.0);
			} else {
				double value = 0;
				double count = 0;
				for (Map.Entry<ArrayList<Resource>, Double> entry : purchasePrices.entrySet()) {
					if (entry.getKey().contains(resource)) {
						double average = (1.0 / entry.getKey().size()) * entry.getValue();
						value += average;
						count++;
					}
				}
				resourceValues.put(resource, value / count);
			}
		}
	}

	private Double calculateBid(CallForBids cfb) {
		Double bid = 0.0;
		for(Resource resource: cfb.getBundle()) {
			bid += resourceValues.get(resource);
		}
		return bid;
	}

	private void sendBid(Double offer, Integer callId) {
		Bid message = new Bid(
				auctioneerIds.get(StartAuction.Mode.A),
				bidderId,
				callId,
				offer
		);
		sendMessage(auctioneerAddresses.get(StartAuction.Mode.A), message);
	}

	private void initialize(InitializeBidder message) {
		if(message.getBidderId().equals(this.bidderId)) {
			wallet = message.getWallet();
		}
	}

	private void register(ICommunicationAddress auctioneer) {
		Register message = new Register(bidderId, groupToken);
		sendMessage(auctioneer, message);
	}

	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("Bidder SENDING " + payload);
	}

	private class MessageObserver implements SpaceObserver<IFact> {
		private static final long serialVersionUID = 3252158684429257439L;

		@SuppressWarnings("rawtypes")
		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			if (event instanceof WriteCallEvent) {
				WriteCallEvent writeEvent = (WriteCallEvent) event;
				if (writeEvent.getObject() instanceof JiacMessage) {
					JiacMessage message = (JiacMessage) writeEvent.getObject();
					handleMessage(message);
					memory.remove(message);
				}
			}
		}
	}


}
