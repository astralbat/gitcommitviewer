package jiracommitviewer.action;

import jiracommitviewer.GitManager;
import jiracommitviewer.MultipleGitRepositoryManager;

@SuppressWarnings("serial")
public class ActivateGitRepositoryAction extends GitActionSupport {

	private Object repositoryId;
	private GitManager gitManager;

	public ActivateGitRepositoryAction(MultipleGitRepositoryManager manager) {
		super(manager);
	}

	public Object getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(Object repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String doExecute() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		gitManager = getMultipleRepoManager().getRepository(repositoryId);
		gitManager.activate();
		if (!gitManager.isActive()) {
			addErrorMessage(getText("git.repository.activation.failed", gitManager.getInactiveMessage()));
		}
		return SUCCESS;
	}

	public GitManager getSubversionManager() {
		return gitManager;
	}
}
