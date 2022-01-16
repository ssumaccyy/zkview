package util;

public class NumberHelper {
	public static Integer parse2IntOrNull(String value) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return null;
		}
	}
}
