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
import java.util.HashMap;

public class ProxyBean extends AbstractBidderBean {

    IGroupAddress groupAddress;
    String messageGroup;
    HashMap<Integer, Auctioneer> auctioneers;

    @Override
    public void doStart() throws Exception {
        memory.attach(new MessageObserver(), new JiacMessage());

        groupAddress = CommunicationAddressFactory.createGroupAddress(messageGroup);
        Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke(joinAction, new Serializable[]{groupAddress});
    }

    private void handleMessage(JiacMessage message){
        Object payload = message.getPayload();
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

    private void register(ICommunicationAddress auctioneer) {
        auctioneers = new HashMap<>();
        memoryLock.writeLock().lock();
        try {
            memory.removeAll(new Wallet(null, null));
            memory.removeAll(new Auctioneer(null, null, null));
        } finally {
            memoryLock.writeLock().unlock();
        }
        Register message = new Register(bidderId, groupToken);
        sendMessage(auctioneer, message);
    }

    private void initialize(InitializeBidder msg) {
        memoryLock.writeLock().lock();
        try {
            memory.write(msg.getWallet());
            memory.write(new Account(msg.getWallet()));
        }finally {
            memoryLock.writeLock().unlock();
        }
    }

    private void handleStartAuction(StartAuction msg, ICommunicationAddress sender) {
        auctioneers.put(msg.getAuctioneerId(), new Auctioneer(msg.getAuctioneerId(), sender, msg.getMode()));
        switch (msg.getMode()){
            case A:
                invokeSimple(BidderBeanA.ACTION_START_AUCTION, msg, sender);
                break;
            case B:
                break;
            case C:
                invokeSimple(BidderBeanC.ACTION_START_AUCTION, msg, sender);
        }
    }

    private void handleCallForBids(CallForBids msg){
        Auctioneer auctioneer = auctioneers.get(msg.getAuctioneerId());
        switch (auctioneer.getMode()){
            case A:
                invokeSimple(BidderBeanA.CALL_FOR_BIDS, msg);
                break;
            case B:
                memoryLock.writeLock().lock();
                try{//update priceList
                    PriceList pl = memory.read(new PriceList(null));
                    pl.setPrice(msg.getBundle(), msg.getMinOffer());
                }finally {
                    memoryLock.writeLock().unlock();
                }
                break;
            case C:
                invokeSimple(BidderBeanC.CALL_FOR_BIDS, msg);
        }
    }

    private void handleInformBuy(InformBuy msg){
        if(msg.getBundle() != null) {
            memoryLock.writeLock().lock();
            try {
                Wallet wallet = memory.read(new Wallet(bidderId, null));
                Account account = memory.read(new Account((Wallet) null));
                wallet.add(msg.getBundle());
                wallet.updateCredits(-msg.getPrice());
                account.addItem(msg.getBundle(), msg.getPrice());
            }finally {
                memoryLock.writeLock().unlock();
            }
        }
    }

    private void handleInformSell(InformSell msg){
        if(msg.getBundle() != null) {
            memoryLock.writeLock().lock();
            try {
                Wallet wallet = memory.read(new Wallet(bidderId, null));
                Account account = memory.read(new Account((Wallet) null));
                wallet.remove(msg.getBundle());
                wallet.updateCredits(msg.getPrice());
                account.removeItem(msg.getBundle(), msg.getPrice());
            }finally {
                memoryLock.writeLock().unlock();
            }
        }
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
