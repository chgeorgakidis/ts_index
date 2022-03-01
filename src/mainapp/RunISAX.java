package mainapp;

import com.google.common.collect.MinMaxPriorityQueue;
import main.java.isax.IndexOptions;
import main.java.isax.iSAXIndex;
import main.java.query.Query;
import main.java.query.TwinSubsequenceSearchQuery;
import main.java.query.TwinSubsequenceTopKQuery;
import main.java.tools.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class RunISAX {

    public static void main(String[] args) throws Exception {

        GlobalConfiSAX.queryType = Integer.parseInt(args[0]);
        if (GlobalConfiSAX.queryType == 0) {
            GlobalConfiSAX.epsilon = Double.parseDouble(args[1]);
        } else {
            GlobalConfiSAX.k = Integer.parseInt(args[1]);
        }
        GlobalConfiSAX.maxEntries = Integer.parseInt(args[2]);
        GlobalConfiSAX.wordlength = Byte.parseByte(args[3]);
        GlobalConfiSAX.noQueries = Integer.parseInt(args[4]);
        GlobalConfiSAX.defaultWindowSize = Integer.parseInt(args[5]);
        GlobalConfiSAX.dataPath = args[6];
        GlobalConfiSAX.indexStorePath = args[7];
        GlobalConfiSAX.resultsPath = args[8];

        //######## READ BREAKPOINTS ########//
        BufferedInputStream in = new BufferedInputStream(new FileInputStream("SAXalphabet"));
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        GlobalConfiSAX.breakpoints = new HashMap<>();

        if (GlobalConfiSAX.zNormalize == 1) {
            String line;
            short cnt = 1;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                Double[] values = new Double[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    values[i] = Double.parseDouble(parts[i]);
                }
                if (cnt == 256) {
                    GlobalConfiSAX.breakpoints.put((short) 512, values);
                } else {
                    GlobalConfiSAX.breakpoints.put(++cnt, values);
                }
            }
        }

        long startExecuteTime;
        long totalElapsedExecuteTime;
        int executeMillis, executeSeconds, executeMinutes, executeHours;
        iSAXIndex si;

        File dataPath = new File(GlobalConfiSAX.dataPath);
        String contents[] = dataPath.list();
        for (String filename : contents) {
            if (filename.contains("DS_Store"))
                continue;
            PrintWriter resultsWriter = new PrintWriter(GlobalConfiSAX.resultsPath + filename);
            System.out.println("Building for " + filename + "...");
            parseInputData(filename);

            if (GlobalConfiSAX.zNormalize == 0) {
                FunctionsiSAX.readAllData();
                FunctionsiSAX.generateBreakpoints();
            }
            startExecuteTime = System.currentTimeMillis();
            si = new iSAXIndex(0, new IndexOptions("root/"));
            FromFileRawDataLoader dl = new FromFileRawDataLoader(si);
            try {
                InsertTimeSeries(dl);
            } catch (Exception e) {
                continue;
            }
            totalElapsedExecuteTime = System.currentTimeMillis() - startExecuteTime;
            executeMillis = (int) totalElapsedExecuteTime % 1000;
            executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
            executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
            executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);
            resultsWriter.println("######### INDEX BUILD TIME STATS #########");
            resultsWriter.println("Index Build time: " + executeHours + "h " + executeMinutes + "m " + executeSeconds + "sec " + executeMillis + "mil");

            Query runQuery = null;
            if (GlobalConfiSAX.queryType == 0) {
                runQuery = new TwinSubsequenceSearchQuery();
                GlobalConfiSAX.resultIndicesRange = new LinkedList<>();
            } else if (GlobalConfiSAX.queryType == 1) {
                runQuery = new TwinSubsequenceTopKQuery();
                GlobalConfiSAX.resultIndicesTopK = MinMaxPriorityQueue.orderedBy(new DistComparator()).maximumSize(GlobalConfiSAX.k).create();
            }

            FunctionsiSAX.readAllData();
            totalElapsedExecuteTime = 0;
            for (int j = 0; j < GlobalConfiSAX.noQueries; j++) {
                GlobalConfiSAX.startValue = randomGenerator(j);
                FunctionsiSAX.readQuery(GlobalConfiSAX.zNormalize);
                GlobalConfiSAX.queryPAA = FunctionsiSAX.getPAA(GlobalConfiSAX.querySubSeq);
                startExecuteTime = System.currentTimeMillis();
                runQuery.execute(si, GlobalConfiSAX.querySubSeq, GlobalConfiSAX.epsilon);
                totalElapsedExecuteTime += System.currentTimeMillis() - startExecuteTime;
            }

            executeMillis = (int) totalElapsedExecuteTime % 1000;
            executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
            executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
            executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);

            FunctionsiSAX.countNodes(si.getIndex());
            FunctionsiSAX.save(si);

            resultsWriter.println("\n######### INDEX SIZE STATS #########");
            Path filePath = Paths.get(GlobalConfiSAX.indexStorePath + "/iSAX_index.dr");
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
            if (GlobalConfiSAX.queryType == 0) {
                resultsWriter.println("Total number of results: " + GlobalConfiSAX.resultIndicesRange.size());
            } else if (GlobalConfiSAX.queryType == 1) {
                resultsWriter.println("Total number of results: " + GlobalConfiSAX.resultIndicesTopK.size());
            }
            resultsWriter.println("Total number of checks: " + GlobalConfiSAX.checkCounter);

            resultsWriter.println("\n######### NODE PRUNING STATS #########");
            resultsWriter.println("Inner nodes pruned: " + (GlobalConfiSAX.prunedInnerNodes / (GlobalConfiSAX.noQueries)) + "/" + GlobalConfiSAX.totalInnerNodes);
            resultsWriter.println("Inner nodes accepted: " + (GlobalConfiSAX.passedInnerNodes / (GlobalConfiSAX.noQueries)) + "/" + GlobalConfiSAX.totalInnerNodes);
            resultsWriter.println("Leaf nodes pruned: " + (GlobalConfiSAX.prunedLeafNodes / (GlobalConfiSAX.noQueries)) + "/" + GlobalConfiSAX.totalLeafNodes);
            resultsWriter.println("Leaf nodes accepted: " + (GlobalConfiSAX.passedLeafNodes / (GlobalConfiSAX.noQueries)) + "/" + GlobalConfiSAX.totalLeafNodes);
            resultsWriter.println("Nodes pruned: " + (GlobalConfiSAX.prunedInnerNodes / (GlobalConfiSAX.noQueries) + GlobalConfiSAX.prunedLeafNodes / (GlobalConfiSAX.noQueries)) +
                    "/" + (GlobalConfiSAX.totalInnerNodes + GlobalConfiSAX.totalLeafNodes));

            resultsWriter.println("Total raw subsequences checked: " + GlobalConfiSAX.totalSubsequencesChecked);
            resultsWriter.close();
        }
    }

    public static void InsertTimeSeries(FromFileRawDataLoader dl) throws Exception {
        System.out.println("Inserting timeseries to index.");
        System.out.println("------------------------------");
        dl.buildIndexSubsequence();
        System.out.println("Complete!");
        System.out.println();
    }

    private static void parseInputData(String filename) throws Exception {
        FunctionsiSAX.parseData(filename);
    }

    private static int randomGenerator(int seed) {
        Random generator = new Random(seed);
        int random_int = generator.nextInt(0, GlobalConfiSAX.tsLength - GlobalConfiSAX.defaultWindowSize + 1);
        return random_int;
    }

}
