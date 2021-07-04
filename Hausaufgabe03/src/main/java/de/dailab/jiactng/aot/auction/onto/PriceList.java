package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.util.*;
import java.util.stream.Collectors;

public class PriceList implements IFact {
    private static final long serialVersionUID = -8577370212807242409L;

    private final HashMap<Integer, Double> purchasePrices;

    private final HashMap<Integer, List<Resource>> bundles;

    public PriceList(StartAuction msg) {
            purchasePrices = new HashMap<>();
            bundles = new HashMap<>();
        for(Item i : msg.getInitialItems()){
            purchasePrices.put(i.getCallId(), i.getPrice());
            bundles.put(i.getCallId(), i.getBundle());
        }
    }

    public PriceList(PriceList pl) {
        if(pl != null) {
            purchasePrices = new HashMap<>(pl.getPurchasePrices());
            bundles = new HashMap<>(pl.getBundles());
        }else {
            purchasePrices = new HashMap<>();
            bundles = new HashMap<>();
        }
    }

    // This List contains the current Price for every resource bundle that can be achieved when
    // selling it in Auction B
    public HashMap<List<Resource>, Double> getPrices() {
        HashMap<List<Resource>, Double> ret = new HashMap<>();
        for(Integer i : bundles.keySet()){
            ret.put(bundles.get(i), purchasePrices.get(i));
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
            return bundles.get(callId);
        return new LinkedList<>();
    }

    public Integer getCallId(List<Resource> res) {
        Wallet w = new Wallet(null, 0.0);
        w.add(res);
        for (Map.Entry<Integer, List<Resource>> entry : bundles.entrySet()) {
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
            bundles.remove(cId);
        }
        purchasePrices.put(callId, price);
        bundles.put(callId, res);
    }

    public void setPrice(double price, int callId) {
        purchasePrices.put(callId, price);
    }

    public int getSize(){
        return purchasePrices.size();
    }

    public HashMap<Integer, List<Resource>> getBundles(){
        return bundles;
    }

    public HashMap<Integer, Double> getPurchasePrices(){
        return purchasePrices;
    }
}

