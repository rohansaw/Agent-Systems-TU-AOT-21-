package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
            this.purchasePrices = new HashMap<>();
            for(Map.Entry<Integer, Double> e : pl.getPurchasePrices().entrySet()){
                this.purchasePrices.put(e.getKey().intValue(), e.getValue().doubleValue());
            }
            this.bundles = new HashMap<>();
            for(Map.Entry<Integer, List<Resource>> e : pl.getBundles().entrySet()){
                this.bundles.put(e.getKey().intValue(), new ArrayList<>(e.getValue()));
            }
        }else{
            this.purchasePrices = new HashMap<>();
            this.bundles = new HashMap<>();
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
        if (purchasePrices.containsKey(callId))
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

