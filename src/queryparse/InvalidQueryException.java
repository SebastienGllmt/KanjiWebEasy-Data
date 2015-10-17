package queryparse;

@SuppressWarnings("serial")
public class InvalidQueryException extends Exception {
	public InvalidQueryException(String message) {
		super(message);
	}
}