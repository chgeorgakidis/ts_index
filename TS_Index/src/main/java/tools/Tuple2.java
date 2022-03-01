package main.java.tools;

import java.io.Serializable;

public class Tuple2<X, Y> implements Serializable {
    public X x;
    public Y y;

    public Tuple2(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public Tuple2() {
        x = null;
        y = null;
    }
}