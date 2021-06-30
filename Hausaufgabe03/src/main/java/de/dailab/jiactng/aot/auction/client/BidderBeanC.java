package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;


import java.util.*;

public class BidderBeanC extends AbstractBidderBean {

    int turn = 0;

    Wallet wallet = null;
    Auctioneer auctioneer = null;
    PriceList priceList = null;
    Account account = null;

    //tune parameter \in (0,1) -> optimum depends on #bidders
    double valueFraction = 0.75;


    @Override
    public void doStart() throws Exception {
    }

    @Override
    public void execute() {
        turn++;
        if(wallet == null) return;

        sellItem();
        //only one offer per bidder per round
        //decide on offering single Res
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

    private void sellItem(){
        updateData();

    }

    public static final String ACTION_START_AUCTION = "BidderC#startAuction";
    @Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public synchronized void startAuction(StartAuction msg, ICommunicationAddress address) {
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        turn = 0;
        wallet = memory.read(new Wallet(bidderId, null));
    }

    public static final String CALL_FOR_BIDS = "BidderC#callForBids";
    @Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public synchronized void callForBids(CallForBids msg) {
        updateData();

        if(!msg.getOfferingBidder().equals(bidderId) && wallet.getCredits() >= msg.getMinOffer()) {
            double bid = getBid(msg) * valueFraction;
            if(bid > 0)
                sendMessage(auctioneer.getAddress(), new Bid(msg.getAuctioneerId(), bidderId, msg.getCallId(), bid));
        }
    }

    public synchronized double getBid(CallForBids msg) {
        Set<List<Resource>> sellWithoutBuy = new HashSet<>();
        HashMap<List<Resource>, Double> profit = new HashMap<>();
        double bid = 0;

        for (List<Resource> l : priceList.getPrices().keySet()) {
            if (wallet.contains(l)) {
                sellWithoutBuy.add(l);
            }
        }

        wallet.add(msg.getBundle());
        account.addItem(msg.getBundle(), msg.getMinOffer());

        while (true) {
            for (List<Resource> l : priceList.getPrices().keySet()) {
                if (wallet.contains(l) && !sellWithoutBuy.contains(l)) {
                    //no subtraction of msg.price -> it's already calculated in account.addItem
                    double prof = priceList.getPrice(l) - account.getCostOfBundle(l);
                    if (prof > 0)
                        profit.put(l, prof);
                }
            }

            if (profit.size() == 0) break;

            Map.Entry<List<Resource>, Double> maxProfit = profit.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElse(null);

            if (maxProfit != null) {
                //get MaxProfit after sell maxProfit
                bid += maxProfit.getValue();
                wallet.remove(maxProfit.getKey());
                account.removeItem(maxProfit.getKey(), maxProfit.getValue());
                profit.remove(maxProfit.getKey());
            }
        }
        return bid;
    }
}
