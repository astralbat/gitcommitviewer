package jiracommitviewer.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.repository.exception.RepositoryException;

@SuppressWarnings("serial")
public class ActivateGitRepositoryAction extends GitActionSupport {
	
	private static final Logger logger = LoggerFactory.getLogger(ActivateGitRepositoryAction.class);

	private String repositoryId;
	private AbstractRepository repository;

	public ActivateGitRepositoryAction(final RepositoryManager manager) {
		super(manager);
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(final String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String doExecute() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		repository = getRepositoryManager().getRepository(repositoryManager.parseRepositoryId(repositoryId));
		try {
			repositoryServiceHelper.getRepositoryService(repository).activate(repository);
		} catch (final RepositoryException re) {
			logger.error("Error activating repository: " + repository.getDisplayName(), re);
			addErrorMessage(getText("git.repository.activation.failed", re.getMessage()));
		}
		return SUCCESS;
	}

	public AbstractRepository getRepository() {
		return repository;
	}
}
