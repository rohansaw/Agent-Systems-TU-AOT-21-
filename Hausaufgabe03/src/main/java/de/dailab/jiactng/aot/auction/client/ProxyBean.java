package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.*;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.auction.onto.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;


public class ProxyBean extends AbstractBidderBean {

    private IGroupAddress groupAddress;
    private HashMap<Integer, Auctioneer> auctioneers;
    private Wallet wallet;
    private Account account;
    private PriceList priceList;
    private int plSize = 0;
    private int countBidder = 0;
    private int countCFBforB = 0;
    private int countOnlyCFBForB = 0;

    //only register for one auction
    private boolean ready = true;

    @Override
    public void doStart() throws Exception {
        memory.attach(new MessageObserver(), new JiacMessage());

        groupAddress = CommunicationAddressFactory.createGroupAddress(messageGroup);
        Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke(joinAction, new Serializable[]{groupAddress});
    }

    @Override
    public synchronized void execute(){
        countOnlyCFBForB++;
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
            Register message = new Register(bidderId, groupToken);
            sendMessage(auctioneer, message);
        }
    }

    private synchronized void initialize(InitializeBidder msg) {
        wallet = msg.getWallet();
        if(wallet != null) {
            countBidder = getBidderCount();
            account = new Account(wallet, countBidder);
            memory.write(wallet);
            memory.write(account);
        } else {
            log.warn("Wallet was null ?!");
        }
    }

    private void handleStartAuction(StartAuction msg, ICommunicationAddress sender) {
        countOnlyCFBForB = 0;
        auctioneers.put(msg.getAuctioneerId(), new Auctioneer(msg.getAuctioneerId(), sender, msg.getMode()));
        switch (msg.getMode()){
            case A:
                account.setProbabilities(msg.getInitialItems());
                invokeSimple(BidderBeanA3.ACTION_START_AUCTION, msg, sender);
                break;
            case B:
                plSize = msg.getNumItems();
                priceList = new PriceList(msg);
                memory.write(priceList);
                //update list after priceList ist initialized
                invokeSimple(BidderBeanA3.UPDATE_BUY_LIST);
                invokeSimple(BidderBeanB2.ACTION_START_AUCTION, msg, sender);
                break;
            case C:
                invokeSimple(BidderBeanC.ACTION_START_AUCTION, msg, sender, countBidder);
        }
    }

    private synchronized void handleCallForBids(CallForBids msg){
        Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
        switch (auctioneer.getMode()){
            case A:
                countOnlyCFBForB = 0;
                invokeSimple(BidderBeanA3.CALL_FOR_BIDS, msg);
                break;
            case B:
                countCFBforB++;
                priceList.setPrice(msg.getBundle(), msg.getMinOffer(), msg.getCallId());
                if(countCFBforB == plSize) {
                    invokeSimple(BidderBeanB2.CALL_FOR_BIDS, countOnlyCFBForB >= 2);
                    countCFBforB = 0;
                }
                break;
            case C:
                //after auction C closed only B is open for sell
                countOnlyCFBForB = 0;
                invokeSimple(BidderBeanC.CALL_FOR_BIDS, msg);
        }
    }

    private synchronized void handleInformBuy(InformBuy msg) {
        if (msg.getType() == InformBuy.BuyType.WON) {
            wallet.add(msg.getBundle());
            wallet.updateCredits(-msg.getPrice());
            account.addItem(msg.getBundle(), msg.getPrice());
        }else if (msg.getType() == InformBuy.BuyType.LOST && msg.getCallId() < 200000) {
            //msg.getCallId() < 200000 -> msg from A
            // Our strategy did not work out, so we need to recalculate
            invokeSimple(BidderBeanA3.UPDATE_BUY_LIST);
        }
    }

    private synchronized void handleInformSell(InformSell msg) {
        if (msg.getBundle() != null && msg.getType() == InformSell.SellType.SOLD) {
            wallet.remove(msg.getBundle());
            wallet.updateCredits(msg.getPrice());
            account.removeItem(msg.getBundle(), msg.getPrice());
        }
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
