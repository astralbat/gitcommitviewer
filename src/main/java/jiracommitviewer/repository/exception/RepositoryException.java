package jiracommitviewer.repository.exception;

/**
 * An error to indicate a problem interacting with a code repository.
 * 
 * @author mark
 */
@SuppressWarnings("serial")
public class RepositoryException extends Exception {

	public RepositoryException(final String s, final Throwable t) {
		super(s, t);
	}
	
	public RepositoryException(final String s) {
		super(s);
	}
}
