package mainapp;

import com.google.common.collect.MinMaxPriorityQueue;
import main.java.query.TwinSubsequenceSearch;
import main.java.query.TwinSubsequenceTopKDFT;
import main.java.tools.DistComparator;
import main.java.tools.FunctionsTSIndex;
import main.java.tools.GlobalConfTSIndex;
import main.java.tsindex.TS_Index_Bulk;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RunTSIndex {

    public static void main(String[] args) throws Exception {

        GlobalConfTSIndex.queryType = Integer.parseInt(args[0]);
        if (GlobalConfTSIndex.queryType == 0) {
            GlobalConfTSIndex.epsilon = Float.parseFloat(args[1]);
        } else {
            GlobalConfTSIndex.k = Integer.parseInt(args[1]);
        }
        GlobalConfTSIndex.m = Integer.parseInt(args[2]);
        GlobalConfTSIndex.M = Integer.parseInt(args[3]);
        GlobalConfTSIndex.numSeg = Integer.parseInt(args[4]);
        GlobalConfTSIndex.noQueries = Integer.parseInt(args[5]);
        GlobalConfTSIndex.defaultWindowSize = Integer.parseInt(args[6]);
        GlobalConfTSIndex.dataPath = args[7];
        GlobalConfTSIndex.indexStorePath = args[8];
        GlobalConfTSIndex.resultsPath = args[9];

        long startExecuteTime;
        long totalElapsedExecuteTime;
        int executeMillis, executeSeconds, executeMinutes, executeHours;
        TS_Index_Bulk TSindex = null;

        File data = new File(GlobalConfTSIndex.dataPath);
        String contents[] = data.list();

            for (String filename : contents) {
                if (filename.contains("DS_Store"))
                    continue;
                PrintWriter resultsWriter = new PrintWriter(GlobalConfTSIndex.resultsPath + filename);
                System.out.println("Building for " + filename + "...");
                parseInputData(filename);

                FunctionsTSIndex.readAllData();
                TSindex = new TS_Index_Bulk<>();
                startExecuteTime = System.currentTimeMillis();
                FunctionsTSIndex.buildIndex(TSindex);
                totalElapsedExecuteTime = System.currentTimeMillis() - startExecuteTime;
                executeMillis = (int) totalElapsedExecuteTime % 1000;
                executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
                executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
                executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);
                resultsWriter.println("######### INDEX BUILD TIME STATS #########");
                resultsWriter.println("Index Build time: " + executeHours + "h " + executeMinutes + "m " + executeSeconds + "sec " + executeMillis + "mil");

                TwinSubsequenceSearch runQuery1 = null;
                TwinSubsequenceTopKDFT runQuery2 = null;
                if (GlobalConfTSIndex.queryType == 0) {
                    runQuery1 = new TwinSubsequenceSearch();
                    GlobalConfTSIndex.resultIndicesRange = new LinkedList<>();
                } else if (GlobalConfTSIndex.queryType == 1) {
                    runQuery2 = new TwinSubsequenceTopKDFT();
                    GlobalConfTSIndex.resultIndicesTopK = MinMaxPriorityQueue.orderedBy(new DistComparator()).maximumSize(GlobalConfTSIndex.k).create();
                }

                totalElapsedExecuteTime = 0;
                if (GlobalConfTSIndex.queryType == 0) {
                    for (int j = 0; j < GlobalConfTSIndex.noQueries; j++) {
                        GlobalConfTSIndex.startValue = randomGenerator(j);
                        FunctionsTSIndex.readQuery(GlobalConfTSIndex.zNormalize);
                        GlobalConfTSIndex.queryPAA = FunctionsTSIndex.getPAA(GlobalConfTSIndex.querySubSeq);
                        startExecuteTime = System.currentTimeMillis();
                        runQuery1.execute(TSindex.getRoot(), GlobalConfTSIndex.querySubSeq, GlobalConfTSIndex.epsilon);
                        totalElapsedExecuteTime += System.currentTimeMillis() - startExecuteTime;
                    }
                }
                else if (GlobalConfTSIndex.queryType == 1) {
                    for (int j = 0; j < GlobalConfTSIndex.noQueries; j++) {
                        GlobalConfTSIndex.startValue = randomGenerator(j);
                        FunctionsTSIndex.readQuery(GlobalConfTSIndex.zNormalize);
                        GlobalConfTSIndex.queryPAA = FunctionsTSIndex.getPAA(GlobalConfTSIndex.querySubSeq);
                        startExecuteTime = System.currentTimeMillis();
                        runQuery2.execute(TSindex.getRoot(), GlobalConfTSIndex.querySubSeq);
                        totalElapsedExecuteTime += System.currentTimeMillis() - startExecuteTime;
                    }
                }


                executeMillis = (int) totalElapsedExecuteTime % 1000;
                executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
                executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
                executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);

                FunctionsTSIndex.countNodes(TSindex.getRoot());
                GlobalConfTSIndex.leafBoundDiffs = new ArrayList<>();
                GlobalConfTSIndex.innerBoundDiffs = new ArrayList<>();
                FunctionsTSIndex.calculateBoundsDiffs(TSindex.getRoot());
                double avgDiffLeaves = FunctionsTSIndex.mean(GlobalConfTSIndex.leafBoundDiffs);
                double avgDiffInner = FunctionsTSIndex.mean(GlobalConfTSIndex.innerBoundDiffs);
                double stdvDiffLeaves = FunctionsTSIndex.stdDev(GlobalConfTSIndex.leafBoundDiffs);
                double stdvDiffInner = FunctionsTSIndex.stdDev(GlobalConfTSIndex.innerBoundDiffs);
                double maxDiffLeaves = FunctionsTSIndex.getMaxDiff(GlobalConfTSIndex.leafBoundDiffs);
                double maxDiffInner = FunctionsTSIndex.getMaxDiff(GlobalConfTSIndex.innerBoundDiffs);
                double minDiffLeaves = FunctionsTSIndex.getMinDiff(GlobalConfTSIndex.leafBoundDiffs);
                double minDiffInner = FunctionsTSIndex.getMinDiff(GlobalConfTSIndex.innerBoundDiffs);
                FunctionsTSIndex.save(TSindex);

                resultsWriter.println("\n######### INDEX SIZE STATS #########");
                Path filePath = Paths.get(GlobalConfTSIndex.indexStorePath + "/TS_index.dr");
                FileChannel fileChannel;
                try {
                    fileChannel = FileChannel.open(filePath);
                    long fileSize = fileChannel.size();
                    resultsWriter.println("Index Size: " + Math.round(((double) fileSize / (1024 * 1024)) * 100.0) / 100.0 + "MB");
                    fileChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                resultsWriter.println("\n######### QUERY RESULTS STATS #########");
                resultsWriter.println("Total time: " + executeHours + "h " + executeMinutes + "m " + executeSeconds + "sec " + executeMillis + "mil");
                if (GlobalConfTSIndex.queryType == 0) {
                    resultsWriter.println("Total number of results: " + GlobalConfTSIndex.resultIndicesRange.size());
                } else if (GlobalConfTSIndex.queryType == 1) {
                    resultsWriter.println("Total number of results: " + GlobalConfTSIndex.resultIndicesTopK.size());
                }
                resultsWriter.println("Total number of checks: " + GlobalConfTSIndex.checkCounter);

                resultsWriter.println("\n######### NODE PRUNING STATS #########");
                resultsWriter.println("Inner nodes pruned: " + (GlobalConfTSIndex.prunedInnerNodes / (GlobalConfTSIndex.noQueries)) + "/" + GlobalConfTSIndex.totalInnerNodes);
                resultsWriter.println("Inner nodes accepted: " + (GlobalConfTSIndex.passedInnerNodes / (GlobalConfTSIndex.noQueries)) + "/" + GlobalConfTSIndex.totalInnerNodes);
                resultsWriter.println("Leaf nodes pruned: " + (GlobalConfTSIndex.prunedLeafNodes / (GlobalConfTSIndex.noQueries)) + "/" + GlobalConfTSIndex.totalLeafNodes);
                resultsWriter.println("Leaf nodes accepted: " + (GlobalConfTSIndex.passedLeafNodes / (GlobalConfTSIndex.noQueries)) + "/" + GlobalConfTSIndex.totalLeafNodes);
                resultsWriter.println("Total Nodes pruned: " + (GlobalConfTSIndex.totalPrunedNodes / (GlobalConfTSIndex.noQueries)) + "/" + (GlobalConfTSIndex.totalInnerNodes + GlobalConfTSIndex.totalLeafNodes));

                resultsWriter.println("\n######### NODE BOUNDS STATS #########");
                resultsWriter.println("Average inner node bounds difference: " + avgDiffInner);
                resultsWriter.println("Average leaf node bounds difference: " + avgDiffLeaves);
                resultsWriter.println("Standard deviation of inner node bounds differences: " + stdvDiffInner);
                resultsWriter.println("Standard deviation of leaf node bounds differences: " + stdvDiffLeaves);
                resultsWriter.println("Maximum inner node bounds difference: " + maxDiffInner);
                resultsWriter.println("Maximum leaf node bounds difference: " + maxDiffLeaves);
                resultsWriter.println("Minimum inner node bounds difference: " + minDiffInner);
                resultsWriter.println("Minimum leaf node bounds difference: " + minDiffLeaves);
                resultsWriter.println("Total raw subsequences checked: " + GlobalConfTSIndex.totalSubsequencesChecked);
                resultsWriter.close();
            }
    }

    private static int randomGenerator(int seed) {
        Random generator = new Random(seed);
        int random_int = generator.nextInt(0, GlobalConfTSIndex.tsLength - GlobalConfTSIndex.defaultWindowSize + 1);
        return random_int;
    }

    private static void parseInputData(String filename) throws Exception {
        FunctionsTSIndex.parseData(filename);
    }
}
