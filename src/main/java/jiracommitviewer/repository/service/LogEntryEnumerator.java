package jiracommitviewer.repository.service;

import jiracommitviewer.domain.AbstractCommitKey;
import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.repository.exception.RepositoryException;

/**
 * Enumerates log entries from the repository.
 * 
 * @author mark
 */
public interface LogEntryEnumerator<R extends AbstractRepository, K extends AbstractCommitKey<K>> {

	/**
	 * Returns {@code true} if there is another log entry.
	 * 
	 * @return {@code true} if there is another log entry
	 * @throws RepositoryException if an error occurs while accessing the repository
	 */
	boolean hasNext() throws RepositoryException;
	
	/**
	 * Gets the next log entry.
	 * 
	 * @return the next entry. Never {@code null}
	 * @throws NoSuchElementException if there are no further entries
	 * @throws RepositoryException if an error occurs while accessing the repository
	 */
	LogEntry<R, K> next() throws RepositoryException;
}
