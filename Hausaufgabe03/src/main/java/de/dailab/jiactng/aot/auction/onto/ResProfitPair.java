package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

public class ResProfitPair{
    private final List<Resource> res;
    private final Double profit;
    public ResProfitPair(List<Resource> res, double profit){
        this.res = res;
        this.profit = profit;
    }
    public Double getProfit(){
        return profit;
    }
    public List<Resource> getRes(){
        return res;
    }
}
