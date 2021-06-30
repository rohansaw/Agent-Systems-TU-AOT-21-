package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.IMethodExposingBean;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;

import java.util.*;

public class BidderBeanA extends AbstractBidderBean {

	int turn = 0;
	Auctioneer auctioneer;

	PriceList priceList = null;
	Account account = null;


	public static final String ACTION_START_AUCTION = "BidderA#startAuction";
	public static final String CALL_FOR_BIDS = "BidderA#callForBids";

	private HashMap<Resource, Double> resourceValues;
	Wallet wallet;


	@Override
	public void doStart() throws Exception {
		resourceValues = new HashMap<>();
	}


	@Override
	public void execute() {
		turn++;
	}

	private synchronized void updateData() {
		Wallet w = new Wallet(wallet.getBidderId(), wallet.getCredits());
		wallet = memory.read(new Wallet(bidderId, null));
		for (Resource r : Resource.values()) {
			w.add(r, wallet.get(r));
		}

		priceList = memory.read(new PriceList(null));
		priceList = new PriceList(priceList.getPrices());

		account = memory.read(new Account((Wallet) null));
		account = new Account(account);
	}

	@IMethodExposingBean.Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
	public synchronized void callForBids(CallForBids cfb) {
		if(cfb.getMode() == CallForBids.CfBMode.BUY) {
			updateData();
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

	private synchronized void calculateResourceValues() {
		PriceList priceList = memory.read(new PriceList(null));
		for(Resource resource : Resource.values()) {
			if (resource.equals(Resource.G)) {
				resourceValues.put(resource, -20.0);
			} else {
				double value = 0;
				double count = 0;
				for (Map.Entry<List<Resource>, Double> entry : priceList.getPrices().entrySet()) {
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

	private synchronized Double calculateBid(CallForBids cfb) {
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
		log.info("A sending BID: "+ message);
		sendMessage(auctioneer.getAddress(), message);
	}
}
