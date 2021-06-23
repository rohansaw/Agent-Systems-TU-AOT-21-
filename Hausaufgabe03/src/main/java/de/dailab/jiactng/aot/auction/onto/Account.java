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
            resourcesCount.put(r, wallet.get(r));
            costAverage.put(r, 0.0);
        }
    }

    //TODO maybe weight cost with probability of occurrence

    private void addResource(Resource res, double price){
        int count = resourcesCount.get(res) + 1;
        resourcesCount.replace(res, count);
        double cost = costAverage.get(res);
        costAverage.replace(res, ((count - 1) * cost + price) / count);
    }

    public void addItem(InformBuy ib){
        double averagePrice = ib.getPrice() / ib.getBundle().size();
        for(Resource res : ib.getBundle()){
            addResource(res, averagePrice);
        }
    }

    private void removeResource(Resource res, double price){
        int count = resourcesCount.get(res) - 1;
        resourcesCount.replace(res, count);
        double cost = costAverage.get(res);
        costAverage.replace(res, ((count + 1) * cost - price) / count);
    }

    public void removeItem(InformSell is){
        double averagePrice = is.getPrice() / is.getBundle().size();
        for(Resource res : is.getBundle()){
            removeResource(res, averagePrice);
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
