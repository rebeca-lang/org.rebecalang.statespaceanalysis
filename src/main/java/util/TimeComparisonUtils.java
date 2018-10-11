package util;


public class TimeComparisonUtils {
	
	public static boolean compareTime (int first, int second, OperatorType type) {
		switch(type) {
		case LE: return first < second;
		case LEQ: return first <= second;
		case GE: return first > second;
		case GEQ: return first >= second;
		case EQ: return first == second;
		case NEQ: return first != second;
		}
		return false;
	}
	

	public enum OperatorType {
		LE, LEQ, GE, GEQ, EQ, NEQ
	}

}
