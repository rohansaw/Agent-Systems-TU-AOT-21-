package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.IMethodExposingBean;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;

import java.util.*;
import java.util.stream.Collectors;

public class BidderBeanA3 extends AbstractBidderBean {

    private Wallet wallet;
    private Auctioneer auctioneer;
    private PriceList priceList;
    private Account account;
    private int countCFB = -1;

    private int buyList = 0;


    public static final String ACTION_START_AUCTION = "BidderA#startAuction";
    public static final String CALL_FOR_BIDS = "BidderA#callForBids";
    public static final String UPDATE_BUY_LIST = "BidderA#updateBuyList";

    private List<Item> initialItems = new LinkedList<>();


    @Override
    public void doStart() throws Exception {
    }


    @Override
    public void execute() {
        return;
    }

    @IMethodExposingBean.Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public void startAuction(StartAuction msg, ICommunicationAddress address) {
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        initialItems = new LinkedList<>(msg.getInitialItems());
    }

    @IMethodExposingBean.Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public void callForBids(CallForBids cfb) {
        countCFB++;
        updateData();
        Double bid = calculateBid();
        if (bid >= cfb.getMinOffer()) {
            sendBid(bid, cfb.getCallId());
        }
        if (countCFB % windowSize == windowSize - 1) {
            countCFB++;
            buyList = getBundlesToBuy();
            countCFB--;
        }
    }

    @IMethodExposingBean.Expose(name = UPDATE_BUY_LIST, scope = ActionScope.AGENT)
    public void updateBuyList() {
        countCFB++;
        buyList = getBundlesToBuy();
        countCFB--;
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

    private double calculateBid() {
        updateData();
        double bid = 0.0;
        if ((buyList & 1) > 0) {
            int windowEnd = countCFB + windowSize - (countCFB % windowSize);
            if (windowEnd > initialItems.size())
                windowEnd = initialItems.size() - countCFB;
            List<Resource> resList = new LinkedList<>();
            int mask = 1;
            for (int i = countCFB; i < windowEnd; i++) {
                if ((buyList & mask) > 0) {
                    resList.addAll(initialItems.get(i).getBundle());
                }
                mask *= 2;
            }

            double profit = calcMaxProfit(resList);
            double[] weights = GaussianElimination.weightResources(resList, account.getProbabilities(), profit);
            for (Resource r : initialItems.get(countCFB).getBundle()) {
                bid += weights[r.ordinal()];
            }
        }
        buyList >>= 1;
        return bid;
    }

    private double calcMaxProfit(List<Resource> res) {
        //greedy -> sell bundle with most value first
        updateData();
        List<Resource> alreadyBought = new ArrayList<>();
        for(Resource r : Resource.values()) {
            for (int i = 0; i < wallet.get(r); i++) {
                alreadyBought.add(r);
            }
        }

        wallet.add(res);

        boolean sold;
        double profit = -20 * res.size();
        do {
            sold = false;
            Map.Entry<Integer, Double> max = priceList.getPurchasePrices().entrySet()
                    .stream()
                    .filter(e -> wallet.contains(priceList.getResList(e.getKey())))
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (max != null) {
                sold = true;
                profit += max.getValue();
                wallet.remove(priceList.getResList(max.getKey()));
                priceList.setPrice(max.getValue() - 5, max.getKey());
                for (Resource r : priceList.getResList(max.getKey())) {
                    if (alreadyBought.contains(r)) {
                        alreadyBought.remove(r);
                        profit -= account.getAverageCost(r) + 20;
                    }
                }
            }
        } while (sold);
        return profit;
    }

    private int getBundlesToBuy() {
        // 2^windowSize possibilities for buy, not buy
        // window = [countCFB, countCFB + windowEnd[
        int windowEnd = countCFB + windowSize - (countCFB % windowSize);
        if (windowEnd > initialItems.size())
            windowEnd = initialItems.size() - countCFB;

        int possibilities = (int) Math.pow(2, windowEnd - countCFB);
        int[] resCount = new int[possibilities];
        List<Integer> maxProfitIdx = new ArrayList<>();
        double maxProfit = 0.0;

        for (int pos = possibilities - 1; pos >= 0; pos--) {
            List<Resource> resList = new LinkedList<>();
            int mask = 1;
            for (int i = countCFB; i < windowEnd; i++) {
                if ((pos & mask) > 0) {
                    resList.addAll(initialItems.get(i).getBundle());
                }
                mask *= 2;
            }

            double p = calcMaxProfit(resList);
            resCount[pos] = resList.size();

            if (p > maxProfit) {
                maxProfitIdx.clear();
                maxProfit = p;
            }
            if (maxProfit == p) { // only calc if first bundle of window is in set
                maxProfitIdx.add(pos);
            }
        }
        // decide on bundle set
        // maxProfit with fewest bundles and most res
        int minBundles = windowSize;
        int maxRes = 0;
        int bundleIdx = 0;
        for (int i : maxProfitIdx) {
            int bitCount = Integer.bitCount(i);
            if (bitCount <= minBundles) {
                minBundles = bitCount;
                if (resCount[i] > maxRes) {
                    maxRes = resCount[i];
                    bundleIdx = i;
                }
            }
        }
        return bundleIdx;
    }

    private void sendBid(Double offer, Integer callId) {
        Bid message = new Bid(
                auctioneer.getAuctioneerId(),
                bidderId,
                callId,
                offer
        );
        log.info("A sending BID: " + message);
        sendMessage(auctioneer.getAddress(), message);
    }
}
