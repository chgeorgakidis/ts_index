package main.java.tools;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import main.java.tsindex.TS_Index_Bulk;
import main.java.tsindex.Node;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FunctionsTSIndex {

    public static void readAllData() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File(GlobalConfTSIndex.dataFile), "r");
        raf.seek(0);
        byte[] data = new byte[(int) (raf.length())];
        raf.readFully(data);
        GlobalConfTSIndex.bigSequence = toFloatArray(data);
        raf.close();
    }

    public static void buildIndex(TS_Index_Bulk LS_Index) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(new File(GlobalConfTSIndex.dataFile), "r");

        int id = 0;
        float[] batchArray;
        raf.seek(0);
        TreeMap<String, ArrayList<float[]>> elements = null;
        try {
            byte[] currBatch = new byte[(int) (raf.length())];
            raf.readFully(currBatch);
            batchArray = toFloatArray(currBatch);
            elements = new TreeMap<>((o1, o2) -> o1.compareToIgnoreCase(o2));
            for (int i = 0; i < batchArray.length - GlobalConfTSIndex.defaultWindowSize; i++) {
                float[] tsArray = Arrays.copyOfRange(batchArray, i, i + GlobalConfTSIndex.defaultWindowSize);
                float[] PAA = getPAAandID(tsArray, id++);
                int[] embedding = getEmbedding(PAA, GlobalConfTSIndex.scale, GlobalConfTSIndex.shift);
                String zorder = ZOrder.valueOf(embedding);
                if (elements.containsKey(zorder)) {
                    elements.get(zorder).add(PAA);
                } else {
                    ArrayList<float[]> tmp = new ArrayList<>();
                    tmp.add(PAA);
                    elements.put(zorder, tmp);
                }
                if (id % 100000 == 0) {
                    System.out.println(id + " elements added...");
                }
            }
        } catch (EOFException e) {
            System.out.println("Done!");
            raf.close();
        }

        LS_Index.build(elements);
        LS_Index.getRoot().calculateIntervals();

        System.out.println("Build Complete!");
        System.out.println();
    }

    public static float[] getPAAandID(float[] data, int id) throws Exception {
        if (Math.IEEEremainder(data.length, GlobalConfTSIndex.numSeg) != 0)
            throw new RuntimeException("Datalength not divisible by number of segments!");

        int segment_size = data.length / GlobalConfTSIndex.numSeg;
        int offset = 0;
        float[] PAA = new float[GlobalConfTSIndex.numSeg + 1];
        /*if (GlobalConf.numSeg == data.length) {
            PAA = Arrays.copyOf(data, data.length);
        }*/

        PAA[GlobalConfTSIndex.numSeg] = id;
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            PAA[i] = FunctionsTSIndex.mean(data, offset, offset + segment_size - 1);
            offset = offset + segment_size;
        }
        return PAA;
    }

    public static int[] getEmbedding(float[] PAA, float scale, int shift) throws Exception {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int minIDX = 0;
        int maxIDX = 0;
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            int scaledPAA = (int) ((PAA[i]+shift)*scale);
            if (scaledPAA < min) {
                min = scaledPAA;
                minIDX = i;
            }
            if (scaledPAA > max) {
                max = scaledPAA;
                maxIDX = i;
            }
        }
        int mean = (int) (mean(PAA, 0, PAA.length-1) * GlobalConfTSIndex.scale);
        int stdDev = (int) (stdDev(PAA) * GlobalConfTSIndex.scale);
        return new int[]{min, (int) (minIDX*(GlobalConfTSIndex.scale)), max, (int) (maxIDX*(GlobalConfTSIndex.scale)), mean, stdDev};
        //return new int[]{min, (int) (minIDX*(GlobalConf.scale)), max, (int) (maxIDX*(GlobalConf.scale))};
    }

    public static Tuple2<float[], int[]> getPAAandEmbedding(float[] data, float scale, int shift) throws Exception {
        if (Math.IEEEremainder(data.length, GlobalConfTSIndex.numSeg) != 0)
            throw new RuntimeException("Datalength not divisible by number of segments!");
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int minIDX = 0;
        int maxIDX = 0;
        int segment_size = data.length / GlobalConfTSIndex.numSeg;
        int offset = 0;
        float[] PAA = new float[GlobalConfTSIndex.numSeg];
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            PAA[i] = FunctionsTSIndex.mean(data, offset, offset + segment_size - 1);
            offset = offset + segment_size;
            int scaledPAA = (int) ((PAA[i]+shift)*scale);
            if (scaledPAA < min) {
                min = scaledPAA;
                minIDX = i;
            }
            if (scaledPAA > max) {
                max = scaledPAA;
                maxIDX = i;
            }
        }
        //int mean = (int) (mean(PAA, 0, PAA.length-1) * GlobalConf.scale);
        //int stdDev = (int) (stdDev(PAA) * GlobalConf.scale);
        //return new Tuple2<>(PAA, new int[]{min, (int) (minIDX*(GlobalConf.scale-100)), max, (int) (maxIDX*(GlobalConf.scale-100)), mean, stdDev});
        return new Tuple2<>(PAA, new int[]{min, (int) (minIDX*(GlobalConfTSIndex.scale)), max, (int) (maxIDX*(GlobalConfTSIndex.scale))});
    }

    public static float[] zNormalization(float[] timeSeries) throws Exception {

        float mean = mean(timeSeries, 0, timeSeries.length - 1);
        float std = stdDev(timeSeries);

        float[] normalized = new float[timeSeries.length];

        if (std == 0)
            std = 1;

        for (int i = 0; i < timeSeries.length; i++) {
            normalized[i] = (timeSeries[i] - mean) / std;
        }

        return normalized;
    }

    public static float stdDev(float[] timeSeries) throws Exception {
        double mean = mean(timeSeries, 0, timeSeries.length - 1);
        double var = 0.0f;

        for (int i = 0; i < timeSeries.length; i++) {
            var += (timeSeries[i] - mean) * (timeSeries[i] - mean);
        }
        var /= (timeSeries.length - 1);

        return (float) Math.sqrt(var);
    }

    public static float mean(float[] data, int index1, int index2) throws Exception {
        if (index1 < 0 || index2 < 0 || index1 >= data.length || index2 >= data.length) {
            throw new Exception("Invalid index!");
        }

        if (index1 > index2) {
            int temp = index2;
            index2 = index1;
            index1 = temp;
        }

        double sum = 0;
        for (int i = index1; i <= index2; i++) {
            sum += data[i];
        }

        return (float) (sum / (index2 - index1 + 1));
    }

    public static void parseData(String path) throws Exception {
        File file = new File(GlobalConfTSIndex.dataPath + path);
        String contents[] = file.list();
        GlobalConfTSIndex.tsLength = 0;
        for (String filename : contents) {
            if (filename.contains("README") || filename.contains("input.csv") || filename.contains("DS_Store"))
                continue;
            File innerFile = new File(GlobalConfTSIndex.dataPath + path + "/" + filename);
            InputStream in = new BufferedInputStream(new FileInputStream(innerFile));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            int tsLength = 0;
            int noLines = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (noLines == 0) {
                    String[] parts = line.split("\t");
                    tsLength = parts.length - 1;
                }
                noLines++;
            }
            br.close();
            GlobalConfTSIndex.tsLength += noLines * tsLength;
        }

        int count = 0;
        GlobalConfTSIndex.bigSequence = new float[GlobalConfTSIndex.tsLength];
        GlobalConfTSIndex.dataFile = GlobalConfTSIndex.dataPath + path + "/input.csv";
        File output = new File(GlobalConfTSIndex.dataFile);
        Files.deleteIfExists(output.toPath());
        OutputStream os = new FileOutputStream(output);
        for (String filename : contents) {
            if (filename.contains("README") || filename.contains("input.csv") || filename.contains("DS_Store"))
                continue;

            File innerFile = new File(GlobalConfTSIndex.dataPath + path + "/" + filename);
            InputStream in = new BufferedInputStream(new FileInputStream(innerFile));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                for (int i = 1; i < parts.length; i++) {
                    if (GlobalConfTSIndex.zNormalize == 0) {
                        os.write(convertFloatToByteArray(Float.parseFloat(line)));
                    }
                    GlobalConfTSIndex.bigSequence[count] = Float.parseFloat(parts[i]);
                    count++;
                }
            }
            in.close();
            br.close();
        }

        if (GlobalConfTSIndex.zNormalize == 1) {
            GlobalConfTSIndex.bigSequence = zNormalization(GlobalConfTSIndex.bigSequence);
            for (float d : GlobalConfTSIndex.bigSequence) {
                os.write(convertFloatToByteArray(d));
            }
        }
        os.close();
    }

    public static void readQuery(int zNormalize) {
        GlobalConfTSIndex.querySubSeq = new float[GlobalConfTSIndex.defaultWindowSize];
        System.arraycopy(GlobalConfTSIndex.bigSequence, GlobalConfTSIndex.startValue, GlobalConfTSIndex.querySubSeq, 0, GlobalConfTSIndex.defaultWindowSize);

        if (zNormalize == 1) {
            GlobalConfTSIndex.queryCheckSeq = new int[GlobalConfTSIndex.querySubSeq.length];
            double[] queryTmpSeq = new double[GlobalConfTSIndex.querySubSeq.length];
            for (int i = 0; i < GlobalConfTSIndex.queryCheckSeq.length; i++) {
                GlobalConfTSIndex.queryCheckSeq[i] = i;
                queryTmpSeq[i] = GlobalConfTSIndex.querySubSeq[i];
            }
            double tmp1;
            int tmp2;
            for (int i = 0; i < queryTmpSeq.length; i++) {
                for (int j = 1; j < (queryTmpSeq.length - i); j++) {
                    if (Math.abs(queryTmpSeq[j - 1]) < Math.abs(queryTmpSeq[j])) {
                        tmp1 = queryTmpSeq[j - 1];
                        tmp2 = GlobalConfTSIndex.queryCheckSeq[j - 1];
                        queryTmpSeq[j - 1] = queryTmpSeq[j];
                        GlobalConfTSIndex.queryCheckSeq[j - 1] = GlobalConfTSIndex.queryCheckSeq[j];
                        queryTmpSeq[j] = tmp1;
                        GlobalConfTSIndex.queryCheckSeq[j] = tmp2;
                    }
                }
            }
        }
    }

    public static void countNodes(Node n) {
        if (n.children != null) {
            for (Node c : n.children) {
                if (c.isLeaf())
                    GlobalConfTSIndex.totalLeafNodes++;
                else
                    GlobalConfTSIndex.totalInnerNodes++;
                countNodes(c);
            }
        }
    }

    public static int countPrunedSubNodes(Node n, int pruned) {
        if (n.children != null) {
            for (Node c : n.children) {
                pruned++;
                countPrunedSubNodes(c, pruned);
            }
        }

        return pruned;
    }

    public static void save(TS_Index_Bulk index) throws IOException {
        Output output = new Output(new FileOutputStream(GlobalConfTSIndex.indexStorePath + "/TS_index.dr"));
        Kryo kryo = new Kryo();
        kryo.writeClassAndObject(output, index);
        output.close();
    }

    public static TS_Index_Bulk load() throws IOException {
        Input input = new Input(new FileInputStream(GlobalConfTSIndex.indexStorePath + "/TS_index.dr"));
        Kryo kryo = new Kryo();
        TS_Index_Bulk index = (TS_Index_Bulk) kryo.readClassAndObject(input);
        input.close();
        return index;
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static float[] getPAA(float[] data) throws Exception {
        if (Math.IEEEremainder(data.length, GlobalConfTSIndex.numSeg) != 0)
            throw new RuntimeException("Datalength not divisible by number of segments!");

        int segment_size = data.length / GlobalConfTSIndex.numSeg;
        int offset = 0;
        float[] PAA = new float[GlobalConfTSIndex.numSeg];
        if (GlobalConfTSIndex.numSeg == data.length) {
            PAA = Arrays.copyOf(data, data.length);
        }

        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            PAA[i] = FunctionsTSIndex.mean(data, offset, offset + segment_size - 1);
            offset = offset + segment_size;
        }
        return PAA;
    }

    public static int[] getPAAScaled(float[] data, float scale, int shift) throws Exception {
        if (Math.IEEEremainder(data.length, GlobalConfTSIndex.numSeg) != 0)
            throw new RuntimeException("Datalength not divisible by number of segments!");

        int segment_size = data.length / GlobalConfTSIndex.numSeg;
        int offset = 0;
        int[] PAA = new int[GlobalConfTSIndex.numSeg];
        for (int i = 0; i < GlobalConfTSIndex.numSeg; i++) {
            PAA[i] = (int) ((FunctionsTSIndex.mean(data, offset, offset + segment_size - 1)+shift) * scale);
            offset = offset + segment_size;
        }
        return PAA;
    }

    public static byte[] convertFloatToByteArray(float number) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES);
        byteBuffer.putFloat(number);
        return byteBuffer.array();
    }

    public static float[] toFloatArray(byte[] byteArray) {
        int times = Float.SIZE / Byte.SIZE;
        float[] floats = new float[byteArray.length / times];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = ByteBuffer.wrap(byteArray, i * times, times).getFloat();
        }
        return floats;
    }

    public static int maxDecDigits(int dimension) {
        int max = 32;
        BigInteger maxDec = new BigInteger("1");
        maxDec = maxDec.shiftLeft(dimension * max);
        maxDec.subtract(BigInteger.ONE);
        return maxDec.toString().length();
    }

    public static String maxDecString(int dimension) {
        int max = 32;
        BigInteger maxDec = new BigInteger("1");
        maxDec = maxDec.shiftLeft(dimension * max);
        maxDec.subtract(BigInteger.ONE);
        return maxDec.toString();
    }

    public static String createExtra(int num) {
        if (num < 1)
            return "";

        char[] extra = new char[num];
        for (int i = 0; i < num; i++)
            extra[i] = '0';
        return (new String(extra));
    }

    public static void calculateBoundsDiffs(Node n) {
        if (n.children != null) {
            for (Node c : n.children) {
                if (c.isLeaf()) {
                    for (int i = 0; i< GlobalConfTSIndex.numSeg; i++) {
                        GlobalConfTSIndex.leafBoundDiffs.add(c.MBTShi[i]-c.MBTSlo[i]);
                    }
                }
                else {
                    for (int i = 0; i< GlobalConfTSIndex.numSeg; i++) {
                        GlobalConfTSIndex.innerBoundDiffs.add(c.MBTShi[i]-c.MBTSlo[i]);
                    }
                }
                calculateBoundsDiffs(c);
            }
        }
    }

    public static float stdDev(ArrayList<Float> data) {
        double mean = mean(data);
        double var = 0.0f;

        for (int i = 0; i < data.size(); i++) {
            var += (data.get(i) - mean) * (data.get(i) - mean);
        }
        var /= (data.size() - 1);

        return (float) Math.sqrt(var);
    }

    public static float mean(ArrayList<Float> data) {
        float sum = 0;
        for (int i=0; i<data.size(); i++) {
            sum += data.get(i);
        }
        return sum / data.size();
    }

    public static double getMaxDiff(ArrayList<Float> boundDiffs) {
        double max = 0;
        for (int i=0; i<boundDiffs.size(); i++) {
            if (boundDiffs.get(i) > max)
                max = boundDiffs.get(i);
        }
        return max;
    }

    public static double getMinDiff(ArrayList<Float> boundDiffs) {
        double min = Double.MAX_VALUE;
        for (int i=0; i<boundDiffs.size(); i++) {
            if (boundDiffs.get(i) < min)
                min = boundDiffs.get(i);
        }
        return min;
    }
}
