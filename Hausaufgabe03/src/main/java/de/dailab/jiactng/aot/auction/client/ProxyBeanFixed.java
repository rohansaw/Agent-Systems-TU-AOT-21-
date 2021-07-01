package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
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
    int countCFBfromB = 0;
    int plSize = 0;
    PriceList priceList = new PriceList(null);
    List<Bid> offeredItems = new LinkedList<>();
    HashMap<Resource, Integer> reservedResources = new HashMap<>();
    Collection<Item> initialItems;
    boolean isFixed = false;
    HashMap<List<Resource>, Double> bundlesToBuy = new HashMap<>();
    ArrayList<Resource> resourcesToSell = new ArrayList<>();
    ArrayList<List<Resource>> bundlesToSell = new ArrayList<>();

    @Override
    public void doStart() throws Exception {
        memory.attach(new MessageObserver(), new JiacMessage());

        groupAddress = CommunicationAddressFactory.createGroupAddress(messageGroup);
        Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke(joinAction, new Serializable[]{groupAddress});
        priceList = new PriceList(null);
    }

    @Override
    public void execute(){
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
    }

    private synchronized void register(ICommunicationAddress auctioneer) {
        auctioneers = new HashMap<>();
        priceList = new PriceList(null);
        Register message = new Register(bidderId, groupToken);
        sendMessage(auctioneer, message);
    }

    private synchronized void initialize(InitializeBidder msg) {
        wallet = msg.getWallet();
        if(wallet != null) {
            account = new Account(wallet, 1);
        } else {
            log.warn("Wallet was null ?!");
        }
    }

    private void handleStartAuction(StartAuction msg, ICommunicationAddress sender) {
        auctioneers.put(msg.getAuctioneerId(), new Auctioneer(msg.getAuctioneerId(), sender, msg.getMode()));
        if(msg.getMode() == StartAuction.Mode.A && msg.getInitialItems() != null) {
            isFixed = true;
            initialItems = msg.getInitialItems();
            calculateBundlesToBuy();
        }
    }

    private void calculateBundlesToBuy(){
        Collection<Item> futureBundles = initialItems;
        for(Map.Entry<List<Resource>, Double> bundle: priceList.getPrices().entrySet()) {
            // identify which bundles we want to sell and which bundles we should buy in A therefore
            // this should be saved in bundlesToBuy (For A) and bundlesToSell (For B)
            // add items that come in bundles, but can not be used for bundles in B to resourcesToSell
        }
    }

    private void handleCallForBids(CallForBids msg){
        Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
        switch (auctioneer.getMode()){
            case A:
                handleCFB_A(msg);
                break;
            case B:
                countCFBfromB++;
                int oldPlSize = plSize;
                priceList.setPrice(msg.getBundle(), msg.getMinOffer(), msg.getCallId());
                if(countCFBfromB >= plSize && plSize == oldPlSize) {
                    handleCFB_B(msg);
                    countCFBfromB = 0;
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
