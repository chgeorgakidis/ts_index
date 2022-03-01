package main.java.tsindex;

import main.java.tools.*;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class TS_Index_Bulk<T> implements Serializable {

    private Node root;

    public TS_Index_Bulk() {}

    public void build(TreeMap<String, ArrayList<float[]>> elements) {
        List<String> keys = new ArrayList<>(elements.keySet());
        BigInteger first = new BigInteger(keys.get(0));
        BigInteger last = new BigInteger(keys.get(keys.size()-1));
        BigInteger diff = last.subtract(first);
        BigInteger elSize = new BigInteger(String.valueOf(elements.size()));
        BigInteger stepPerElement = diff.divide(elSize);
        BigInteger meanNodeSize = new BigInteger(String.valueOf((GlobalConfTSIndex.M- GlobalConfTSIndex.m)/2));
        BigInteger stepPerElements = meanNodeSize.multiply(stepPerElement);

        int count2 = 0;
        int count = 0;
        ArrayList<Node> leafNodes = new ArrayList<>(GlobalConfTSIndex.bigSequence.length/ GlobalConfTSIndex.M);
        Node n = new Node(true);

        float[] MBTShi = new float[GlobalConfTSIndex.numSeg];
        float[] MBTSlo = new float[GlobalConfTSIndex.numSeg];
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            MBTShi[i] = -1.0f * Float.MAX_VALUE;
            MBTSlo[i] = Float.MAX_VALUE;
        }
        ArrayList<Float> tmpArray = new ArrayList<>();


        //ArrayList<float[]>[] values = elements.values().toArray(new ArrayList[elements.values().size()]);
        //for (int k=0; k<values.length; k++) {
        BigInteger currZCode = null;
        //BigInteger prevZCode = null;
        BigInteger avgZCode = null;
        BigInteger zCodeDiff = null;
        int keyIndex = 0;
        boolean newNode = true;
        for (ArrayList<float[]> PAAs : elements.values()) {
            //ArrayList<float[]> PAAs = values[k];
            currZCode = new BigInteger(keys.get(keyIndex));
            if (keyIndex > 0) {
                zCodeDiff = currZCode.subtract(avgZCode);
            }
            if (newNode) {
                zCodeDiff = new BigInteger(String.valueOf(0));
                avgZCode = currZCode;
                newNode = false;
            }
            float[] PAA;
            for (int i=0; i<PAAs.size(); i++) {
                PAA = PAAs.get(i);
                if ((count < GlobalConfTSIndex.m) || ((count >= GlobalConfTSIndex.m) && (count < GlobalConfTSIndex.M) && (zCodeDiff.compareTo(stepPerElements) != 1))) {
                    tmpArray.add(PAA[GlobalConfTSIndex.numSeg]);
                    for (int j = 0; j < GlobalConfTSIndex.numSeg; j++) {
                        float tsVal = PAA[j];
                        if (tsVal < MBTSlo[j]) {
                            MBTSlo[j] = tsVal;
                        }
                        if (tsVal > MBTShi[j]) {
                            MBTShi[j] = tsVal;
                        }
                    }
                } else {
                    newNode = true;
                    System.arraycopy(MBTSlo, 0, n.MBTSlo, 0, GlobalConfTSIndex.numSeg);
                    System.arraycopy(MBTShi, 0, n.MBTShi, 0, GlobalConfTSIndex.numSeg);
                    n.data = tmpArray.toArray(new Float[0]);
                    leafNodes.add(n);

                    n = new Node(true);
                    MBTShi = new float[GlobalConfTSIndex.numSeg];
                    MBTSlo = new float[GlobalConfTSIndex.numSeg];
                    for (int j = 0; j < GlobalConfTSIndex.numSeg; j++) {
                        MBTShi[j] = -1.0f * Float.MAX_VALUE;
                        MBTSlo[j] = Float.MAX_VALUE;
                    }
                    for (int j = 0; j < GlobalConfTSIndex.numSeg; j++) {
                        float tsVal = PAA[j];
                        if (tsVal < MBTSlo[j]) {
                            MBTSlo[j] = tsVal;
                        }
                        if (tsVal > MBTShi[j]) {
                            MBTShi[j] = tsVal;
                        }
                    }
                    tmpArray = new ArrayList<>();
                    tmpArray.add(PAA[GlobalConfTSIndex.numSeg]);
                    count = 1;

                    continue;
                }
                count++;
                if (count2++ % 100000 == 0) {
                    System.out.println(count2);
                }
            }
            //prevZCode = currZCode;
            keyIndex++;
            avgZCode = (avgZCode.multiply(BigInteger.valueOf(keyIndex-1)).add(currZCode)).divide(BigInteger.valueOf(keyIndex));
        }
        System.arraycopy(MBTSlo, 0, n.MBTSlo, 0, GlobalConfTSIndex.numSeg);
        System.arraycopy(MBTShi, 0, n.MBTShi, 0, GlobalConfTSIndex.numSeg);
        n.data = new Float[count];
        System.arraycopy(tmpArray.toArray(new Float[0]), 0, n.data, 0, count);
        leafNodes.add(n);
        //tighten(leafNodes);
        buildInnerNodes(leafNodes);
    }

    private void buildInnerNodes(ArrayList<Node> nodes) {
        int count = 0;
        ArrayList<Node> nextLevelNodes = new ArrayList<>();
        Node n = new Node(false);

        float[] MBTShi = new float[GlobalConfTSIndex.numSeg];
        float[] MBTSlo = new float[GlobalConfTSIndex.numSeg];
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            MBTShi[i] = -1.0f * Float.MAX_VALUE;
            MBTSlo[i] = Float.MAX_VALUE;
        }

        for (Node node : nodes) {
            if (count < GlobalConfTSIndex.M) {
                n.children.add(node);

                for (int j = 0; j < GlobalConfTSIndex.numSeg; j++) {
                    float lo = node.getMBTSlo()[j];
                    float hi = node.getMBTShi()[j];
                    if (lo < MBTSlo[j]) {
                        MBTSlo[j] = lo;
                    }
                    if (hi > MBTShi[j]) {
                        MBTShi[j] = hi;
                    }
                }

            } else {

                System.arraycopy(MBTSlo, 0, n.MBTSlo, 0, GlobalConfTSIndex.numSeg);
                System.arraycopy(MBTShi, 0, n.MBTShi, 0, GlobalConfTSIndex.numSeg);

                nextLevelNodes.add(n);
                n = new Node(false);

                MBTShi = new float[GlobalConfTSIndex.numSeg];
                MBTSlo = new float[GlobalConfTSIndex.numSeg];
                for (int j = 0; j < GlobalConfTSIndex.numSeg; j++) {
                    MBTShi[j] = -1.0f * Float.MAX_VALUE;
                    MBTSlo[j] = Float.MAX_VALUE;
                }

                for (int j = 0; j < GlobalConfTSIndex.numSeg; j++) {
                    float lo = node.getMBTSlo()[j];
                    float hi = node.getMBTShi()[j];
                    if (lo < MBTSlo[j]) {
                        MBTSlo[j] = lo;
                    }
                    if (hi > MBTShi[j]) {
                        MBTShi[j] = hi;
                    }
                }

                n.children.add(node);
                count = 1;
            }
            count++;
        }
        System.arraycopy(MBTSlo, 0, n.MBTSlo, 0, GlobalConfTSIndex.numSeg);
        System.arraycopy(MBTShi, 0, n.MBTShi, 0, GlobalConfTSIndex.numSeg);
        nextLevelNodes.add(n);
        //tighten(nextLevelNodes);
        if (nextLevelNodes.size() > 1) {
            buildInnerNodes(nextLevelNodes);
        } else {
            this.root = n;
        }
    }

    /*private void tighten(ArrayList<Node> nodes) {
        assert (nodes.size() >= 1) : "Pass some nodes to tighten!";
        for (Node n : nodes) {
            assert (n.children.size() > 0) : "tighten() called on empty node!";
            float[] MBTShi = new float[GlobalConf.numSeg];
            float[] MBTSlo = new float[GlobalConf.numSeg];
            for (int i = 0; i < GlobalConf.numSeg; i++) {
                MBTShi[i] = -1.0f * Float.MAX_VALUE;
                MBTSlo[i] = Float.MAX_VALUE;

                if (!n.isLeaf()) {
                    for (int j = 0; j < n.children.size(); j++) {
                        Node c = n.children.get(j);
                        float lo = c.getMBTSlo()[i];
                        float hi = c.getMBTShi()[i];
                        c.parent = n;
                        if (lo < MBTSlo[i]) {
                            MBTSlo[i] = lo;
                        }
                        if (hi > MBTShi[i]) {
                            MBTShi[i] = hi;
                        }
                    }
                } else {
                    for (int j = 0; j < n.children.size(); j++) {
                        Node e = n.children.get(j);
                        TimeSeries ts = ((TimeSeries) ((Entry) e).getEntry());
                        //float tsVal = ts.getTimeSeries()[i] / GlobalConf.scale;
                        float tsVal = ts.getTimeSeries()[i];
                        e.parent = n;
                        if (tsVal < MBTSlo[i]) {
                            MBTSlo[i] = tsVal;
                        }
                        if (tsVal > MBTShi[i]) {
                            MBTShi[i] = tsVal;
                        }
                    }
                }
            }
            System.arraycopy(MBTSlo, 0, n.MBTSlo, 0, GlobalConf.numSeg);
            System.arraycopy(MBTShi, 0, n.MBTShi, 0, GlobalConf.numSeg);
        }
    }*/

    public Node getRoot() {
        return root;
    }
}