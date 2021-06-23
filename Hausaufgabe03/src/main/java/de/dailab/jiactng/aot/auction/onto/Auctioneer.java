package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.knowledge.IFact;

public class Auctioneer implements IFact {

    private static final long serialVersionUID = -2272277026805904128L;

    private final StartAuction.Mode mode;

    private final Integer auctioneerId;

    private final ICommunicationAddress address;


    public Auctioneer(Integer id, ICommunicationAddress address, StartAuction.Mode mode) {
        this.auctioneerId = id;
        this.address = address;
        this.mode = mode;
    }

    public Integer getAuctioneerId() {
        return auctioneerId;
    }

    // this method seems to be needed in order for the Memory to work properly
    public void setAuctioneerId(Integer id) {
        throw new UnsupportedOperationException();
    }

    public ICommunicationAddress getAddress() {
        return address;
    }

    public StartAuction.Mode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return String.format("Bidder(id=%d, address=%s, mode=%s)", auctioneerId, address, mode);
    }

}
