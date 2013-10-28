package jiracommitviewer.domain;

import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Represents a log entry which may be used to commit files or represent a past commit.
 * 
 * @author mark
 *
 * @param <R>
 * @param <K>
 */
public abstract class AbstractLogEntry<R extends AbstractRepository> {

	private final String authorName;
	private final String message;
	private final List<CommitFile> commitFiles;
	private final R repository;
	
	/**
	 * Creates a new log entry.
	 * 
	 * @param repository the repository where the log entry resides. Must not be {@code null}
	 * @param branch the branch on which the log entry resides. May be {@code null} if not specified or unknown
	 * @param authorName the name of the author of the commit. Must not be {@code null}
	 * @param date the date of the commit. Must not be {@code null}
	 * @param message the commit message. Must not be {@code null}
	 * @param commitFiles the list of files commmitted during this commit. Must not be {@code null}
	 */
	public AbstractLogEntry(final R repository, final String authorName, final String message, final List<CommitFile> commitFiles) {
		Validate.notNull(repository, "repository must not be null");
		Validate.notNull(authorName, "author must not be null");
		Validate.notNull(message, "message must not be null");
		Validate.notNull(commitFiles, "committedFiles must not be null");
		
		this.repository = repository;
		this.authorName = authorName;
		this.message = message;
		this.commitFiles = commitFiles;
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
	 * The author of the log entry.
	 * 
	 * @return the author. Never {@code null}
	 */
	public String getAuthorName() {
		return authorName;
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
	public List<CommitFile> getCommitFiles() {
		return commitFiles;
	}
	
	/**
	 * Returns a sensible string representation of this log entry that's useful
	 * for debugging.
	 * 
	 * @return a string representation
	 */
	public String toString() {
		return authorName + "; " + message;
	}
}
