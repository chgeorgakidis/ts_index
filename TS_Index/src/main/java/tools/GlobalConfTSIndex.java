package main.java.tools;

import com.google.common.collect.MinMaxPriorityQueue;

import java.util.*;

public class GlobalConfTSIndex {

    //######## PATHS ########//
    public static String dataPath = "/Users/gchatzi/Desktop/UCR_Subset/";
    public static String dataFile;
    public static String indexStorePath = "/Users/gchatzi/Library/Mobile Documents/com~apple~CloudDocs/Data/TSIndex/";
    public static String resultsPath = "/Users/gchatzi/Desktop/resultsTSIndex/";

    //######## INDEX ########//
    public static boolean variableLengthQuery = false;
    public static int defaultWindowSize = 100;
    public static int queryLength = 100;
    public static int zNormalize = 1;
    public static int indexIDs = 0;
    public static float[] bigSequence;
    public static LinkedList<Float> resultIndicesRange;
    public static MinMaxPriorityQueue<ElementDistance> resultIndicesTopK;
    public static int tsLength;
    public static int M = 30;
    public static int m = 10;
    public static int numSeg = 25;
    public static float scale = 10f;
    public static int shift = 100;
    public static int dims = 6;

    //######## QUERY ########//
    public static int noQueries = 100;
    public static int queryType = 0; // 0: Sim search, 1: Top-k
    public static double checkCounter = 0;
    public static int prunedInnerNodes = 0;
    public static int prunedLeafNodes = 0;
    public static int passedInnerNodes = 0;
    public static int passedLeafNodes = 0;
    public static int totalInnerNodes = 0;
    public static int totalLeafNodes = 0;
    public static int totalPrunedNodes = 0;
    public static ArrayList<Float> innerBoundDiffs;
    public static ArrayList<Float> leafBoundDiffs;
    public static int totalSubsequencesChecked = 0;
    public static int startValue = 1000;
    public static int[] queryCheckSeq;
    public static float[] querySubSeq;
    public static float[] queryPAA;
    public static float epsilon = 0.5f;
    public static int k = 25;
    public static int[] queryLengths = {50, 100, 150, 200, 250};
    public static double kThreshold = Double.MAX_VALUE;
    public static int nodeId = 0;
}
