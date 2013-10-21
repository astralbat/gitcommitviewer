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
public class LogEntry<R extends AbstractRepository, K extends AbstractCommitKey<K>> {
	
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private K commitKey;
	private K parentCommitKey;
	private final String author;
	private final Date date;
	private final String message;
	private final List<CommitFile> committedFiles;
	private final R repository;
	private String branch;
	
	/**
	 * Creates a new log entry.
	 * 
	 * @param repository the repository where the log entry resides. Must not be {@code null}
	 * @param branch the branch on which the log entry resides. May be {@code null} if not specified or unknown
	 * @param commitKey the key for the commit. Must not be {@code null}
	 * @param parentCommitKey the parent of the commit. May be {@code null} if there is no parent
	 * @param author the author of the commit. Must not be {@code null}
	 * @param date the date of the commit. Must not be {@code null}
	 * @param message the commit message. Must not be {@code null}
	 * @param committedFiles the list of files commmitted during this commit. Must not be {@code null}
	 */
	public LogEntry(final R repository, final String branch, final K commitKey, final K parentCommitKey, final String author, 
			final Date date, final String message, final List<CommitFile> committedFiles) {
		Validate.notNull(repository, "repository must not be null");
		Validate.notNull(commitKey, "commitKey must not be null");
		Validate.notNull(author, "author must not be null");
		Validate.notNull(date, "date must not be null");
		Validate.notNull(message, "message must not be null");
		Validate.notNull(committedFiles, "committedFiles must not be null");
		
		this.repository = repository;
		this.branch = branch;
		this.commitKey = commitKey;
		this.parentCommitKey = parentCommitKey;
		this.author = author;
		this.date = date;
		this.message = message;
		this.committedFiles = committedFiles;
	}
	
	/**
	 * Gets the repository to which this log entry belongs.
	 * 
	 * @return the repository. Never {@code null}
	 */
	public R getRepository() {
		return repository;
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
	 * The author of the log entry.
	 * 
	 * @return the author. Never {@code null}
	 */
	public String getAuthor() {
		return author;
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
	 * Gets the message behind the log entry. The message may be surrounded by additional
	 * white space.
	 * 
	 * @return the message. Never {@code null}
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Gets a list of all files that have changed against this commit
	 * 
	 * @return the list of all changed files. Never {@code null}
	 */
	public List<CommitFile> getCommittedFiles() {
		return committedFiles;
	}
	
	/**
	 * Returns a sensible string representation of this log entry that's useful
	 * for debugging.
	 * 
	 * @return a string representation
	 */
	public String toString() {
		return commitKey.marshal() + "; " + dateFormatter.format(date) + "; " + author + "; " + message;
	}
}
