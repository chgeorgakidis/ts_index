package main.java.tools;

import java.io.Serializable;

public class Tuple3<X, Y, Z> implements Serializable {
    public final X x;
    public final Y y;
    public final Z z;

    public Tuple3(X x, Y y, Z z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Tuple3() {
        x = null;
        y = null;
        z = null;
    }
}