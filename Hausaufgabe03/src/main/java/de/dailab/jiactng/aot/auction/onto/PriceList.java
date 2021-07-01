package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.util.*;
import java.util.stream.Collectors;

public class PriceList implements IFact {
    private static final long serialVersionUID = -8577370212807242409L;

    private final HashMap<Integer, Double> purchasePrices;

    private final HashMap<Integer, List<Resource>> callIds;

    public PriceList(PriceList pl) {
        if(pl != null) {
            purchasePrices = new HashMap<>(pl.getPurchasePrices());
            callIds = new HashMap<>(pl.getCallIds());
        }else {
            purchasePrices = new HashMap<>();
            callIds = new HashMap<>();
        }
    }

    // This List contains the current Price for every resource bundle that can be achieved when
    // selling it in Auction B
    public HashMap<List<Resource>, Double> getPrices() {
        HashMap<List<Resource>, Double> ret = new HashMap<>();
        for(Integer i : callIds.keySet()){
            ret.put(callIds.get(i), purchasePrices.get(i));
        }
        return ret;
    }

    public double getPrice(List<Resource> res){
        Integer cid = getCallId(res);
        if(cid != null)
            return purchasePrices.get(cid);
        return 0.0;
    }

    public double getPrice(int callId){
        if(purchasePrices.containsKey(callId))
            return purchasePrices.get(callId);
        return 0.0;
    }

    public List<Resource> getResList(int callId){
        if(purchasePrices.containsKey(callId))
            return callIds.get(callId);
        return new LinkedList<>();
    }

    public Integer getCallId(List<Resource> res) {
        Wallet w = new Wallet(null, 0.0);
        w.add(res);
        for (Map.Entry<Integer, List<Resource>> entry : callIds.entrySet()) {
            if (entry.getValue().size() == res.size() && w.contains(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void setPrice(List<Resource> res, double price, int callId) {
        Integer cId = getCallId(res);
        if(cId != null){
            purchasePrices.remove(cId);
            callIds.remove(cId);
        }
        purchasePrices.put(callId, price);
        callIds.put(callId, res);
    }

    public int getSize(){
        return purchasePrices.size();
    }

    public HashMap<Integer, List<Resource>> getCallIds(){
        return callIds;
    }

    public HashMap<Integer, Double> getPurchasePrices(){
        return purchasePrices;
    }
}

