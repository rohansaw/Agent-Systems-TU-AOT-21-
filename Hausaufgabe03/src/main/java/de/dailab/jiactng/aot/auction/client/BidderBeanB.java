package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;


import java.util.*;
public class BidderBeanB extends AbstractBidderBean {

    int turn = 0;
    Wallet wallet;
    Auctioneer auctioneer;
    PriceList priceList;
    Account account;

    @Override
    public void doStart() throws Exception {
    }

    @Override
    public void execute() {
    }

    private synchronized void updateData() {
        Wallet w = memory.read(new Wallet(bidderId, null));
        wallet = new Wallet(w.getBidderId(), w.getCredits());
        for (Resource r : Resource.values()) {
            wallet.add(r, w.get(r));
        }

        priceList = memory.read(new PriceList(null));
        priceList = new PriceList(priceList);

        account = memory.read(new Account((Account) null));
        account = new Account(account);
    }

    public static final String ACTION_START_AUCTION = "BidderB#startAuction";
    @Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public synchronized void startAuction(StartAuction msg, ICommunicationAddress address) {
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        turn = 0;
        wallet = memory.read(new Wallet(bidderId, null));
    }

    public static final String CALL_FOR_BIDS = "BidderB#callForBids";
    @Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public synchronized void callForBids(boolean sellAll) {
        updateData();
        HashMap<Integer, Double> profit = new HashMap<>();

        for (int cid : priceList.getCallIds().keySet()) {
            if (wallet.contains(priceList.getResList(cid))) {
                double prof = priceList.getPrice(cid) - account.getCostOfBundle(priceList.getResList(cid));
                if (prof > 0 || sellAll)
                    profit.put(cid, prof);
            }
        }
        Map.Entry<Integer, Double> maxProfit = profit.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);
        if (maxProfit != null) {
            sendBid(maxProfit.getValue(), maxProfit.getKey());
        }
    }


    private void sendBid(Double bid, Integer callId) {
        Bid message = new Bid(auctioneer.getAuctioneerId(), bidderId, callId, bid);
        log.info("B sending Bid: "+ message);
        sendMessage(auctioneer.getAddress(), message);
    }

}
