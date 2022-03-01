package main.java.tsindex;

import main.java.tools.GlobalConfTSIndex;

import java.io.Serializable;
import java.util.ArrayList;

public class Node implements Serializable {
    public final boolean leaf;
    public int uniqueId;
    public ArrayList<Node> children;
    public Float[] data;
    public ArrayList<float[]> intervals;
    public float[] MBTSlo;
    public float[] MBTShi;

    public Node(boolean leaf) {
        this.uniqueId = GlobalConfTSIndex.indexIDs++;
        resetMBTSData();
        if (!leaf)
            this.children = new ArrayList<>();
        this.leaf = leaf;
        this.uniqueId = ++GlobalConfTSIndex.nodeId;
    }

    public Node() {
        this.uniqueId = GlobalConfTSIndex.indexIDs++;
        leaf = false;
    }

    public void resetMBTSData() {
        this.MBTShi = new float[GlobalConfTSIndex.numSeg];
        this.MBTSlo = new float[GlobalConfTSIndex.numSeg];
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            this.MBTShi[i] = Float.MIN_VALUE;
            this.MBTSlo[i] = Float.MAX_VALUE;
        }
    }

    public void calculateIntervals() {
        for (Node c : this.children) {
            if (c.isLeaf()) {
                c.intervals = new ArrayList<>();
                float first = c.data[0];
                float[] interval = new float[]{first, first};
                c.intervals.add(interval);
                for (int i=0; i<c.data.length; i++) {
                    float id = c.data[i];
                    if (id == first) continue;
                    boolean found = false;
                    for (float[] inter : c.intervals) {
                        if (id > inter[1]) {
                            if (id - inter[1] == 1) {
                                inter[1] = id;
                                found = true;
                                break;
                            }
                        } else {
                            if (inter[0] - id == 1) {
                                inter[0] = id;
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found) {
                        interval = new float[]{id, id};
                        c.intervals.add(interval);
                    }
                }
                c.children = null;
                c.data = null;
            } else {
                c.calculateIntervals();
            }
        }
    }

    public boolean isLeaf() {
        return leaf;
    }

    public float[] getMBTShi() {
        return MBTShi;
    }

    public void setMBTShi(float[] MBTShi) {
        this.MBTShi = MBTShi;
    }

    public float[] getMBTSlo() {
        return MBTSlo;
    }

    public void setMBTSlo(float[] MBTSlo) {
        this.MBTSlo = MBTSlo;
    }

}