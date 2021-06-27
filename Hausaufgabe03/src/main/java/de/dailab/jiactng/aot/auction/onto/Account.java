package de.dailab.jiactng.aot.auction.onto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.dailab.jiactng.agentcore.knowledge.IFact;

public class Account implements IFact {
    private static final long serialVersionUID = 184766311824118260L;

    private final Map<Resource, Integer> resourcesCount;

    //average cost of single Resource
    private final Map<Resource, Double> costAverage;

    public Account(Wallet wallet){
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
    }

    public Map<Resource, Integer> getResourcesCount(){
        return resourcesCount;
    }

    public Map<Resource, Double> getCostAverage(){
        return costAverage;
    }

    public Account(Account account){
        if(account != null) {
            resourcesCount = new HashMap<>(account.getResourcesCount());
            costAverage = new HashMap<>(account.getCostAverage());
        }else{
            resourcesCount = new HashMap<>();
            costAverage = new HashMap<>();
        }
    }

    //TODO maybe weight cost with probability of occurrence

    private void addResource(Resource res, double price){
        int count = resourcesCount.get(res) + 1;
        resourcesCount.replace(res, count);
        double cost = costAverage.get(res);
        costAverage.replace(res, ((count - 1) * cost + price) / count);
    }

    public void addItem(List<Resource> res, double prize){
        double averagePrice = prize / res.size();

        for(Resource r : res){
            addResource(r, averagePrice);
        }
    }

    private void removeResource(Resource res, double price){
        int count = resourcesCount.get(res) - 1;
        resourcesCount.replace(res, count);
        double cost = costAverage.get(res);
        costAverage.replace(res, ((count + 1) * cost - price) / count);
    }

    public void removeItem(List<Resource> res, double prize){
        double averagePrice = prize / res.size();
        for(Resource r : res){
            removeResource(r, averagePrice);
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
}