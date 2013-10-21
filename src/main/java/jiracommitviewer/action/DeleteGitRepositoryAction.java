package jiracommitviewer.action;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.AbstractRepository;

@SuppressWarnings("serial")
public class DeleteGitRepositoryAction extends GitActionSupport {
	
	private String repositoryId;
	private AbstractRepository repository;

	public DeleteGitRepositoryAction(final RepositoryManager manager) {
		super(manager);
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(final String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String doDefault() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		repository = repositoryManager.getRepository(repositoryManager.parseRepositoryId(repositoryId));
		return INPUT;
	}

	public String doExecute() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		repositoryManager.removeRepository(repositoryManager.parseRepositoryId(repositoryId));
		return getRedirect("ViewGitRepositories.jspa");
	}

	public AbstractRepository getRepository() {
		return repository;
	}
}
