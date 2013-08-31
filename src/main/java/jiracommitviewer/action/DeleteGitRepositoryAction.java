package jiracommitviewer.action;

import jiracommitviewer.GitManager;
import jiracommitviewer.MultipleGitRepositoryManager;

@SuppressWarnings("serial")
public class DeleteGitRepositoryAction extends GitActionSupport {
	
	private Object repositoryId;
	private GitManager gitManager;

	public DeleteGitRepositoryAction(final MultipleGitRepositoryManager manager) {
		super(manager);
	}

	public Object getRepoId() {
		return repositoryId;
	}

	public void setRepositoryId(Object repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String doDefault() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		gitManager = getMultipleRepoManager().getRepository(repositoryId);
		return INPUT;
	}

	public String doExecute() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		getMultipleRepoManager().removeRepository(repositoryId);
		return getRedirect("ViewGitRepositories.jspa");
	}

	public GitManager getGitManager() {
		return gitManager;
	}
}
