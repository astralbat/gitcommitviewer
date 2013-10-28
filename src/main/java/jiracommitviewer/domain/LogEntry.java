package jiracommitviewer.domain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Represents the log entry for a commit.
 * 
 * @author mark
 */
public class LogEntry<R extends AbstractRepository, K extends AbstractCommitKey<K>> extends AbstractLogEntry<R> {
	
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private K commitKey;
	private K parentCommitKey;
	private final Date date;
	private String branch;
	
	/**
	 * Creates a new log entry.
	 * 
	 * @param repository the repository where the log entry resides. Must not be {@code null}
	 * @param branch the branch on which the log entry resides. May be {@code null} if not specified or unknown
	 * @param commitKey the key for the commit. Must not be {@code null}
	 * @param parentCommitKey the parent of the commit. May be {@code null} if there is no parent
	 * @param authorName the name of the author of the commit. Must not be {@code null}
	 * @param date the date of the commit. Must not be {@code null}
	 * @param message the commit message. Must not be {@code null}
	 * @param commitFiles the list of files commmitted during this commit. Must not be {@code null}
	 */
	public LogEntry(final R repository, final String branch, final K commitKey, final K parentCommitKey, final String authorName, 
			final Date date, final String message, final List<CommitFile> commitFiles) {
		super(repository, authorName, message, commitFiles);
		
		Validate.notNull(date, "date must not be null");
		
		this.commitKey = commitKey;
		this.parentCommitKey = parentCommitKey;
		this.branch = branch;
		this.date = date;
	}
	
	/**
	 * Gets the commit key identifying the commit behind the log entry.
	 * 
	 * @return the commit. Never {@code null}
	 */
	public K getCommitKey() {
		return commitKey;
	}
	
	/**
	 * Gets the parent commit key if one is present.
	 * 
	 * @return the parent key key or {@code null} if this commit does not have a parent
	 */
	public K getParentCommitKey() {
		return parentCommitKey;
	}
	
	/**
	 * Gets the branch this commit was made on.
	 * 
	 * @return the branch. Will be {@code null} if the branch was not specified or is unknown
	 */
	public String getBranch() {
		return branch;
	}
	
	/**
	 * Sets the branch this commit was made on.
	 * 
	 * @param branch the branch. May be {@code null} if the branch is not known
	 */
	public void setBranch(final String branch) {
		this.branch = branch;
	}
	
	/**
	 * Gets the date and time when this log entry was made.
	 * 
	 * @return the date and time. Never {@code null}
	 */
	public Date getDate() {
		return date;
	}
	
	/**
	 * Returns a sensible string representation of this log entry that's useful
	 * for debugging.
	 * 
	 * @return a string representation
	 */
	public String toString() {
		return getCommitKey().marshal() + "; " + dateFormatter.format(date) + "; " + getAuthorName() + "; " + getMessage();
	}
}
