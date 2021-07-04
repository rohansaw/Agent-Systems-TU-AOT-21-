package de.dailab.jiactng.aot.auction.client;

import de.dailab.jiactng.agentcore.action.scope.ActionScope;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.aot.auction.onto.*;

import java.util.*;
import java.util.stream.Collectors;

public class BidderBeanB2 extends AbstractBidderBean {

    int turn = 0;
    Wallet wallet;
    Auctioneer auctioneer;
    PriceList priceList;
    Account account;
    int countAllCFB = 0;

    @Override
    public void doStart() throws Exception {
    }

    @Override
    public void execute() {
    }

    private synchronized void updateData() {
        Wallet w = memory.read(new Wallet(bidderId, null));
        wallet = new Wallet(w.getBidderId(), w.getCredits());
        for (Resource r : Resource.values()) {
            wallet.add(r, w.get(r));
        }

        priceList = memory.read(new PriceList((PriceList) null));
        priceList = new PriceList(priceList);

        account = memory.read(new Account((Account) null));
        account = new Account(account);
    }

    public static final String ACTION_START_AUCTION = "BidderB#startAuction";
    @Expose(name = ACTION_START_AUCTION, scope = ActionScope.AGENT)
    public synchronized void startAuction(StartAuction msg, ICommunicationAddress address) {
        auctioneer = new Auctioneer(msg.getAuctioneerId(), address, msg.getMode());
        turn = 0;
        wallet = memory.read(new Wallet(bidderId, null));
    }

    public static final String CALL_FOR_BIDS = "BidderB#callForBids";
    @Expose(name = CALL_FOR_BIDS, scope = ActionScope.AGENT)
    public synchronized void callForBids(boolean sellAll) {
        countAllCFB++;
        if (countAllCFB % windowSize != 0)
            return;

        updateData();

        while (true) {
            List<Map.Entry<List<Resource>, Double>> buy = priceList.getPrices().entrySet()
                    .stream()
                    .filter(e -> wallet.contains(e.getKey())).collect(Collectors.toList());
            if (buy.size() == 0)
                break;
            List<ResProfitPair> profitList = new ArrayList<>(buy.size());
            for (Map.Entry<List<Resource>, Double> elem : buy) {
                profitList.add(new ResProfitPair(elem.getKey(), elem.getValue() - account.getCostOfBundle(elem.getKey())));
            }
            ResProfitPair max = profitList.stream()
                    .max(Comparator.comparing(ResProfitPair::getProfit))
                    .orElse(null);
            if (max != null && (max.getProfit() > 0 || sellAll)) {
                wallet.remove(max.getRes());
                Integer cid = priceList.getCallId(max.getRes());
                priceList.setPrice(max.getProfit() - 5, cid);
                account.removeItem(max.getRes(), priceList.getPrice(max.getRes()));
                sendBid(cid);
            } else {
                break;
            }
        }
    }

    private void sendBid(Integer callId) {
        Bid message = new Bid(auctioneer.getAuctioneerId(), bidderId, callId, null);
        log.info("B sending Bid: " + message);
        sendMessage(auctioneer.getAddress(), message);
    }
}
