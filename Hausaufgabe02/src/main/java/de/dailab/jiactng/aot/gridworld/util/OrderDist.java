package de.dailab.jiactng.aot.gridworld.util;

import de.dailab.jiactng.aot.gridworld.model.Order;

public class OrderDist  implements Comparable<OrderDist>{
    public Order order;
    public Integer distance;
    public OrderDist(Order order, Integer distance) {
        this.order = order;
        this.distance = distance;
    }

    @Override
    public int compareTo(OrderDist od) {
        return this.distance - od.distance;
    }
}
