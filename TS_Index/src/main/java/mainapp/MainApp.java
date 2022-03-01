package main.java.mainapp;

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

public class MainApp {

    public static void main(String[] args) throws Exception {

        long startExecuteTime;
        long totalElapsedExecuteTime;
        int executeMillis, executeSeconds, executeMinutes, executeHours;
        TS_Index_Bulk TSindex = null;

        File dataPath = new File(GlobalConfTSIndex.dataPath);
        String contents[] = dataPath.list();
        if (GlobalConfTSIndex.variableLengthQuery == false) {
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
        } else { // Variable length query

            TwinSubsequenceSearch runQuery1 = null;
            TwinSubsequenceTopKDFT runQuery2 = null;
            if (GlobalConfTSIndex.queryType == 0) {
                runQuery1 = new TwinSubsequenceSearch();
                GlobalConfTSIndex.resultIndicesRange = new LinkedList<>();
            } else if (GlobalConfTSIndex.queryType == 1) {
                runQuery2 = new TwinSubsequenceTopKDFT();
                GlobalConfTSIndex.resultIndicesTopK = MinMaxPriorityQueue.orderedBy(new DistComparator()).maximumSize(GlobalConfTSIndex.k).create();
            }

            for (int length : GlobalConfTSIndex.queryLengths) {
                GlobalConfTSIndex.queryLength = length;

                totalElapsedExecuteTime = 0;
                Set<Float> totalResults = new HashSet<>();
                for (int i = 0; i < GlobalConfTSIndex.noQueries; i++) {
                    GlobalConfTSIndex.startValue = 1000 + (i);
                    FunctionsTSIndex.readQuery(0);
                    startExecuteTime = System.currentTimeMillis();
                    if (GlobalConfTSIndex.queryType == 1) {

                        int step = 0;
                        Set<Float> prevResults = new HashSet<>();
                        Set<Float> currResults = null;
                        for (int l = 0; l < GlobalConfTSIndex.queryLength; l+= GlobalConfTSIndex.defaultWindowSize) {
                            currResults = new HashSet<>();
                            float[] currQuery = new float[GlobalConfTSIndex.defaultWindowSize];
                            System.arraycopy(GlobalConfTSIndex.querySubSeq, l, currQuery, 0, GlobalConfTSIndex.defaultWindowSize);
                            if (GlobalConfTSIndex.zNormalize == 1) {
                                currQuery = FunctionsTSIndex.zNormalization(currQuery);
                            }
                            GlobalConfTSIndex.queryPAA = FunctionsTSIndex.getPAA(currQuery);
                            runQuery1.executeVariable(TSindex.getRoot(), currQuery, GlobalConfTSIndex.epsilon, currResults, prevResults, step);
                            if (currResults.size() == 0)
                                break;
                            prevResults = (Set<Float>) ((HashSet<Float>) currResults).clone();
                            step++;
                        }
                        if (currResults.size() > 0) {
                            totalResults.addAll(currResults);
                        }

                    } else if (GlobalConfTSIndex.queryType == 2) {
                        System.out.println("kNN for variable length query not implemented!");
                        System.exit(0);
                    }
                    totalElapsedExecuteTime += System.currentTimeMillis() - startExecuteTime;
                }

                //Collections.sort(GlobalConf.resultIndicesSearch);
                executeMillis = (int) totalElapsedExecuteTime % 1000;
                executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
                executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
                executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);

                System.out.println("\n######### QUERY RESULTS STATS #########");
                System.out.println("Total time: " + executeHours + "h " + executeMinutes + "m " + executeSeconds + "sec " + executeMillis + "mil");
                if (GlobalConfTSIndex.queryType == 1)
                    System.out.println("Total number of results: " + totalResults.size());
            }
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
