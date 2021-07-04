package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;


import java.util.*;
import java.util.stream.Collectors;

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
        if (turn >= 0) {
            sellItem();
            turn++;
        }
        //only one offer per bidder per round
        //decide on offering single Res
    }

    private synchronized void updateData() {
        lock.readLock().lock();
        try {
            Wallet w = memory.read(new Wallet(bidderId, null));
            wallet = new Wallet(w.getBidderId(), w.getCredits());
            for (Resource r : Resource.values()) {
                wallet.add(r, w.get(r));
            }

            priceList = memory.read(new PriceList((PriceList) null));
            priceList = new PriceList(priceList);

            account = memory.read(new Account((Account) null));
            account = new Account(account);
        }finally {
            lock.readLock().unlock();
        }
    }

    private void sellItem() {
        updateData();
        if (wallet.get(Resource.G) > 0) {
            List<Resource> bundle = new LinkedList<>();
            bundle.add(Resource.G);
            Offer offer = new Offer(auctioneer.getAuctioneerId(), bidderId, bundle, 20.0);
            sendMessage(auctioneer.getAddress(), offer);
        }
    }

    public static final String ACTION_START_AUCTION = "BidderC#startAuction";

    @Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public void startAuction(StartAuction msg, ICommunicationAddress address, int countBidders) {
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        turn = 0;
        countBidder = countBidders;
        if (countBidder < 2) countBidder = 2;
    }

    public static final String CALL_FOR_BIDS = "BidderC#callForBids";

    @Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public void callForBids(CallForBids msg) {
        updateData();
        if (!msg.getOfferingBidder().equals(bidderId) && wallet.getCredits() >= msg.getMinOffer()) {
            double bid = getBid(msg) * ((countBidder - 1) / countBidder);
            if (bid >= msg.getMinOffer())
                sendMessage(auctioneer.getAddress(), new Bid(msg.getAuctioneerId(), bidderId, msg.getCallId(), bid));
        }
    }

    private double getBid(CallForBids msg) {
        double profitWithoutBuy = calcMaxProfit(null, 0);
        double profitWithBuy = calcMaxProfit(msg.getBundle(), msg.getMinOffer());
        return profitWithoutBuy - profitWithBuy;
    }

    private double calcMaxProfit(List<Resource> res, double prize) {
        updateData();
        Wallet wallet = memory.read(new Wallet(bidderId, null));
        Wallet w = new Wallet(wallet.getBidderId(), wallet.getCredits());
        for (Resource r : Resource.values()) {
            w.add(r, wallet.get(r));
        }
        if (res != null)
            w.add(res);
        if (prize != 0) // find better estimator for prize
            account.addItem(res, prize);

        double profit = 0;
        while (true) {
            List<Map.Entry<Integer, List<Resource>>> buy = priceList.getBundles().entrySet()
                    .stream()
                    .filter(e -> wallet.contains(e.getValue())).collect(Collectors.toList());
            if (buy.size() == 0)
                break;
            List<ResProfitPair> profitList = new ArrayList<>(buy.size());
            for (Map.Entry<Integer, List<Resource>> elem : buy) {
                profitList.add(new ResProfitPair(elem.getValue(), priceList.getPrice(elem.getKey()) - account.getCostOfBundle(elem.getValue()), elem.getKey()));
            }
            ResProfitPair max = profitList.stream()
                    .max(Comparator.comparing(ResProfitPair::getProfit))
                    .orElse(null);
            if (max != null) {
                profit += max.getProfit();
                wallet.remove(max.getRes());
                account.removeItem(max.getRes(), priceList.getPrice(max.getCid()));
                priceList.setPrice(priceList.getPrice(max.getCid()) - 5, max.getCid());
            } else {
                break;
            }
        }
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
