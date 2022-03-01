package main.java.query;

import main.java.tsindex.Node;
import main.java.tools.FunctionsTSIndex;
import main.java.tools.GlobalConfTSIndex;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;

public class TwinSubsequenceSearch {

    public void execute(Node node, float[] querySubsequence, float epsilon) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File(GlobalConfTSIndex.dataFile), "r");
        for (Node c : node.children) {
            if (c.isLeaf()) {
                if (!isOutOfBounds(GlobalConfTSIndex.queryPAA, epsilon, c)) {
                    GlobalConfTSIndex.passedLeafNodes++;
                    for (float[] interval : c.intervals) {
                        //System.out.println("Interval " + interval[0] + "," + interval[1]);
                        for (int i = (int) interval[0]; i <= interval[1]; i++) {
                            //System.out.println("Read from disk " + GlobalConf.readFromDisk++ + " times!");
                            //System.out.println(i);
                            GlobalConfTSIndex.totalSubsequencesChecked++;
                            byte[] b = new byte[4 * GlobalConfTSIndex.defaultWindowSize];
                            raf.seek((long) (i * 4));
                            raf.readFully(b);
                            float[] currSub = FunctionsTSIndex.toFloatArray(b);
                            boolean broken = false;

                            if (GlobalConfTSIndex.zNormalize == 0) {
                                for (int j = 0; j < GlobalConfTSIndex.defaultWindowSize; j++) {
                                    GlobalConfTSIndex.checkCounter++;
                                    if (Math.abs(currSub[j] - querySubsequence[j]) > epsilon) {
                                        broken = true;
                                        break;
                                    }
                                }
                            } else {
                                for (int j = 0; j < GlobalConfTSIndex.defaultWindowSize; j++) {
                                    GlobalConfTSIndex.checkCounter++;
                                    if (Math.abs(currSub[GlobalConfTSIndex.queryCheckSeq[j]] - querySubsequence[GlobalConfTSIndex.queryCheckSeq[j]]) > epsilon) {
                                        broken = true;
                                        break;
                                    }
                                }
                            }

                            if (broken)
                                continue;
                            else {
                                GlobalConfTSIndex.resultIndicesRange.add((float) i);
                            }
                        }
                    }
                } else {
                    GlobalConfTSIndex.prunedLeafNodes++;
                }
            } else {
                if (!isOutOfBounds(GlobalConfTSIndex.queryPAA, epsilon, c)) {
                    GlobalConfTSIndex.passedInnerNodes++;
                    execute(c, querySubsequence, epsilon);
                } else {
                    GlobalConfTSIndex.prunedInnerNodes++;
                    int pruned = 0;
                    pruned = FunctionsTSIndex.countPrunedSubNodes(c, pruned);
                    GlobalConfTSIndex.totalPrunedNodes += pruned;
                }
            }
        }

        raf.close();
    }

    public void executeVariable(Node node, float[] querySubsequence, float epsilon, Set<Float> currResults, Set<Float> prevResults, int step) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(new File(GlobalConfTSIndex.dataFile), "r");
        for (Node c : node.children) {
            if (c.isLeaf()) {
                if (!isOutOfBounds(GlobalConfTSIndex.queryPAA, epsilon, c)) {
                    GlobalConfTSIndex.passedLeafNodes++;
                    for (float[] interval : c.intervals) {
                        for (int i = (int) interval[0]; i <= interval[1]; i++) {
                            GlobalConfTSIndex.totalSubsequencesChecked++;
                            byte[] b = new byte[4 * GlobalConfTSIndex.defaultWindowSize];
                            raf.seek(i * 4);
                            try {
                                raf.readFully(b);
                            } catch(Exception e) {
                                System.out.println();
                                continue;
                            }
                            float[] currSub = FunctionsTSIndex.toFloatArray(b);
                            boolean broken = false;

                            if (GlobalConfTSIndex.zNormalize == 0) {
                                for (int j = 0; j < GlobalConfTSIndex.defaultWindowSize; j++) {
                                    GlobalConfTSIndex.checkCounter++;
                                    if (Math.abs(currSub[j] - querySubsequence[j]) > epsilon) {
                                        broken = true;
                                        break;
                                    }
                                }
                            } else {
                                for (int j = 0; j < GlobalConfTSIndex.defaultWindowSize; j++) {
                                    GlobalConfTSIndex.checkCounter++;
                                    if (Math.abs(currSub[j] - querySubsequence[j]) > epsilon) {
                                        broken = true;
                                        break;
                                    }
                                }
                            }

                            if (broken)
                                continue;
                            else {
                                if (prevResults.size() > 0) {
                                    if (prevResults.contains((float) i-step))
                                        currResults.add((float) (i-step));
                                } else {
                                    currResults.add((float) i);
                                }
                            }
                        }
                    }
                } else {
                    GlobalConfTSIndex.prunedLeafNodes++;
                }
            } else {
                if (!isOutOfBounds(GlobalConfTSIndex.queryPAA, epsilon, c)) {
                    GlobalConfTSIndex.passedInnerNodes++;
                    executeVariable(c, querySubsequence, epsilon, currResults, prevResults, step);
                } else {
                    GlobalConfTSIndex.prunedInnerNodes++;
                    int pruned = 0;
                    pruned = FunctionsTSIndex.countPrunedSubNodes(c, pruned);
                    GlobalConfTSIndex.totalPrunedNodes += pruned;
                }
            }
        }

        raf.close();
    }

    private boolean isOutOfBounds(float[] querySubsequence, float epsilon, Node c) {
        boolean broken = false;
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            GlobalConfTSIndex.checkCounter++;
            if (querySubsequence[i] > c.getMBTShi()[i]) {
                if (querySubsequence[i] - c.getMBTShi()[i] > epsilon) {
                    broken = true;
                    break;
                }
            } else if (querySubsequence[i] < c.getMBTSlo()[i]) {
                if (c.getMBTSlo()[i] - querySubsequence[i] > epsilon) {
                    broken = true;
                    break;
                }
            }
        }
        return broken;
    }
}