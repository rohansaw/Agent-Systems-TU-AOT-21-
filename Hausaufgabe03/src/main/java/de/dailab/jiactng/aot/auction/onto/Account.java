package de.dailab.jiactng.aot.auction.onto;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dailab.jiactng.agentcore.knowledge.IFact;
import org.jblas.Solve;
import org.jblas.DoubleMatrix;

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
        DoubleMatrix weights = calcWeights(res, prize);
        for(Resource r : res){
            addResource(r, weights.get(r.ordinal()));
        }
    }

    private void removeResource(Resource res, double price){
        int count = resourcesCount.get(res) - 1;
        resourcesCount.replace(res, count);
        double cost = costAverage.get(res);
        costAverage.replace(res, ((count + 1) * cost - price) / count);
    }

    public void removeItem(List<Resource> res, double prize){
        DoubleMatrix weights = calcWeights(res, prize);
        for(Resource r : res){
            removeResource(r, weights.get(r.ordinal()));
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

    private DoubleMatrix calcWeights(List<Resource> res, double prize){
        //P(j), P(k) unknown => j, k not in calculation
        int[] count = new int[7];
        double[][] matrix = new double[8][8];
        double[] b = new double[8];
        for(Resource r : res){
            count[r.ordinal()]++;
        }

        for (int row = 0; row < 7; row++) {
            if(count[row] == 0) continue;
            double coef = probabilities[row] / count[row];

            for (int col = 0; col < 7; col++) {
                if (col == row || count[col] == 0) continue;
                matrix[row][col] = coef * count[col];
            }

            matrix[row][7] = 1;
            b[row] = coef * prize;
        }
        for (int i = 0; i < 7; i++){
            matrix[7][i] = count[i];
        }
        b[7] = prize;

        DoubleMatrix matrixA = new DoubleMatrix(matrix);
        DoubleMatrix matrixB = new DoubleMatrix(b);
        return Solve.solve(matrixA, matrixB);
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
