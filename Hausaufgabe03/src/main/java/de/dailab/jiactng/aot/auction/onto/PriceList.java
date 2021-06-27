package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PriceList implements IFact {
    private static final long serialVersionUID = -8577370212807242409L;

    private HashMap<List<Resource>, Double> purchasePrices;

    public PriceList(HashMap<List<Resource>, Double> prices) {
        if(prices != null)
            purchasePrices = new HashMap<>(prices);
        else
            purchasePrices = new HashMap<>();
    }

    public PriceList(List<Resource> res, double price){
        purchasePrices = new HashMap<>();
        if(res != null)
            purchasePrices.put(res, price);
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

    public void setPrices(HashMap<List<Resource>, Double> prices) {
        purchasePrices = prices;
    }

    public void setPrice(List<Resource> res, double price) {
        purchasePrices.put(res, price);
    }
}

