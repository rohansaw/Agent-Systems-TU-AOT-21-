package de.dailab.jiactng.aot.auction.client;

import com.sun.org.apache.xml.internal.security.Init;
import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.AbstractMethodExposingBean;
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

public class AbstractBidderBean extends AbstractMethodExposingBean {

    String groupToken;
    String messageGroup;
    String bidderId;


    protected void addAuctioneer(Auctioneer auctioneer) {
        memory.write(auctioneer);
    }

    protected Auctioneer getAuctioneer(Integer id) {
        return id == null ? null : memory.read(new Auctioneer(id, null, null));
    }

    protected void sendMessage(ICommunicationAddress receiver, IFact payload) {
        Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
        JiacMessage message = new JiacMessage(payload);
        invoke(sendAction, new Serializable[] {message, receiver});
        System.out.println("Bidder SENDING " + payload);
    }

    //setters for bidder.xml

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public void setMessageGroup(String messageGroup) {
        this.messageGroup = messageGroup;
    }

    public void setGroupToken(String groupToken) {
        this.groupToken = groupToken;
    }


}
