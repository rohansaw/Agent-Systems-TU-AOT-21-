package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.auction.onto.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.*;

public class ProxyBeanFixed extends AbstractBidderBean {

    IGroupAddress groupAddress;
    HashMap<Integer, Auctioneer> auctioneers;
    Wallet wallet;
    Account account;
    int countCFBforB = 0;
    int plSize = 0;
    PriceList priceList = new PriceList((PriceList) null);
    List<Bid> offeredItems = new LinkedList<>();
    HashMap<Resource, Integer> reservedResources = new HashMap<>();
    List<Item> initialItems = new LinkedList<>();
    boolean isFixed = false;
    HashMap<List<Resource>, Double> bundlesToBuy = new HashMap<>();
    ArrayList<Resource> resourcesToSell = new ArrayList<>();
    ArrayList<List<Resource>> bundlesToSell = new ArrayList<>();

    private int countOnlyCFBforB = 0;
    private int countBidder = 0;

    //only register for one auction
    private boolean ready = true;

    private int countCFBforA = -1;

    private int windowSize = 5;

    @Override
    public void doStart() throws Exception {
        memory.attach(new MessageObserver(), new JiacMessage());

        groupAddress = CommunicationAddressFactory.createGroupAddress(messageGroup);
        Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke(joinAction, new Serializable[]{groupAddress});
    }

    @Override
    public synchronized void execute(){
        countOnlyCFBforB++;
    }

    private void handleMessage(JiacMessage message){
        Object payload = message.getPayload();
        log.info("--------------");
        if(wallet != null){
            log.info(wallet.toString());
        }
        log.info("Bidder RECEIVED:");
        log.info(payload);

        if(payload instanceof StartAuctions) {
            register(message.getSender());
        }

        if(payload instanceof InitializeBidder) {
            initialize((InitializeBidder) payload);
        }

        if(payload instanceof StartAuction) {
            handleStartAuction((StartAuction) payload, message.getSender());
        }
        if(payload instanceof CallForBids) {
            handleCallForBids((CallForBids) payload);
        }
        if(payload instanceof InformBuy) {
            handleInformBuy((InformBuy) payload);
        }
        if(payload instanceof InformSell) {
            handleInformSell((InformSell) payload);
        }
        if(payload instanceof EndAuction){
            ready = true;
        }
    }

    private synchronized void register(ICommunicationAddress auctioneer) {
        if(ready) {
            ready = false;
            auctioneers = new HashMap<>();
            memory.removeAll(new Wallet(null, null));
            memory.removeAll(new Auctioneer(null, null, null));
            memory.removeAll(new PriceList((PriceList) null));
            // memory.removeAll(new PriceList(null));
            Register message = new Register(bidderId, groupToken);
            sendMessage(auctioneer, message);
        }
    }

    private synchronized void initialize(InitializeBidder msg) {
        wallet = msg.getWallet();
        if(wallet != null) {
            countBidder = getBidderCount();
            account = new Account(wallet, countBidder);
            priceList = new PriceList((PriceList) null);
            memory.write(priceList);
            memory.write(wallet);
            memory.write(account);
        } else {
            log.warn("Wallet was null ?!");
        }
    }

