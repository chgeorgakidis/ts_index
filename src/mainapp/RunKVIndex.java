package mainapp;

import main.java.kvindex.IndexBuilder;
import main.java.kvindex.IndexNode;
import main.java.query.EpsilonMatchingQuery;
import main.java.tools.FunctionsKVIndex;
import main.java.tools.GlobalConfKVIndex;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class RunKVIndex {

    public static void main(String[] args) throws Exception {

        GlobalConfKVIndex.epsilon = Double.parseDouble(args[0]);
        GlobalConfKVIndex.noQueries = Integer.parseInt(args[1]);
        GlobalConfKVIndex.defaultWindowSize = Integer.parseInt(args[2]);
        GlobalConfKVIndex.dataPath = args[3];
        GlobalConfKVIndex.indexStorePath = args[4];
        GlobalConfKVIndex.resultsPath = args[5];

        long startExecuteTime;
        long totalElapsedExecuteTime;
        int executeMillis, executeSeconds, executeMinutes, executeHours;

        File dataPath = new File(GlobalConfKVIndex.dataPath);
        String contents[] = dataPath.list();
        for (String filename : contents) {
            if (filename.contains("DS_Store"))
                continue;

            PrintWriter resultsWriter = new PrintWriter(GlobalConfKVIndex.resultsPath + filename);
            System.out.println("Building for " + filename + "...");
            parseInputData(filename);
            IndexBuilder kvindexBuilder = new IndexBuilder(GlobalConfKVIndex.dataFile);
            startExecuteTime = System.currentTimeMillis();
            Map<Double, IndexNode> kvindex = kvindexBuilder.buildIndex();
            totalElapsedExecuteTime = System.currentTimeMillis() - startExecuteTime;
            executeMillis = (int) totalElapsedExecuteTime % 1000;
            executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
            executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
            executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);
            resultsWriter.println("######### INDEX BUILD TIME STATS #########");
            resultsWriter.println("Index Build time: " + executeHours + "h " + executeMinutes + "m " + executeSeconds + "sec " + executeMillis + "mil");

            EpsilonMatchingQuery runQuery = new EpsilonMatchingQuery();
            GlobalConfKVIndex.resultIndices = new LinkedList<>();
            FunctionsKVIndex.readAllData();
            totalElapsedExecuteTime = 0;
            for (int j = 0; j < GlobalConfKVIndex.noQueries; j++) {
                GlobalConfKVIndex.startValue = randomGenerator(j);
                FunctionsKVIndex.readQuery(GlobalConfKVIndex.zNormalize);
                startExecuteTime = System.currentTimeMillis();
                runQuery.execute(kvindex, GlobalConfKVIndex.querySubSeq, GlobalConfKVIndex.epsilon);
                totalElapsedExecuteTime += System.currentTimeMillis() - startExecuteTime;
            }

            kvindexBuilder.save();
            Collections.sort(GlobalConfKVIndex.resultIndices);
            executeMillis = (int) totalElapsedExecuteTime % 1000;
            executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
            executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
            executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);

            resultsWriter.println("\n######### INDEX SIZE STATS #########");
            Path filePath = Paths.get(GlobalConfKVIndex.indexStorePath + "/KV_index.dr");
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
            resultsWriter.println("Total number of results: " + GlobalConfKVIndex.resultIndices.size());
            resultsWriter.println("Total number of checks: " + GlobalConfKVIndex.checkCounter);

            resultsWriter.close();
        }
    }

    private static void parseInputData(String filename) throws Exception {
        FunctionsKVIndex.parseData(filename);
    }

    private static int randomGenerator(int seed) {
        Random generator = new Random(seed);
        int random_int = generator.nextInt(0, GlobalConfKVIndex.tsLength - GlobalConfKVIndex.defaultWindowSize + 1);
        return random_int;
    }

}
