package queryparse;

public class ReservedSymbols {

	public static final char CLOSE_PAREN = ')';
	public static final char OPEN_PAREN = '(';
	public static final char OPTIONAL = '?';
	public static final char REPEAT = '*';
	public static final char OPEN_SQUARE = '[';
	public static final char CLOSE_SQUARE = ']';
	public static final char DOT = '.';
	public static final char PLUS = '+';
	public static final char OR = '|';

	public static boolean isReserved(char c) {
		switch (c) {
			case CLOSE_PAREN:
			case OPEN_PAREN:
			case OPTIONAL:
			case REPEAT:
			case OPEN_SQUARE:
			case CLOSE_SQUARE:
			case DOT:
			case PLUS:
			case OR:
				return true;
			default:
				return false;
		}
	}
}
