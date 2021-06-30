package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.IMethodExposingBean;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BidderBeanA extends AbstractBidderBean {

	int turn = 0;
	Auctioneer auctioneer;


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

	@IMethodExposingBean.Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
	public synchronized void callForBids(CallForBids cfb) {
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

	private Double calculateBid(CallForBids cfb) {
		Double standard_bid = 0.0;
		calculateResourceValues();
		for(Resource resource: cfb.getBundle()) {
			standard_bid += resourceValues.get(resource);
		}
		// Currently experimental activate, if we want to bid higher
		// return Math.max(standard_bid, calculateGreedyBid(cfb.getBundle()));
		return standard_bid;
	}

	private Double calculateGreedyBid(List<Resource> resources){
		PriceList priceList = memory.read(new PriceList(null));
		Wallet wallet = memory.read(new Wallet(null, null));
		Account account = memory.read(new Account((Account) null));
		Double bestValue = 0.0;
		for(Map.Entry<List<Resource>, Double> bundle : priceList.getPrices().entrySet()) {
			// check which resources need to be present in wallet in order to sell the new bundle directly
			List<Resource> requiredResources = bundle.getKey()
					.stream()
					.filter(element -> !resources.contains(element))
					.collect(Collectors.toList());
			if(wallet.contains(requiredResources)){
				Double payedSoFar = requiredResources.stream()
						.map(r -> {
							if(account.getAverageCost(r) > 0.0) {
								return account.getAverageCost(r);
							} else{
								return Double.MAX_VALUE;
							}
						})
						.reduce(0.0, (a, b) -> a+b);
				bestValue = Math.max(bundle.getValue() - payedSoFar, bestValue);
			}
		}
		return bestValue;
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
