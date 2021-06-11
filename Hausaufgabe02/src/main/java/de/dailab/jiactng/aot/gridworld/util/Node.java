package de.dailab.jiactng.aot.gridworld.util;

import de.dailab.jiactng.aot.gridworld.model.Order;

public class Node {
    Order order;
    public Node(Order order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return order.id.equals(node.order.id);
    }

    @Override
    public int hashCode() {
        return order.id.hashCode();
    }
}