package jiracommitviewer.action;

import jiracommitviewer.GitManager;
import jiracommitviewer.MultipleGitRepositoryManager;

import com.opensymphony.util.TextUtils;

@SuppressWarnings("serial")
public class AddGitRepositoryAction extends GitActionSupport {
	
	private String root;
	private String displayName;
	private String commitLinkFormat;
	private String modifiedFileLinkFormat;

	public AddGitRepositoryAction(final MultipleGitRepositoryManager manager) {
		super(manager);
	}

	public void doValidation() {
		if (!TextUtils.stringSet(getDisplayName())) {
			addError("displayName", getText("git.errors.you.must.specify.a.name.for.the.repository"));
		}

		validateRepositoryParameters();
	}

	/**
	 * Gets the SSH connection string.
	 * 
	 * @return the SSH connection string
	 */
	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root != null ? root.trim() : root;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getCommitLinkFormat() {
		return commitLinkFormat;
	}
	
	public void setCommitLinkFormat(String commitLinkFormat) {
		if (TextUtils.stringSet(commitLinkFormat)) {
			this.commitLinkFormat = commitLinkFormat;
		} else {
			this.commitLinkFormat = null;
		}
	}
	
	public String getModifiedFileLinkFormat() {
		return modifiedFileLinkFormat;
	}
	
	public void setModifiedFileLinkFormat(String modifiedFileLinkFormat) {
		if (TextUtils.stringSet(modifiedFileLinkFormat)) {
			this.modifiedFileLinkFormat = modifiedFileLinkFormat;
		} else {
			this.modifiedFileLinkFormat = null;
		}
	}
	
	public String doExecute() throws Exception {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		GitManager gitManager = getMultipleRepoManager().createRepository(displayName);
		if (!gitManager.isActive()) {
			addErrorMessage(gitManager.getInactiveMessage());
			addErrorMessage(getText("admin.errors.occured.when.creating"));
			getMultipleRepoManager().removeRepository(gitManager.getId());
			return ERROR;
		}

		return getRedirect("ViewGitRepositories.jspa");
	}

	// This is public for testing purposes
	public void validateRepositoryParameters() {
		if (!TextUtils.stringSet(getDisplayName()))
			addError("displayName", getText("git.errors.you.must.specify.a.name.for.the.repository"));
		if (!TextUtils.stringSet(getRoot()))
			addError("root", getText("admin.errors.you.must.specify.the.root.of.the.repository"));
	}
}
