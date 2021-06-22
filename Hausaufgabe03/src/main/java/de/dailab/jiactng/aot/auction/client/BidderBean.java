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
import java.util.HashMap;

public class BidderBean extends AbstractAgentBean {

	int turn = 0;
	String bidderId;
	String groupToken;
	String groupName = "someGroupName";
	String messageGroup;
	IGroupAddress groupAddress;
	HashMap<Integer, StartAuction.Mode> auctioneerModes = new HashMap<>();
	HashMap<StartAuction.Mode, Integer> auctioneerIds = new HashMap<>();
	HashMap<StartAuction.Mode, ICommunicationAddress> auctioneerAddresses = new HashMap<>();

	Wallet wallet;

	@Override
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
			if(bid != null) {
				sendBid(bid, cfb.getCallId());
			}
		}
	}

	private Double calculateBid(CallForBids cfb) {
		return null;
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

	//setters for bidder.xml

	public void setBidderId(String bidderId) {
		this.bidderId = bidderId;
	}

	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}

	public void setGroupToken(String groupToken) {
		this.groupToken = groupToken;
	}

	public class MessageObserver implements SpaceObserver<IFact> {
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
