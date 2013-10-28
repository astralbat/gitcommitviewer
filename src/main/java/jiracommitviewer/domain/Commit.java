package jiracommitviewer.domain;

import java.util.Arrays;

/**
 * Represents a commit yet to be made against a repository.
 * 
 * @author mark
 *
 * @param <R>
 * @param <K>
 */
public class Commit<R extends AbstractRepository> extends AbstractLogEntry<R> {

	/**
	 * Creates a new commit
	 * 
	 * @param repository the repository to commit to. Must not be {@code null}
	 * @param authorName the name of the author of the commit. Must not be {@code null}
	 * @param message the commit message. Must not be {@code null}
	 * @param commitFiles the list of files commmitted during this commit. Must not be {@code null}
	 */
	public Commit(final R repository, final String authorName, final String message, final CommitFile... commitFiles) {
		super(repository, authorName, message, Arrays.asList(commitFiles));
	}
}
