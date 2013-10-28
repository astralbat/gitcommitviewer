package jiracommitviewer.repository.service;

import jiracommitviewer.domain.Commit;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.repository.exception.RepositoryException;

/**
 * Services Git repositories.
 * 
 * @author mark
 */
public interface GitRepositoryService extends RepositoryService<GitRepository, GitCommitKey> {

	/**
	 * Clones the repository. If the repository has already been cloned then this will do nothing.
	 * 
	 * @param gitRepository the repository to clone. Must not be {@code null}
	 * @throws RepositoryException if an error occurs whilst cloning
	 */
	void cloneRepository(GitRepository gitRepository) throws RepositoryException;
	
	/**
	 * Fetches all the latest commits from the repository.
	 * 
	 * @param gitRepository the repository whose existant clone to fetch commits. Must not be {@code null}
	 * @throws RepositoryException if an error occurs whilst fetching
	 */
	void fetch(GitRepository gitRepository) throws RepositoryException;
	
	/**
	 * Tests whether a clone exists for the given {@code repository}.
	 * 
	 * @param repository the repository to check for. Must not be {@code null}
	 * @return true if there is a clone for this repository
	 * @throws RepositoryException if an error occurs whilst checking
	 */
	boolean isCloned(GitRepository repository) throws RepositoryException;
	
	/**
	 * Creates a new, non-bare repository at the location specified by the URI within {@code repository}, creates a new
	 * master branch and checks it out.
	 * <p>
	 * Only the file:// scheme is supported for creating new reposotories.
	 * 
	 * @param repository the repository to create. Must not be {@code null}
	 */
	void create(final GitRepository repository);
	
	/**
	 * Makes a commit to the repository from the specified {@code logEntry}.
	 * <p>
	 * Paths are added, modified and removed by inspecting the {@code logEntry}'s committed files.
	 * 
	 * @param repository the repository to commit to. Must not be {@code null}
	 * @param commit the commit with files to commit. Must not be {@code null}
	 * @throws RepositoryException if an error occurs whilst committing
	 */
	void commit(final GitRepository repository, final Commit<GitRepository> commit) throws RepositoryException;
}
