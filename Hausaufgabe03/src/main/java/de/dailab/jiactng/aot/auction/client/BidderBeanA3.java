package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.IMethodExposingBean;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;

import java.util.*;
import java.util.stream.Collectors;

public class BidderBeanA3 extends AbstractBidderBean {

    private int turn = 0;
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
        turn++;
    }

    @IMethodExposingBean.Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public synchronized void startAuction(StartAuction msg, ICommunicationAddress address) {
        wallet = memory.read(new Wallet(bidderId, null));
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        turn = 0;
        initialItems = new LinkedList<>(msg.getInitialItems());
        countCFB++;
        buyList = getBundlesToBuy();
        countCFB--;
    }

    @IMethodExposingBean.Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public void callForBids(CallForBids cfb) {
        countCFB++;
        updateData();
        Double bid = calculateBid();
        if (bid >= cfb.getMinOffer()) {
            sendBid(bid, cfb.getCallId());
        }
        if(countCFB % windowSize == windowSize - 1) {
            countCFB++;
            buyList = getBundlesToBuy();
            countCFB--;
        }
    }

    @IMethodExposingBean.Expose(name = UPDATE_BUY_LIST, scope = ActionScope.AGENT)
    public synchronized void updateBuyList() {
        buyList = getBundlesToBuy();
    }

    private double calculateBid(){
        double bid = 0.0;
        if((buyList & 1) > 0) {
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
            for (Resource r : initialItems.get(countCFB).getBundle()){
                bid += weights[r.ordinal()];
            }
        }
        return bid;
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


    private synchronized double calcMaxProfit(List<Resource> res) {
        //greedy -> sell bundle with most value first
        List<Resource> alreadyBought = new ArrayList<>();
        Wallet wallet = memory.read(new Wallet(bidderId, null));
        Wallet w = new Wallet(wallet.getBidderId(), wallet.getCredits());
        for (Resource r : Resource.values()) {
            w.add(r, wallet.get(r));
            for (int i = 0; i < wallet.get(r); i++) {
                alreadyBought.add(r);
            }
        }
        w.add(res);

        boolean sold;
        double profit = 0;

        do {
            sold = false;
            Map.Entry<List<Resource>, Double> max = priceList.getPrices().entrySet()
                    .stream()
                    .max(Comparator.comparing(Map.Entry::getValue))
                    .orElse(null);
            if (max != null) {
                sold = true;
                profit += max.getValue();
                wallet.remove(max.getKey());
                for (Resource r : max.getKey()) {
                    if (alreadyBought.contains(r)) {
                        alreadyBought.remove(r);
                        profit -= account.getAverageCost(r);
                    }
                }
            }
        } while (sold);
        return profit;
    }

    private int getBundlesToBuy() {
        //windowSize = 5 -> 32 possibilities for buy, not buy
        //maybe calculate for every CFB(C) too -> 64 possibilities
        // window = [countCFB, countCFB + windowEnd[
        int windowEnd = countCFB + windowSize - (countCFB % windowSize);
        if (windowEnd > initialItems.size())
            windowEnd = initialItems.size() - countCFB;

        int possibilities = (int) Math.pow(2, windowEnd - countCFB);
        ArrayList<Double> profit = new ArrayList(possibilities);
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
            profit.add(p);
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
