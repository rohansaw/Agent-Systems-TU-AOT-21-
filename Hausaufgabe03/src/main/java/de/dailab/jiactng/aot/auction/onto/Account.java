package de.dailab.jiactng.aot.auction.onto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.dailab.jiactng.agentcore.knowledge.IFact;


public class Account implements IFact {
    private static final long serialVersionUID = 184766311824118260L;

    private final Map<Integer, Integer> resourcesCount;

    //average cost of single Resource
    private final Map<Integer, Double> costAverage;

    private int countBidders;

    private double[] probabilities;

    public Account(Wallet wallet, Integer countBidders){
        resourcesCount = new HashMap<>();
        costAverage = new HashMap<>();
        for(Resource r : Resource.values()){
            if(wallet != null) {
                resourcesCount.put(r.ordinal(), wallet.get(r));
            }else{
                resourcesCount.put(r.ordinal(), 0);
            }
            costAverage.put(r.ordinal(), 0.0);
        }
        this.countBidders = countBidders;
        probabilities = new double[9];
    }

    private Object deepCopy(Object o){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(o);

            //De-serialization of object
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            return in.readObject();
        }catch (Exception e){
            return null;
        }
    }

    public Account(Account account){
        this.probabilities = new double[9];
        this.resourcesCount = new HashMap<>();
        this.costAverage = new HashMap<>();
        if(account != null) {
            for(Map.Entry<Integer, Integer> e : account.getResourcesCount().entrySet()){
                this.resourcesCount.put(e.getKey().intValue(), e.getValue().intValue());
            }
            for(Map.Entry<Integer, Double> e : account.getCostAverage().entrySet()){
                this.costAverage.put(e.getKey().intValue(), e.getValue().doubleValue());
            }

            countBidders = account.getCountBidders();
            for(int i = 0; i < 9; i++){
                probabilities[i] = account.getProbabilities()[i];
            }
        }else{
            countBidders = 1;
        }
    }

    private void addResource(Resource res, double price){
        int count = resourcesCount.get(res.ordinal()) + 1;
        resourcesCount.replace(res.ordinal(), count);
        double cost = costAverage.get(res.ordinal());
        costAverage.replace(res.ordinal(), ((count - 1) * cost + price) / count);
    }

    public void addItem(List<Resource> res, double prize){
        double[] weights = GaussianElimination.weightResources(res, probabilities, prize);
        for(Resource r : res){
            addResource(r, weights[r.ordinal()]);
        }
    }

    private void removeResource(Resource res, double price){
        int count = resourcesCount.get(res.ordinal()) - 1;
        resourcesCount.replace(res.ordinal(), count);
        double cost = costAverage.get(res.ordinal());
        costAverage.replace(res.ordinal(), ((count + 1) * cost - price) / count);
    }

    public void removeItem(List<Resource> res, double prize){
        double[] weights = GaussianElimination.weightResources(res, probabilities, prize);
        for(Resource r : res){
            removeResource(r, weights[r.ordinal()]);
        }
    }

    public double getAverageCost(Resource res){
        return costAverage.get(res.ordinal());
    }

    public double getCostOfBundle(List<Resource> res){
        double ret = 0.0;
        for(Resource r : res){
            ret += getAverageCost(r);
        }
        return ret;
    }

    public void setProbabilities(Collection<Item> list) {
        probabilities = new double[9];
        int counter = 0;
        for (Item item : list){
            for (Resource r : item.getBundle()){
                probabilities[r.ordinal()]++;
                counter++;
            }
        }
        probabilities[Resource.J.ordinal()] = countBidders * resourcesCount.get(Resource.J.ordinal());
        probabilities[Resource.K.ordinal()] = countBidders * resourcesCount.get(Resource.K.ordinal());
        counter += countBidders * resourcesCount.get(Resource.J.ordinal()) + countBidders * resourcesCount.get(Resource.K.ordinal());
        for(int i = 0; i < 9; i++){
            probabilities[i] /= counter;
        }
    }

    public boolean contains(List<Resource> bundle) {
        return bundle.stream()
                .collect(Collectors.groupingBy(k -> k, Collectors.counting())).entrySet().stream()
                .allMatch(e -> get(e.getKey()) >= e.getValue());
    }

    public Integer get(Resource resource) {
        return this.resourcesCount.computeIfAbsent(resource.ordinal(), r -> 0);
    }

    public int getCountBidders(){ return countBidders;}
    public double[] getProbabilities(){ return probabilities;}
    public Map<Integer, Integer> getResourcesCount(){
        return resourcesCount;
    }
    public Map<Integer, Double> getCostAverage(){
        return costAverage;
    }
}
