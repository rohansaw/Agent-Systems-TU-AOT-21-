package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.action.IMethodExposingBean;
import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.auction.onto.*;
import de.dailab.jiactng.aot.auction.server.AuctionRunnerBean;
import de.dailab.jiactng.aot.auction.server.AuctioneerCBean;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

public class BidderBeanC extends AbstractBidderBean {

    int turn = 0;
    HashMap<Integer, StartAuction.Mode> auctioneerModes = new HashMap<>();
    HashMap<StartAuction.Mode, Integer> auctioneerIds = new HashMap<>();
    HashMap<StartAuction.Mode, ICommunicationAddress> auctioneerAddresses = new HashMap<>();

    Wallet wallet;
    Auctioneer auctioneer;

    @Override
    public void doStart() throws Exception {
    }

    @Override
    public void execute() {
        turn++;
        if(wallet == null) return;
    }

    public static final String ACTION_START_AUCTION = "BidderC#startAuction";

    @Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public synchronized void startAuction(StartAuction msg, ICommunicationAddress address) {
        wallet = memory.read(new Wallet(bidderId, null));
        auctioneer = memory.read(new Auctioneer(msg.getAuctioneerId(), address, msg.getMode()));
    }

}