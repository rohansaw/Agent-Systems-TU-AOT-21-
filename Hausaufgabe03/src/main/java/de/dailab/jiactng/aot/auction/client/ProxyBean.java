package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
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

public class ProxyBean extends AbstractBidderBean {

    IGroupAddress groupAddress;
    String messageGroup;

    @Override
    public void doStart() throws Exception {
        memory.attach(new MessageObserver(), new JiacMessage());

        groupAddress = CommunicationAddressFactory.createGroupAddress(messageGroup);
        Action joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke(joinAction, new Serializable[]{groupAddress});
    }

    private void handleMessage(JiacMessage message){
        Object payload = message.getPayload();
        log.info("Bidder RRECEIVED:");
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
    }

    private synchronized void register(ICommunicationAddress auctioneer) {
        memory.removeAll(new Wallet(null, null));
        memory.removeAll(new Auctioneer(null, null, null));
        Register message = new Register(bidderId, groupToken);
        sendMessage(auctioneer, message);
    }

    private synchronized void initialize(InitializeBidder message) {
        memory.write(message.getWallet());
    }

    private synchronized void handleStartAuction(StartAuction msg, ICommunicationAddress sender) {
        memory.write(new Auctioneer(msg.getAuctioneerId(), sender, msg.getMode()));
        switch (msg.getMode()){
            case A:
            case B:
            case C:
                invokeSimple(BidderBeanC.ACTION_START_AUCTION, msg, sender);
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
