package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

public class ResProfitPair{
    private final List<Resource> res;
    private final double profit;
    private final int cid;
    public ResProfitPair(List<Resource> res, double profit, int cid){
        this.res = res;
        this.profit = profit;
        this.cid = cid;
    }
    public double getProfit(){
        return profit;
    }
    public int getCid(){return cid;}
    public List<Resource> getRes(){
        return res;
    }
}
