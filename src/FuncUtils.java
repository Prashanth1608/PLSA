import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.StatUtils;

import cc.mallet.util.Maths;

public class FuncUtils {
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				int compare = (o1.getValue()).compareTo(o2.getValue());
				return -compare;
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				int compare = (o1.getValue()).compareTo(o2.getValue());
				return compare;
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Get the log Diriclet delta value of a double array of same values
	 * 
	 * @param value
	 *            : the value of every element in the array
	 * @param vectorSize
	 *            : the size of the array
	 * @return
	 */
	public static double getLogDirDelta(double[] array) {
		// Log(DirDelta(alpha) = sum(log(Gamma(alpha_i))) -
		// log(Gamma(sum(alpha_i)))
		double logDirDelta = -1.0 * Maths.logGamma(StatUtils.sum(array));
		for (int i = 0; i < array.length; i++)
			logDirDelta += Maths.logGamma(array[i]);
		return logDirDelta;
	}

	public static double getLogDirDelta(double value, int dims) {
		// Log(DirDelta(alpha) = sum(log(Gamma(alpha_i))) -
		// log(Gamma(sum(alpha_i)))
		return dims * Maths.logGamma(value) - Maths.logGamma(value * dims);
	}

	/**
	 * Sample a value from a double array;
	 * 
	 * @param probs
	 * @return
	 */
	public static int nextDiscrete(double[] probs) {
		double sum = 0.0;
		double r = MTRandom.nextDouble() * StatUtils.sum(probs);
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r)
				return i;
		}
		return probs.length - 1;
	}
}
