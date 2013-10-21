package jiracommitviewer.repository.service;

import java.io.File;

import jiracommitviewer.domain.AbstractCommitKey;
import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.repository.exception.RepositoryException;

/**
 * Base service class for manipulating the repository.
 * 
 * @author mark
 * @param <T>
 */
public interface RepositoryService<R extends AbstractRepository, K extends AbstractCommitKey<K>> {

	/**
	 * Attempts to active the repository. If the repository cannot be activated for any reason
	 * then the {@code repository} will be set as inactive.
	 * 
	 * @param repository the repository to try to activate. Must not be {@code null}
	 * @throws RepositoryException if an error occurs
	 */
	void activate(final R repository) throws RepositoryException;
	
	/**
     * Gets the top-level directory at which to store the indexes, and if applicable, cloned repositories.
     * <p>
     * The structure is as follows:
     * <pre>
     * /{id}/repo
     * /indexes
     * </pre>
     * 
     * @return the index path. Never {@code null}
     */
	File getIndexPath();
	
	/**
	 * Gets an iterator of log entries from the repository that are after, in sequence, the specified
	 * {@code gitCommitKey}.
	 * 
	 * @param repository the repository. Must not be {@code null}
	 * @param commitKey the commit key. When {@code null}, all log entries are returned from the beginning
	 * @return an enumerator for reading log entries sequentially. Never {@code null}
	 * @throws RepositoryException if {@code commitKey} is not {@code null} and the identified commit does not exist; or
	 * if there is an error while accessing the repository
	 */
	LogEntryEnumerator<R, K> getLogEntries(final R repository, K commitKey) throws RepositoryException;
	
	/**
	 * Gets the log entry for the commit specified by {@code commitKey}.
	 * 
	 * @param repository the repository to get the log entry for. Must not be {@code null}
	 * @param commitKey the key that identifies the commit whose log entry to get. Must not be {@code null}
	 * @return the log entry. Never {@code null}
	 * @throws RepositoryException if {@code commitKey} is not {@code null} and the identified commit does not exist; or
	 * if there is an error while accessing the repository
	 */
	LogEntry<R, K> getLogEntry(final R repository, K commitKey) throws RepositoryException;
}
