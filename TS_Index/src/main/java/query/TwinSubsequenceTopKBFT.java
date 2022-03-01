package main.java.query;

import main.java.tools.ElementDistance;
import main.java.tools.NodeDistance;
import main.java.tsindex.Node;
import main.java.tools.FunctionsTSIndex;
import main.java.tools.GlobalConfTSIndex;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.PriorityQueue;

public class TwinSubsequenceTopKBFT {

    public void execute(Node node, float[] querySubsequence) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File(GlobalConfTSIndex.dataFile), "r");

        PriorityQueue<NodeDistance> queue = new PriorityQueue<>();
        queue.add(new NodeDistance(node, 0));

        //fillTopKList(node, querySubsequence, raf);

        while (queue.isEmpty() == false) {
            NodeDistance nd = queue.poll();
            if (GlobalConfTSIndex.resultIndicesTopK.size() == GlobalConfTSIndex.k) {
                double thresholdDistance = GlobalConfTSIndex.resultIndicesTopK.peekLast().distance;
                if (nd.distance > thresholdDistance)
                    break;
            }

            Node n = nd.n;
            if (n.leaf) {
                GlobalConfTSIndex.passedLeafNodes++;
                for (float[] interval : n.intervals) {
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
                for (Node c : n.children) {
                    if (GlobalConfTSIndex.resultIndicesTopK.size() < GlobalConfTSIndex.k) {
                        double chebDist = calculateChebDistNode(querySubsequence, c);
                        queue.add(new NodeDistance(c, chebDist));
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
                            queue.add(new NodeDistance(c, chebDist));
                        }
                    }
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

    private void fillTopKList(Node node, float[] querySubsequence, RandomAccessFile raf) throws IOException {
        if (node.leaf) {
            GlobalConfTSIndex.passedLeafNodes++;
            for (float[] interval : node.intervals) {
                for (int i = (int) interval[0]; i <= interval[1]; i++) {
                    GlobalConfTSIndex.totalSubsequencesChecked++;
                    byte[] b = new byte[4 * GlobalConfTSIndex.defaultWindowSize];
                    raf.seek(i * 4);
                    raf.readFully(b);
                    float[] currSub = FunctionsTSIndex.toFloatArray(b);

                    if (GlobalConfTSIndex.resultIndicesTopK.size() < GlobalConfTSIndex.k) {
                        double chebDist = calculateChebDistElement(querySubsequence, currSub);
                        GlobalConfTSIndex.resultIndicesTopK.add(new ElementDistance((float) i, chebDist));
                    } else {
                        return;
                    }
                }
            }

        } else {
            GlobalConfTSIndex.passedInnerNodes++;
            PriorityQueue<NodeDistance> activeBranchList = new PriorityQueue<>();
            for (Node c : node.children) {
                double chebDist = calculateChebDistNode(querySubsequence, c);
                activeBranchList.add(new NodeDistance(c, chebDist));
            }
            for (NodeDistance nd : activeBranchList) {
                if (nd.distance < GlobalConfTSIndex.kThreshold) {
                    fillTopKList(nd.n, querySubsequence, raf);
                } else {
                    break;
                }
            }
        }
    }
}