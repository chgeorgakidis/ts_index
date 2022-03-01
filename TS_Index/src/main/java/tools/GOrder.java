package main.java.tools;

import java.math.BigInteger;
import java.util.Vector;

public class GOrder {

	private static String createExtra(int num) {
		if (num < 1)
			return "";

		char[] extra = new char[num];
		for (int i = 0; i < num; i++)
			extra[i] = '0';
		return (new String(extra));
	}
	
	public static String valueOf(int[] coord) {
		Vector<String> arrPtr = new Vector<String>(GlobalConfTSIndex.dims);
		// System.out.println( "maxDec " + maxDec.toString() );
		int max = 32;
		int fix = FunctionsTSIndex.maxDecDigits(GlobalConfTSIndex.dims); // global maximum possible zvalue
		// length
		// System.out.println( fix );

		for (int i = 0; i < GlobalConfTSIndex.dims; i++) {
			String p = Integer.toBinaryString((int) coord[i]);
			// System.out.println( coord[i] + " " + p );
			arrPtr.add(p);
		}

		for (int i = 0; i < arrPtr.size(); ++i) {
			String extra = createExtra(max - arrPtr.elementAt(i).length());
			arrPtr.set(i, extra + arrPtr.elementAt(i));
			// System.out.println( i + " " + arrPtr.elementAt(i) );
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
		// System.out.println( value );
		// Covert a binary representation of order into a big integer
		BigInteger integer = new BigInteger(order, 2);

		// Convert the big integer to Gray code format
		BigInteger integerShifted = integer.shiftRight(1);
		BigInteger ret = integer.xor(integerShifted);

		// Return a fixed length decimal String representation of
		// the big integer (z-order)
		order = ret.toString();
		// System.out.println( order );
		if (order.length() < fix) {
			String extra = createExtra(fix - order.length());
			order = extra + order;
		} else if (order.length() > fix) {
			System.out.println("too big gray order, need to fix Gorder.java");
			System.exit(-1);
		}

		//int[] tmp = toCoord(order, 12);

		return order;
	}

	public static int[] toCoord(String g) {
		int DECIMAL_RADIX = 10;
		int BINARY_RADIX = 2;

		if (g == null) {
			System.out.println("Z-order Null pointer!!!@Zorder.toCoord");
			System.exit(-1);
		}

		BigInteger bigG = new BigInteger(g, DECIMAL_RADIX);

		// Convert from Gray code format to normal integer
		BigInteger mask = null;
		for (mask = bigG.shiftRight(1); mask != BigInteger.ZERO; mask = mask.shiftRight(1)) {
			bigG = bigG.xor(mask);
		}

		String bigZStr = bigG.toString(BINARY_RADIX);

		int len = bigZStr.length();
		int prefixZeros = 0;
		if (len % GlobalConfTSIndex.dims != 0)
			prefixZeros = GlobalConfTSIndex.dims - len % GlobalConfTSIndex.dims;

		String prefix = ZOrder.createExtra(prefixZeros);
		bigZStr = prefix + bigZStr;

		len = bigZStr.length();

		if (len % GlobalConfTSIndex.dims != 0) {
			System.out.println("Wrong prefix!!!@Gorder.toCoord");
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