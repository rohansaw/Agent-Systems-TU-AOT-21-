package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.util.ArrayList;
import java.util.HashMap;

public class PriceList implements IFact {
    private static final long serialVersionUID = 6349483199350325596L;

    private HashMap<ArrayList<Resource>, Double> purchasePrices;

    // This List contains the current Price for every resource bundle that can be achieved when
    // selling it in Auction B
    public HashMap<ArrayList<Resource>, Double> getPrices() {
        return purchasePrices;
    }

    public void setPrices( HashMap<ArrayList<Resource>, Double> prices) {
        purchasePrices = prices;
    }
}