    private void handleStartAuction(StartAuction msg, ICommunicationAddress sender) {
        auctioneers.put(msg.getAuctioneerId(), new Auctioneer(msg.getAuctioneerId(), sender, msg.getMode()));
        countOnlyCFBforB = 0;
        if(msg.getMode() == StartAuction.Mode.A)
            account.setProbabilities(initialItems);
        if(msg.getMode() == StartAuction.Mode.A && msg.getInitialItems() != null) {
            isFixed = true;
            initialItems = new LinkedList<>(msg.getInitialItems());
            calculateBundlesToBuy();
        }
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

    private void handleCallForBids(CallForBids msg){
        Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
        switch (auctioneer.getMode()){
            case A:
                countCFBforA++;
                handleCFB_A(msg);
                break;
            case B:
                countCFBforB++;
                priceList.setPrice(msg.getBundle(), msg.getMinOffer(), msg.getCallId());
                if(countCFBforB == plSize) {
                    invokeSimple(BidderBeanB.CALL_FOR_BIDS, countOnlyCFBforB >= 2);
                    countCFBforB = 0;
                }
                break;
            case C:
                handleCFB_C(msg);
        }
    }

    private void handleCFB_A(CallForBids msg) {
        if(initialItems.contains(msg.getBundle())) {
            initialItems.remove(msg.getBundle());
            if(bundlesToBuy.containsKey(msg.getBundle())) {
                Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
                Double bid = bundlesToBuy.get(msg.getBundle());
                sendBid(bid, msg.getCallId(), auctioneer);
                bundlesToBuy.remove(msg.getBundle());
            }
        }
    }

    private void handleCFB_B(CallForBids msg) {
        if(wallet.contains(msg.getBundle()) && bundlesToSell.contains(msg.getBundle())) {
            Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
            sendBid(msg.getMinOffer(), msg.getCallId(), auctioneer);
            // ToDo: maybe mark these resources as reserved, so that we dont use resources in mutliple sells and sell stuff we dont have
        }
    }

    private Double calculateBidFor(Resource resource) {
        // ToDo: Use soma average value maybe to calcualte this?

        return 100.0;
    }


    private void handleCFB_C(CallForBids msg) {
        // ToDo: better would be to use a "bestResource" that should be sold. This would be the resource that
        // we have the most in our wallet and that is also in the resourcesToSell list
        for(Resource resource : resourcesToSell) {
            List list = new ArrayList();
            list.add(resource);
            if(wallet.contains(list)) {
                Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
                Double bid = calculateBidFor(resource);
                sendBid(bid, msg.getCallId(), auctioneer);
                break;
            }
        }
    }

    private synchronized void handleInformBuy(InformBuy msg) {
        if (msg.getBundle() != null && msg.getType() == InformBuy.BuyType.WON) {
            wallet.add(msg.getBundle());
            wallet.updateCredits(-msg.getPrice());
            account.addItem(msg.getBundle(), msg.getPrice());
        } else if(msg.getBundle() != null && msg.getType() == InformBuy.BuyType.LOST) {
            // Our strategy did not work out, so we need to recalculate
            calculateBundlesToBuy();
        }
    }

    private synchronized void handleInformSell(InformSell msg) {
        if (msg.getBundle() != null && msg.getType() == InformSell.SellType.SOLD) {
            wallet.remove(msg.getBundle());
            wallet.updateCredits(msg.getPrice());
            account.removeItem(msg.getBundle(), msg.getPrice());
        }
    }

    private void sendBid(Double bid, Integer callId, Auctioneer auctioneer) {
        Bid message = new Bid(auctioneer.getAuctioneerId(), bidderId, callId, bid);
        log.info("B sending Bid: "+ message);
        sendMessage(auctioneer.getAddress(), message);
    }

    private synchronized int getBidderCount() {
        AgentDescription description = new AgentDescription(null, "BidderAgent", null, null, null, null);
        List<IAgentDescription> list = thisAgent.searchAllAgents(description);
        if(list == null)
            return 1;
        return list.size();
    }

    private void invokeSimple(String actionName, Serializable... params) {
        invoke(retrieveAction(actionName), params);
    }

    public class MessageObserver implements SpaceObserver<IFact> {
        private static final long serialVersionUID = 3252158684429257439L;

        @SuppressWarnings("rawtypes")
        @Override
        public void notify(SpaceEvent<? extends IFact> event) {
            if (event instanceof WriteCallEvent) {
                WriteCallEvent writeEvent = (WriteCallEvent) event;
                if (writeEvent.getObject() instanceof JiacMessage) {
                    JiacMessage message = (JiacMessage) writeEvent.getObject();
                    handleMessage(message);
                    memory.remove(message);
                }
            }
        }
    }
}
