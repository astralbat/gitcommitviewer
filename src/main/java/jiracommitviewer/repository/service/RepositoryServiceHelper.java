package jiracommitviewer.repository.service;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.GitRepository;

/**
 * Simple helper class to support the {@link RepositoryService} and its subtypes.
 * 
 * @author mark
 */
public class RepositoryServiceHelper {
	
	@Autowired
	private GitRepositoryService gitRepositoryService;

	/**
	 * Gets the repository service associated with the specified {@code repository}.
	 * 
	 * @param repository the repository whose service to get. Must not be {@code null}
	 * @return the repository service. Never {@code null}
	 */
	@SuppressWarnings("unchecked")
	public <T extends AbstractRepository> RepositoryService<T, ?> getRepositoryService(final T repository) {
		Validate.notNull(repository, "repository must not be null");
		
		if (repository instanceof GitRepository) {
			return (RepositoryService<T, ?>)gitRepositoryService;
		}
		throw new RuntimeException("Unable to find repository service for repository type: " + repository.getClass());
	}
}
