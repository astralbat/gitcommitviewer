package jiracommitviewer;

/**
 * Manages a Git repository.
 * 
 * @author mark
 */
public interface GitManager {
	
	String GIT_URI = "git.uri";
	String GIT_REPOSITORY_NAME = "git.display.name";
	
	/** The link format for commits. */
    String GIT_LINKFORMAT_COMMIT = "linkformat.commit";
    /** The link format for files within a commit. */
    String GIT_LINKFORMAT_FILE_MODIFIED = "linkformat.file.modified";
	
	/**
	 * Gets the identity of this repository.
	 * 
	 * @return the identity
	 */
	Object getId();
	
	/**
	 * Gets the display name of this repository.
	 * 
	 * @return the display name. Never {@code null}
	 */
	String getDisplayName();
	
	/**
	 * Gets the connection URI.
	 * 
	 * @return the connection URI. Never {@code null}
	 */
	String getUri();
	
	/**
	 * Gets the view links to view code for particular commits.
	 * 
	 * @return the view links. Never {@code null}
	 */
	String getCommitLinkFormat();
	
	String getModifiedFileLinkFormat();

	/**
	 * Attempts to activate this repository if it was previously inactive. The URL is checked
	 * for correctness, and it is tested along with the credentials. If this should prove
	 * successful, then the repository will be active. If not, {@link #isActive()} will
	 * return {@code false} and {@link #getInactiveMessage()} will return the reason why.
	 */
	void activate();
	
	/**
	 * Gets whether this repository is active. The repository will be inactive if 
	 * the system could not activate it because of an error.
	 * 
	 * @return true if active, false if not
	 */
	boolean isActive();
	
	/**
	 * Gets this message to show when this repository is inactive. This will usually be an
	 * error message from when the system previously tried to activate this repository.
	 * 
	 * @return the inactive error message. Will be {@code null} when there is no message
	 */
	String getInactiveMessage();
}
