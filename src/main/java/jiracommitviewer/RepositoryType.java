package jiracommitviewer;

import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.GitRepository;

import org.apache.commons.lang3.Validate;

/**
 * An enumeration of all repository types supported.
 * 
 * @author mark
 */
public enum RepositoryType {

	GIT;
	
	/**
	 * Gets the {@code RepositoryType} from the specified {@code repository}.
	 * 
	 * @param repository the repository to get the type for. Must not be {@code null}
	 * @return the type. Never {@code null}
	 * @throws IllegalArgumentException if the supplied {@code repository} is not supported
	 */
	public static RepositoryType fromRepository(final AbstractRepository repository) {
		Validate.notNull(repository, "repository must not be null");
		
		if (repository instanceof GitRepository) {
			return GIT;
		}
		throw new IllegalArgumentException("Unsupported repository: " + repository.getClass().getName());
	}
}
