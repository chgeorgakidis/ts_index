package main.java.tools;

import java.util.Comparator;

public class DistComparator implements Comparator {

    public DistComparator() {}

    @Override
    public int compare(Object o1, Object o2) {
        ElementDistance is1 = (ElementDistance) o1;
        ElementDistance is2 = (ElementDistance) o2;
        if (is1.distance > is2.distance) {
            return 1;
        } else if (is1.distance < is2.distance) {
            return -1;
        }
        return 0;
    }
}