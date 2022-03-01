package main.java.tools;

import java.math.BigInteger;
import java.util.Vector;

/**
 * source: http://www.cs.utah.edu/~lifeifei/knnj/#codes
 **/
public class ZOrder {

    public static String createExtra(int num) {
        if (num < 1)
            return "";

        char[] extra = new char[num];
        for (int i = 0; i < num; i++)
            extra[i] = '0';
        return (new String(extra));
    }

    public static String valueOf(int[] subseqPAA) {
        Vector<String> arrPtr = new Vector<>(GlobalConfTSIndex.dims);
        int max = 8;
        int fix = FunctionsTSIndex.maxDecDigits(GlobalConfTSIndex.dims); // global maximum possible zvalue

        for (int i = 0; i < GlobalConfTSIndex.dims; i++) {
            String p = Integer.toBinaryString(subseqPAA[i]);
            arrPtr.add(p);
        }

        for (int i = 0; i < arrPtr.size(); ++i) {
            String extra = createExtra(max - arrPtr.elementAt(i).length());
            arrPtr.set(i, extra + arrPtr.elementAt(i));
        }

        char[] value = new char[GlobalConfTSIndex.dims * max];
        int index = 0;

        // Create Zorder
        for (int i = 0; i < max; ++i) {
            for (String e : arrPtr) {
                char ch = e.charAt(i);
                value[index++] = ch;
            }
        }

        String order = new String(value);
        BigInteger ret = new BigInteger(order, 2);

        // Return a fixed length decimal String representation of the big integer (z-order)
        order = ret.toString();
        if (order.length() < fix) {
            String extra = createExtra(fix - order.length());
            order = extra + order;
        } else if (order.length() > fix) {
            System.out.println("too big zorder, need to fix Zorder.java");
            System.exit(-1);
        }

        return order;
    }

    public static int[] toCoord(String z) {
        int DECIMAL_RADIX = 10;
        int BINARY_RADIX = 2;

        if (z == null) {
            System.out.println("Z-order Null pointer!!!@Zorder.toCoord");
            System.exit(-1);
        }

        BigInteger bigZ = new BigInteger(z, DECIMAL_RADIX);
        String bigZStr = bigZ.toString(BINARY_RADIX);

        int len = bigZStr.length();
        int prefixZeros = 0;
        if (len % GlobalConfTSIndex.dims != 0)
            prefixZeros = GlobalConfTSIndex.dims - len % GlobalConfTSIndex.dims;

        String prefix = ZOrder.createExtra(prefixZeros);
        bigZStr = prefix + bigZStr;
        len = bigZStr.length();

        if (len % GlobalConfTSIndex.dims != 0) {
            System.out.println("Wrong prefix!!!@Zorder.toCoord");
            System.exit(-1);
        }

        // The most significant bit is save at starting position of
        // the char array.
        char[] bigZCharArray = bigZStr.toCharArray();
        int[] coord = new int[GlobalConfTSIndex.dims];
        for (int i = 0; i < GlobalConfTSIndex.dims; i++)
            coord[i] = 0;
        for (int i = 0; i < bigZCharArray.length; ) {
            for (int j = 0; j < GlobalConfTSIndex.dims; ++j) {
                coord[j] <<= 1;
                coord[j] |= bigZCharArray[i++] - '0';
            }
        }
        return coord;
    }
}