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


public class ProxyBean extends AbstractBidderBean {

    IGroupAddress groupAddress;
    HashMap<Integer, Auctioneer> auctioneers;
    Wallet wallet;
    Account account;
    PriceList priceList;
    int countCFBfromB = 0;
    int plSize = 0;
    int countOnlyCFBB = 0;
    int countBidder = 0;

    //only register for one auction
    boolean ready = true;

    @Override
    public void doStart() throws Exception {
        memory.attach(new MessageObserver(), new JiacMessage());

        groupAddress = CommunicationAddressFactory.createGroupAddress(messageGroup);
        Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke(joinAction, new Serializable[]{groupAddress});
    }

    @Override
    public synchronized void execute(){
        countOnlyCFBB++;
    }

    private void handleMessage(JiacMessage message){
        Object payload = message.getPayload();
        log.info("--------------");
        Wallet w = memory.read(new Wallet(bidderId, null));
        if(w != null){
            log.info(w.toString());
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
            memory.removeAll(new PriceList(null));
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
            priceList = new PriceList(null);
            memory.write(priceList);
            memory.write(wallet);
            memory.write(account);
        } else {
            log.warn("Wallet was null ?!");
        }
    }

    private void handleStartAuction(StartAuction msg, ICommunicationAddress sender) {
        auctioneers.put(msg.getAuctioneerId(), new Auctioneer(msg.getAuctioneerId(), sender, msg.getMode()));
        countOnlyCFBB = 0;
        switch (msg.getMode()){
            case A:
                account.setProbabilities(msg.getInitialItems());
                invokeSimple(BidderBeanA.ACTION_START_AUCTION, msg, sender);
                break;
            case B:
                plSize = msg.getNumItems();
                invokeSimple(BidderBeanB.ACTION_START_AUCTION, msg, sender);
                break;
            case C:
                invokeSimple(BidderBeanC.ACTION_START_AUCTION, msg, sender, countBidder);
        }
    }

    private synchronized void handleCallForBids(CallForBids msg){
        Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
        switch (auctioneer.getMode()){
            case A:
                invokeSimple(BidderBeanA.CALL_FOR_BIDS, msg);
                break;
            case B:
                countCFBfromB++;
                priceList.setPrice(msg.getBundle(), msg.getMinOffer(), msg.getCallId());
                if(countCFBfromB == plSize) {
                    invokeSimple(BidderBeanB.CALL_FOR_BIDS, countOnlyCFBB >= 2);
                    countCFBfromB = 0;
                }
                break;
            case C:
                //after auction C closed only B is open for sell
                countOnlyCFBB = 0;
                invokeSimple(BidderBeanC.CALL_FOR_BIDS, msg);
        }
    }

    private synchronized void handleInformBuy(InformBuy msg) {
        if (msg.getBundle() != null && msg.getType() == InformBuy.BuyType.WON) {
            wallet.add(msg.getBundle());
            wallet.updateCredits(-msg.getPrice());
            account.addItem(msg.getBundle(), msg.getPrice());
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
        int count = 0;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://130.149.154.9:8080/auctionobserver"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int first = response.body().indexOf("[");
            int last = response.body().indexOf("]");
            int index = -1;
            String list = "";
            if(first > 0 && last > 0) {
                list = response.body().substring(first, last + 1);
                index = 0;
            }
            while (index != -1){
                index = list.indexOf(", ", index);
                if(index != -1){
                    count++;
                    index += 2;
                }
            }
            count++;

            //searchAllAgents not guaranteed to find all
            //searchAllAgents finds itself
        } catch (Exception e) {
            count = thisAgent.searchAllAgents(new AgentDescription(null, "BidderAgent", null, null, null, null)).size();
        }
        if(count == 0)
            count = thisAgent.searchAllAgents(new AgentDescription(null, "BidderAgent", null, null, null, null)).size();

        return count;
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
