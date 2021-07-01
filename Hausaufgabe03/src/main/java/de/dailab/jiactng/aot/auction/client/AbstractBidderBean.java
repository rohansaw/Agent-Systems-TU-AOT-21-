package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.AbstractMethodExposingBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.auction.onto.*;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AbstractBidderBean extends AbstractMethodExposingBean {

    protected String groupToken;
    protected String messageGroup;
    protected String bidderId;

    protected void sendMessage(ICommunicationAddress receiver, IFact payload) {
        Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
        JiacMessage message = new JiacMessage(payload);
        invoke(sendAction, new Serializable[] {message, receiver});
        log.info("Bidder SENDING " + payload);
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
