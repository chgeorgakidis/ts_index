package mainapp;

import main.java.tools.FunctionsSweepLine;
import main.java.tools.GlobalConfSweepLine;

import java.io.EOFException;
import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class RunSweepLine {

    public static void main(String[] args) throws Exception {

        GlobalConfSweepLine.epsilon = Double.parseDouble(args[0]);
        GlobalConfSweepLine.noQueries = Integer.parseInt(args[1]);
        GlobalConfSweepLine.defaultWindowSize = Integer.parseInt(args[2]);
        GlobalConfSweepLine.dataPath = args[3];
        GlobalConfSweepLine.resultsPath = args[4];

        long totalElapsedExecuteTime = 0;
        long startExecuteTime;

        File dataPath = new File(GlobalConfSweepLine.dataPath);
        String contents[] = dataPath.list();
        for (String filename : contents) {
            if (filename.contains("DS_Store"))
                continue;

            GlobalConfSweepLine.resultIndices = new LinkedList<>();
            PrintWriter resultsWriter = new PrintWriter(GlobalConfSweepLine.resultsPath + filename);
            FunctionsSweepLine.parseData(filename);
            FunctionsSweepLine.readAllData();
            int count = 0;
            for (int j = 0; j < GlobalConfSweepLine.noQueries; j++) {
                startExecuteTime = System.currentTimeMillis();
                GlobalConfSweepLine.startValue = randomGenerator(j);
                FunctionsSweepLine.readQuery(GlobalConfSweepLine.zNormalize);
                RandomAccessFile raf = new RandomAccessFile(new File(GlobalConfSweepLine.dataFile), "r");
                int id = 0;
                double[] tsArray;
                int pos = 0;
                raf.seek(pos);
                byte[] currDouble = new byte[8 * GlobalConfSweepLine.defaultWindowSize];
                while (true) {
                    try {
                        raf.readFully(currDouble);
                        tsArray = FunctionsSweepLine.toDoubleArray(currDouble);
                        FunctionsSweepLine.checkLocalSim(tsArray, id++);
                        raf.seek(id * 8);
                    } catch (EOFException e) {
                        raf.close();
                        break;
                    }
                }
                count++;
                System.out.println("Run " + count + " queries...");
                totalElapsedExecuteTime += System.currentTimeMillis() - startExecuteTime;
            }


            Collections.sort(GlobalConfSweepLine.resultIndices);
            int executeMillis = (int) totalElapsedExecuteTime % 1000;
            int executeSeconds = (int) (totalElapsedExecuteTime / 1000) % 60;
            int executeMinutes = (int) ((totalElapsedExecuteTime / (1000 * 60)) % 60);
            int executeHours = (int) ((totalElapsedExecuteTime / (1000 * 60 * 60)) % 24);

            resultsWriter.println("######### QUERY RESULTS STATS #########");
            resultsWriter.println("Total time: " + executeHours + "h " + executeMinutes + "m " + executeSeconds + "sec " + executeMillis + "mil");
            resultsWriter.println("Total number of results: " + GlobalConfSweepLine.resultIndices.size());
            resultsWriter.println("Total number of checks: " + GlobalConfSweepLine.checkCounter);
            resultsWriter.close();
        }
    }

    private static int randomGenerator(int seed) {
        Random generator = new Random(seed);
        int random_int = generator.nextInt(0, GlobalConfSweepLine.tsLength - GlobalConfSweepLine.defaultWindowSize + 1);
        return random_int;
    }
}