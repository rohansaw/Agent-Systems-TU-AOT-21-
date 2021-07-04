package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.IMethodExposingBean;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BidderBeanA2 extends AbstractBidderBean {

    int turn = 0;
    Auctioneer auctioneer;


    public static final String ACTION_START_AUCTION = "BidderA#startAuction";
    public static final String CALL_FOR_BIDS = "BidderA#callForBids";

    private HashMap<Resource, Double> resourceValues;
    private HashMap<Resource, Double> resourceProbs;
    Wallet wallet;


    @Override
    public void doStart() throws Exception {
        resourceValues = new HashMap<>();
        resourceProbs.put(Resource.A, 4.0);
        resourceProbs.put(Resource.B, 4.0);
        resourceProbs.put(Resource.C, 2.0);
        resourceProbs.put(Resource.D, 2.0);
        resourceProbs.put(Resource.E, 1.0);
        resourceProbs.put(Resource.F, 1.0);
        resourceProbs.put(Resource.G, 1.0);
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
        PriceList priceList = memory.read(new PriceList((PriceList) null));
        for(Resource resource : Resource.values()) {
            if (resource.equals(Resource.G)) {
                resourceValues.put(resource, -20.0);
            } else {
                double value = 0;
                double count = 0;
                for (Map.Entry<List<Resource>, Double> entry : priceList.getPrices().entrySet()) {
                    if (entry.getKey().contains(resource)) {
                        // Price will be adjusted a little bit below or above our estimate,
                        // depending on the probability of the item
                        double probability = resourceProbs.get(entry.getKey());
                        double normalizedProb = probability - 2;
                        double basePrice = (1.0 / entry.getKey().size()) * entry.getValue();
                        double weightedPrice = basePrice + 0.1 * normalizedProb * basePrice;
                        value += weightedPrice;
                        count++;
                    }
                }
                resourceValues.put(resource, value / count);
            }
        }
    }

    private Double calculateBid(CallForBids cfb) {
        Double weighted_bid = 0.0;
        calculateResourceValues();
        for(Resource resource: cfb.getBundle()) {
            weighted_bid += resourceValues.get(resource);
        }
        return weighted_bid;
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
