package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PriceList implements IFact {
    private static final long serialVersionUID = -8577370212807242409L;

    private HashMap<List<Resource>, Double> purchasePrices;

    private HashMap<List<Resource>, Integer> callIds;

    public PriceList(HashMap<List<Resource>, Double> prices) {
        if(prices != null) {
            purchasePrices = new HashMap<>(prices);
        }else {
            purchasePrices = new HashMap<>();
        }
        callIds = new HashMap<>();
    }

    // This List contains the current Price for every resource bundle that can be achieved when
    // selling it in Auction B
    public HashMap<List<Resource>, Double> getPrices() {
        return purchasePrices;
    }

    public double getPrice(List<Resource> res){
        if(purchasePrices.containsKey(res))
            return purchasePrices.get(res);
        return 0.0;
    }

    public void setToZero(){
        for (List<Resource> res : purchasePrices.keySet()){
            purchasePrices.put(res, 0.0);
        }
    }

    public void setPrices(HashMap<List<Resource>, Double> prices) {
        purchasePrices = prices;
    }

    public Integer getCallId(List<Resource> res){
        return callIds.get(res);
    }

    public int setPrice(List<Resource> res, double price, int callId) {
        purchasePrices.put(res, price);
        callIds.put(res, callId);
        return purchasePrices.size();
    }
}

