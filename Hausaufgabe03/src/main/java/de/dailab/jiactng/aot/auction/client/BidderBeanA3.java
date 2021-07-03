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


    public static final String ACTION_START_AUCTION = "BidderA#startAuction";
    public static final String CALL_FOR_BIDS = "BidderA#callForBids";

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

    private synchronized double calcMaxProfit(List<Resource> res){
        //greedy -> sell bundle with most value first
        List<Resource> alreadyBought = new ArrayList<>();
        Wallet wallet = memory.read(new Wallet(bidderId, null));
        Wallet w = new Wallet(wallet.getBidderId(), wallet.getCredits());
        for (Resource r : Resource.values()) {
            w.add(r, wallet.get(r));
            for(int i = 0; i < wallet.get(r); i++){
                alreadyBought.add(r);
            }
        }
        w.add(res);

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
                profit += max.getValue();
                wallet.remove(max.getKey());
                for(Resource r : max.getKey()){
                    if(alreadyBought.contains(r)){
                        alreadyBought.remove(r);
                        profit -= account.getAverageCost(r);
                    }
                }
            }
        }while (sold);
        return profit;
    }

    private synchronized void calculateBundlesToBuy(){
        //windowSize = 5 -> 32 possibilities for buy, not buy
        //maybe calculate for every CFB(C) too -> 64 possibilities
        // window = [countCFBforA, countCFBforA + windowSize[
        if(countCFBforA + windowSize > initialItems.size())
            windowSize = initialItems.size() - countCFBforA;
        int possibilities = (int)Math.pow(2, windowSize);
        ArrayList<Double> profit = new ArrayList(possibilities);
        profit.add(0.0); // buy 0 bundles

        double[] valueOfFirstBundle = new double[possibilities];

        List<Integer> maxProfitIdx = new ArrayList<>();
        double maxProfit = 0.0;

        for(int pos = 1; pos < possibilities; pos++){
            List<Resource> buyList = new LinkedList<>();
            int mask = 1;
            for(int i = countCFBforA; i < countCFBforA + windowSize; i++){
                if((pos & mask) > 0) {
                    buyList.addAll(initialItems.get(i).getBundle());
                }
                mask *= 2;
            }

            double p = calcMaxProfit(buyList);
            if(p > maxProfit){
                maxProfitIdx.clear();
                maxProfit = p;
            }
            if(maxProfit == p && (pos & 1) > 0){ // only calc if first bundle of window is in set
                maxProfitIdx.add(possibilities);
                double[] weights = GaussianElimination.weightResources(buyList, account.getProbabilities(), profit.get(pos));
                for(Resource r : initialItems.get(countCFBforA).getBundle()){
                    valueOfFirstBundle[pos] += weights[r.ordinal()];
                }
            }
            profit.add(p);
        }
        // decide on bundle set
        // a) maxProfit with fewest bundles
        // b) maxProfit with most bundles
        // c) maxProfit with most single Res
        double bid = 0.0;
        //a

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
