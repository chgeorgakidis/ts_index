package main.java.tools;

import main.java.tsindex.Node;

public class NodeDistance implements Comparable<NodeDistance> {

    public Node n;
    public double distance;

    public NodeDistance() {
    }

    public NodeDistance(Node n, double d) {
        this.n = n;
        this.distance = d;
    }

    @Override
    public int compareTo(NodeDistance bd) {
        if (distance < bd.distance) {
            return -1;
        } else if (distance > bd.distance) {
            return 1;
        } else {
            int th = System.identityHashCode(n);
            int oh = System.identityHashCode(bd.n);
            if (th < oh) {
                return -1;
            } else if (th > oh) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public boolean equals(Object o) {
        NodeDistance bd = (NodeDistance) o;
        return n == bd.n && distance == bd.distance;
    }
}