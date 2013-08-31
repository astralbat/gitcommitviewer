package jiracommitviewer.action;

import jiracommitviewer.GitManager;
import jiracommitviewer.MultipleGitRepositoryManager;

@SuppressWarnings("serial")
public class UpdateGitRepositoryAction extends AddGitRepositoryAction {
	
	private Object repositoryId;

	public UpdateGitRepositoryAction(MultipleGitRepositoryManager multipleRepoManager) {
		super(multipleRepoManager);
	}

	public String doDefault() {
		if (ERROR.equals(super.doDefault()))
			return ERROR;

        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

        if (repositoryId == null) {
			addErrorMessage(getText("git.repository.id.missing"));
			return ERROR;
		}

		// Retrieve the repository
		final GitManager repository = getMultipleRepoManager().getRepository(repositoryId);
		if (repository == null) {
			addErrorMessage(getText("git.repository.does.not.exist", repositoryId.toString()));
			return ERROR;
		}

		this.setDisplayName(repository.getDisplayName());
        this.setCommitLinkFormat(repository.getCommitLinkFormat());
        this.setModifiedFileLinkFormat(repository.getModifiedFileLinkFormat());
		return INPUT;
	}

	public String doExecute() {
		if (!hasPermissions()) {
			addErrorMessage(getText("git.admin.privilege.required"));
			return ERROR;
		}

		if (repositoryId == null) {
			return getRedirect("ViewGitRepositories.jspa");
		}

		GitManager gitManager = getMultipleRepoManager().getRepository(repositoryId);
		if (!gitManager.isActive()) {
			repositoryId = gitManager.getId();
			addErrorMessage(gitManager.getInactiveMessage());
			addErrorMessage(getText("admin.errors.occured.when.updating"));
			return ERROR;
		}
		return getRedirect("ViewGitRepositories.jspa");
	}

	public Object getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(Object repositoryId) {
		this.repositoryId = repositoryId;
	}
}
