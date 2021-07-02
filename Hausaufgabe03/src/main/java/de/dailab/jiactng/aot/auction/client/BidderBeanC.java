package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;


import java.util.*;

public class BidderBeanC extends AbstractBidderBean {

    int turn = -1;

    Wallet wallet = null;
    Auctioneer auctioneer = null;
    PriceList priceList = null;
    Account account = null;

    //tune parameter \in (0,1) -> optimum depends on #bidders
    int countBidder = 2;


    @Override
    public void doStart() throws Exception {
    }

    @Override
    public void execute() {
        if(turn >= 0) {
            sellItem();
            turn++;
        }
        //only one offer per bidder per round
        //decide on offering single Res
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

    private synchronized void sellItem(){
        //updateData();

    }

    public static final String ACTION_START_AUCTION = "BidderC#startAuction";
    @Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public synchronized void startAuction(StartAuction msg, ICommunicationAddress address, int countBidders) {
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        turn = 0;
        countBidder = countBidders;
        if(countBidder < 2) countBidder = 2;
    }

    public static final String CALL_FOR_BIDS = "BidderC#callForBids";
    @Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public void callForBids(CallForBids msg) {
        updateData();

        if(!msg.getOfferingBidder().equals(bidderId) && wallet.getCredits() >= msg.getMinOffer()) {
            double bid = getBid(msg) * ((countBidder - 1) / countBidder);
            if(bid > msg.getMinOffer())
                sendMessage(auctioneer.getAddress(), new Bid(msg.getAuctioneerId(), bidderId, msg.getCallId(), bid));
        }
    }

    private synchronized double getBid(CallForBids msg){
        double profitWithoutBuy = calcMaxProfit(null, 0);
        double profitWithBuy = calcMaxProfit(msg.getBundle(), msg.getMinOffer());
        return profitWithoutBuy - profitWithBuy;
    }

    private synchronized double calcMaxProfit(List<Resource> res, double prize){
        //greedy -> sell bundle with most value first
        Wallet wallet = memory.read(new Wallet(bidderId, null));
        Wallet w = new Wallet(wallet.getBidderId(), wallet.getCredits());
        for (Resource r : Resource.values()) {
            w.add(r, wallet.get(r));
        }
        if(res != null)
            w.add(res);
        if(prize != 0) // find better estimator for prize
            account.addItem(res, prize);

        boolean sold;
        double profit = 0;

        do{
            sold = false;
            Map.Entry<List<Resource>, Double> max = priceList.getPrices().entrySet()
                    .stream()
                    .max(Comparator.comparing(Map.Entry::getValue))
                    .orElse(null);
            if(max != null){
                sold = true;
                profit += max.getValue() - account.getCostOfBundle(max.getKey());
                wallet.remove(max.getKey());
            }
        }while (sold);
        return profit;
    }

    /*

    public synchronized double getBid(CallForBids msg) {
        Set<Integer> sellWithoutBuy = new HashSet<>();
        HashMap<Integer, Double> profit = new HashMap<>();
        double bid = account.getCostOfBundle(msg.getBundle());

        for (int cid : priceList.getCallIds().keySet()) {
            if (wallet.contains(priceList.getResList(cid))) {
                sellWithoutBuy.add(cid);
            }
        }

        wallet.add(msg.getBundle());
        account.addItem(msg.getBundle(), msg.getMinOffer());

        while (true) {
            for (int cid : priceList.getCallIds().keySet()) {
                if (wallet.contains(priceList.getResList(cid)) && !sellWithoutBuy.contains(cid)) {
                    //no subtraction of msg.price -> it's already calculated in account.addItem
                    double prof = priceList.getPrice(cid) - account.getCostOfBundle(priceList.getResList(cid));
                    if (prof > 0)
                        profit.replace(cid, prof);
                }
            }

            if (profit.size() == 0) break;

            Map.Entry<Integer, Double> maxProfit = profit.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElse(null);

            if (maxProfit != null) {
                //get MaxProfit after sell maxProfit
                List<Resource> res = priceList.getResList(maxProfit.getKey());
                bid += maxProfit.getValue();
                wallet.remove(res);
                account.removeItem(res, maxProfit.getValue());
                profit.remove(maxProfit.getKey());
            }
        }

        return bid;
    }
     */
}
