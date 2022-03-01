package main.java.tools;

public class ElementDistance {

    public Float index;
    public double distance;

    public ElementDistance(Float index, double d) {
        this.index = index;
        this.distance = d;
    }

    public boolean equals(Object o) {
        ElementDistance bd = (ElementDistance) o;
        return index == bd.index && distance == bd.distance;
    }
}