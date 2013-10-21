package jiracommitviewer.index.exception;

/**
 * An error to indicate a general problem reading/writing the index.
 * 
 * @author mark
 */
@SuppressWarnings("serial")
public class IndexException extends Exception {

	public IndexException(final String s, final Throwable t) {
		super(s, t);
	}
	
	public IndexException(final String s) {
		super(s);
	}
}
