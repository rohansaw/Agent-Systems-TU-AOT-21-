package de.dailab.jiactng.aot.auction.onto;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dailab.jiactng.agentcore.knowledge.IFact;


public class Account implements IFact {
    private static final long serialVersionUID = 184766311824118260L;

    private final Map<Resource, Integer> resourcesCount;

    //average cost of single Resource
    private final Map<Resource, Double> costAverage;

    private int countBidders;

    private double[] probabilities;

    public Account(Wallet wallet, Integer countBidders){
        resourcesCount = new HashMap<>();
        costAverage = new HashMap<>();
        for(Resource r : Resource.values()){
            if(wallet != null) {
                resourcesCount.put(r, wallet.get(r));
            }else{
                resourcesCount.put(r, 0);
            }
            costAverage.put(r, 0.0);
        }
        this.countBidders = countBidders;
        probabilities = new double[9];
    }

    public Account(Account account){
        probabilities = new double[9];
        if(account != null) {
            resourcesCount = new HashMap<>(account.getResourcesCount());
            costAverage = new HashMap<>(account.getCostAverage());
            countBidders = account.getCountBidders();
            for(int i = 0; i < 9; i++){
                probabilities[i] = account.getProbabilities()[i];
            }
        }else{
            resourcesCount = new HashMap<>();
            costAverage = new HashMap<>();
            countBidders = 1;
        }
    }

    private void addResource(Resource res, double price){
        int count = resourcesCount.get(res) + 1;
        resourcesCount.replace(res, count);
        double cost = costAverage.get(res);
        costAverage.replace(res, ((count - 1) * cost + price) / count);
    }

    public void addItem(List<Resource> res, double prize){
        double[] weights = GaussianElimination.weightResources(res, probabilities, prize);
        for(Resource r : res){
            addResource(r, weights[r.ordinal()]);
        }
    }

    private void removeResource(Resource res, double price){
        int count = resourcesCount.get(res) - 1;
        resourcesCount.replace(res, count);
        double cost = costAverage.get(res);
        costAverage.replace(res, ((count + 1) * cost - price) / count);
    }

    public void removeItem(List<Resource> res, double prize){
        double[] weights = GaussianElimination.weightResources(res, probabilities, prize);
        for(Resource r : res){
            removeResource(r, weights[r.ordinal()]);
        }
    }

    public double getAverageCost(Resource res){
        return costAverage.get(res);
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
        probabilities[Resource.J.ordinal()] = countBidders * resourcesCount.get(Resource.J);
        probabilities[Resource.K.ordinal()] = countBidders * resourcesCount.get(Resource.K);
        counter += countBidders * resourcesCount.get(Resource.J) + countBidders * resourcesCount.get(Resource.K);
        for(int i = 0; i < 9; i++){
            probabilities[i] /= counter;
        }
    }

    public int getCountBidders(){ return countBidders;}
    public double[] getProbabilities(){ return probabilities;}
    public Map<Resource, Integer> getResourcesCount(){
        return resourcesCount;
    }
    public Map<Resource, Double> getCostAverage(){
        return costAverage;
    }
}
