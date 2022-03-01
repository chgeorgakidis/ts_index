package main.java.query;

import main.java.tools.ElementDistance;
import main.java.tools.FunctionsTSIndex;
import main.java.tools.GlobalConfTSIndex;
import main.java.tools.NodeDistance;
import main.java.tsindex.Node;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.PriorityQueue;

public class TwinSubsequenceTopKDFT {

    public void execute(Node node, float[] querySubsequence) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File(GlobalConfTSIndex.dataFile), "r");
        kNearestTraversal(node, querySubsequence, raf);
    }

    private void kNearestTraversal(Node node, float[] querySubsequence, RandomAccessFile raf) throws IOException {
        if (node.leaf) {
            GlobalConfTSIndex.passedLeafNodes++;
            for (float[] interval : node.intervals) {
                for (int i = (int) interval[0]; i <= interval[1]; i++) {
                    GlobalConfTSIndex.totalSubsequencesChecked++;
                    byte[] b = new byte[4 * GlobalConfTSIndex.defaultWindowSize];
                    raf.seek(i * 4);
                    raf.readFully(b);
                    float[] currSub = FunctionsTSIndex.toFloatArray(b);
                    boolean broken = false;

                    if (GlobalConfTSIndex.resultIndicesTopK.size() < GlobalConfTSIndex.k) {
                        double chebDist = calculateChebDistElement(querySubsequence, currSub);
                        GlobalConfTSIndex.resultIndicesTopK.add(new ElementDistance((float) i, chebDist));
                    } else {
                        double chebDist = 0.0;
                        double thresholdDistance = GlobalConfTSIndex.resultIndicesTopK.peekLast().distance;
                        for (int j = 0; j < GlobalConfTSIndex.defaultWindowSize; j++) {
                            GlobalConfTSIndex.checkCounter++;
                            double currChebDist = Math.abs(currSub[j] - querySubsequence[j]);
                            if (currChebDist > thresholdDistance) {
                                broken = true;
                                break;
                            }
                            if (currChebDist > chebDist) {
                                chebDist = currChebDist;
                            }
                        }
                        if (broken) {
                            continue;
                        } else {
                            GlobalConfTSIndex.resultIndicesTopK.add(new ElementDistance((float) i, chebDist));
                        }
                    }
                }
            }
        } else {
            GlobalConfTSIndex.passedInnerNodes++;
            PriorityQueue<NodeDistance> activeBranchList = new PriorityQueue<>();
            for (Node c : node.children) {
                if (GlobalConfTSIndex.resultIndicesTopK.size() < GlobalConfTSIndex.k) {
                    double chebDist = calculateChebDistNode(querySubsequence, c);
                    activeBranchList.add(new NodeDistance(c, chebDist));
                } else {
                    double chebDist = 0.0;
                    boolean broken = false;
                    double thresholdDistance = GlobalConfTSIndex.resultIndicesTopK.peekLast().distance;
                    for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
                        GlobalConfTSIndex.checkCounter++;
                        if (GlobalConfTSIndex.queryPAA[i] > c.getMBTShi()[i]) {
                            double currChebDist = Math.abs(GlobalConfTSIndex.queryPAA[i] - c.getMBTShi()[i]);
                            if (currChebDist > thresholdDistance) {
                                broken = true;
                                break;
                            }
                            if (currChebDist > chebDist) {
                                chebDist = currChebDist;
                            }
                        } else if (GlobalConfTSIndex.queryPAA[i] < c.getMBTSlo()[i]) {
                            double currChebDist = Math.abs(c.getMBTSlo()[i] - GlobalConfTSIndex.queryPAA[i]);
                            if (currChebDist > thresholdDistance) {
                                broken = true;
                                break;
                            }
                            if (currChebDist > chebDist) {
                                chebDist = currChebDist;
                            }
                        }
                    }

                    if (broken) {
                        if (c.leaf)
                            GlobalConfTSIndex.prunedLeafNodes++;
                        else
                            GlobalConfTSIndex.prunedInnerNodes++;
                        continue;
                    } else {
                        activeBranchList.add(new NodeDistance(c, chebDist));
                    }
                }
            }
            for (NodeDistance nd : activeBranchList) {
                double thresholdDistance = Double.MAX_VALUE;
                if (GlobalConfTSIndex.resultIndicesTopK.size() == GlobalConfTSIndex.k)
                    thresholdDistance = GlobalConfTSIndex.resultIndicesTopK.peekLast().distance;
                if (nd.distance < thresholdDistance) {
                    kNearestTraversal(nd.n, querySubsequence, raf);
                }
            }
        }
    }

    private double calculateChebDistElement(float[] querySubsequence, float[] currSub) {
        double chebDist = 0.0;
        for (int j = 0; j < GlobalConfTSIndex.defaultWindowSize; j++) {
            GlobalConfTSIndex.checkCounter++;
            double currChebDist = Math.abs(currSub[j] - querySubsequence[j]);
            if (currChebDist > chebDist) {
                chebDist = currChebDist;
            }
        }
        return chebDist;
    }

    private double calculateChebDistNode(float[] querySubsequence, Node c) {
        double chebDist = 0.0;
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            GlobalConfTSIndex.checkCounter++;
            if (GlobalConfTSIndex.queryPAA[i] > c.getMBTShi()[i]) {
                double currChebDist = Math.abs(GlobalConfTSIndex.queryPAA[i] - c.getMBTShi()[i]);
                if (currChebDist > chebDist) {
                    chebDist = currChebDist;
                }
            } else if (GlobalConfTSIndex.queryPAA[i] < c.getMBTSlo()[i]) {
                double currChebDist = Math.abs(c.getMBTSlo()[i] - GlobalConfTSIndex.queryPAA[i]);
                if (currChebDist > chebDist) {
                    chebDist = currChebDist;
                }
            }
        }
        return chebDist;
    }
}