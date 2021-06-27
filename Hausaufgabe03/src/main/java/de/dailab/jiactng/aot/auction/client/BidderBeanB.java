package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;


import java.util.*;
public class BidderBeanB extends AbstractBidderBean{

    int turn = 0;
    Wallet wallet;
    Auctioneer auctioneer;
    PriceList priceList;
    Account account;

    @Override
    public void doStart() throws Exception {
    }

    @Override
    public void execute() {
        turn++;
        if(wallet == null) return;
    }

    private synchronized void updateWallet(){
        Wallet w = new Wallet(wallet.getBidderId(), wallet.getCredits());

        wallet = memory.read(new Wallet(bidderId, null));
        for(Resource r : Resource.values()){
            w.add(r, wallet.get(r));
        }
        wallet = w;
    }
    private synchronized void updatePriceList(){

        priceList = memory.read(new PriceList(null));
        priceList = new PriceList(priceList.getPrices());

    }

    private synchronized void updateAccount(){
        account = memory.read(new Account((Wallet) null));
        account = new Account(account);

    }

    public static final String ACTION_START_AUCTION = "BidderB#startAuction";
    @Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public synchronized void startAuction(StartAuction msg, ICommunicationAddress address) {
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        turn = 0;
        updateWallet();
        updatePriceList();
        updateAccount();
    }

    public static final String CALL_FOR_BIDS = "BidderB#callForBids";
    @Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public synchronized void callForBids(CallForBids msg) {

        Set<List<Resource>> list = new HashSet<>();
        HashMap<List<Resource>, Double> profit = new HashMap<>();
        Random r = new Random();

        if(msg.getMode() == CallForBids.CfBMode.SELL && wallet.contains(msg.getBundle())){
            for(List<Resource> l : priceList.getPrices().keySet()){
                if(wallet.contains(l)) {
                    list.add(l);
                    double prof = priceList.getPrice(l) - account.getCostOfBundle(l);
                    profit.put(l, prof);
                }
            }
            if(priceList.getPrice(msg.getBundle())> account.getCostOfBundle(msg.getBundle())){
                Double bid= r.nextInt((int) priceList.getPrice(msg.getBundle()) - (int) account.getCostOfBundle(msg.getBundle()))+account.getCostOfBundle(msg.getBundle());
                if(bid> msg.getMinOffer())
                    sendBid(bid, msg.getCallId());
                else return;
            }
            else{
                Double bid = account.getCostOfBundle(msg.getBundle());
                if(bid> msg.getMinOffer())
                    sendBid(bid, msg.getCallId());
                else return;
            }


        }

    }
    private void sendBid(Double bid, Integer callId) {
        Bid message = new Bid(auctioneer.getAuctioneerId(), bidderId, callId, bid);
        sendMessage(auctioneer.getAddress(), message);
    }

}
