package de.dailab.jiactng.aot.auction.client;

import com.sun.org.apache.xml.internal.security.Init;
import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.action.IMethodExposingBean;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
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
	String messageGroup;
	IGroupAddress groupAddress;
	Auctioneer auctioneer;


	public static final String ACTION_START_AUCTION = "BidderA#startAuction";
	public static final String CALL_FOR_BIDS = "BidderA#callForBids";

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
	Wallet wallet;


	@Override
	public void doStart() throws Exception {
		groupAddress = CommunicationAddressFactory.createGroupAddress(groupName);
		Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
		invoke(joinAction, new Serializable[]{groupAddress});
	}

	@Override
	public void execute() {
		turn++;
		if(wallet == null) return;
	}

	@IMethodExposingBean.Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
	public void callForBids(CallForBids cfb) {
		if(cfb.getMode() == CallForBids.CfBMode.BUY) {
			Double bid = calculateBid(cfb);
			if(bid > 0) {
				sendBid(bid, cfb.getCallId());
			}
		}
	}

	@IMethodExposingBean.Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
	public synchronized void startAuction(StartAuction msg, ICommunicationAddress address) {
		wallet = memory.read(new Wallet(bidderId, null));
		auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
		turn = 0;
	}

	private void calculateResourceValues() {
		for(Resource resource : resourceNames) {
			if (resource.equals(Resource.G)) {
				resourceValues.put(resource, -20.0);
			} else {
				double value = 0;
				double count = 0;
				PriceList priceList = memory.read(new PriceList());
				for (Map.Entry<ArrayList<Resource>, Double> entry : priceList.getPrices().entrySet()) {
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
		calculateResourceValues();
		for(Resource resource: cfb.getBundle()) {
			bid += resourceValues.get(resource);
		}
		return bid;
	}

	private void sendBid(Double offer, Integer callId) {
		Bid message = new Bid(
				auctioneer.getAuctioneerId(),
				bidderId,
				callId,
				offer
		);
		sendMessage(auctioneer.getAddress(), message);
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
}
