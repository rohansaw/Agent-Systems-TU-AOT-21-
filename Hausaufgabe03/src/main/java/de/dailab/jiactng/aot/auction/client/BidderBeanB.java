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
        turn++;
        if (wallet == null) return;
    }

    private synchronized void updateWallet() {
        Wallet w = new Wallet(wallet.getBidderId(), wallet.getCredits());

        wallet = memory.read(new Wallet(bidderId, null));
        for (Resource r : Resource.values()) {
            w.add(r, wallet.get(r));
        }
        wallet = w;
    }

    private synchronized void updatePriceList() {
        priceList = memory.read(new PriceList(null));
        priceList = new PriceList(priceList.getPrices());
    }

    private synchronized void updateAccount() {
        account = memory.read(new Account((Wallet) null));
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
    public synchronized void callForBids(CallForBids msg) {
        updateAccount();
        updateWallet();
        updatePriceList();

        HashMap<List<Resource>, Double> profit = new HashMap<>();
        for (List<Resource> l : priceList.getPrices().keySet()) {
            if (wallet.contains(l)) {
                double prof = priceList.getPrice(l) - account.getCostOfBundle(l);
                if (prof > 0)
                    profit.put(l, prof);
            }
        }
        Map.Entry<List<Resource>, Double> maxProfit = profit.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);
        if (maxProfit != null) {
            sendBid(maxProfit.getValue(), priceList.getCallId(maxProfit.getKey()));
            account.removeItem(maxProfit.getKey(), priceList.getPrice(maxProfit.getKey()));
            wallet.remove(maxProfit.getKey());
            wallet.updateCredits(priceList.getPrice(maxProfit.getKey()));
            priceList.setPrice(maxProfit.getKey(), 0.0, 0);
        }
    }


    private void sendBid(Double bid, Integer callId) {
        Bid message = new Bid(auctioneer.getAuctioneerId(), bidderId, callId, bid);
        sendMessage(auctioneer.getAddress(), message);
    }

}
