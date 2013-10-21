package jiracommitviewer.action;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LinkFormatterType;
import jiracommitviewer.repository.exception.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class UpdateGitRepositoryAction extends AddGitRepositoryAction {
	
	private static final Logger logger = LoggerFactory.getLogger(UpdateGitRepositoryAction.class);
	
	private String repositoryId;

	public UpdateGitRepositoryAction(final RepositoryManager repositoryManager) {
		super(repositoryManager);
	}

	public String doDefault() {
		if (ERROR.equals(super.doDefault())) {
			return ERROR;
		}

        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

        if (repositoryId == null) {
			addErrorMessage(getText("git.repository.id.missing"));
			return ERROR;
		}

		// Retrieve the repository
		final GitRepository repository = repositoryManager.getRepository(repositoryManager.parseRepositoryId(repositoryId), 
				GitRepository.class);
		if (repository == null) {
			addErrorMessage(getText("git.repository.does.not.exist", repositoryId.toString()));
			return ERROR;
		}

		this.setDisplayName(repository.getDisplayName());
		this.setRepositoryUri(repository.getUri());
		this.setPrivateKeyFile(repository.getPrivateKeyPath() == null ? "" : repository.getPrivateKeyPath().getPath());
        this.setFileAddedFormat(repository.getLinkFormatter().getFileAddedFormat());
        this.setChangesetFormat(repository.getLinkFormatter().getChangesetFormat());
        this.setFileDeletedFormat(repository.getLinkFormatter().getFileDeletedFormat());
        this.setFileModifiedFormat(repository.getLinkFormatter().getFileModifiedFormat());
        this.setFileReplacedFormat(repository.getLinkFormatter().getFileReplacedFormat());
        this.setFileViewFormat(repository.getLinkFormatter().getFileViewFormat());
        if (repository.getLinkFormatterType() != null) {
        	this.setLinkFormatType(repository.getLinkFormatterType().getDisplayName());
        }
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

		final GitRepository repository = repositoryManager.getRepository(repositoryManager.parseRepositoryId(repositoryId), 
				GitRepository.class);
		try {
			repository.setDisplayName(getDisplayName());
			if (!StringUtils.isEmpty(getLinkFormatType())) {
				repository.setLinkFormatterType(LinkFormatterType.findByDisplayName(getLinkFormatType()));
			}
			repository.setPrivateKeyPath(getPrivateKeyFile());
			repository.setUri(getRepositoryUri());
			repository.getLinkFormatter().setChangesetFormat(getChangesetFormat());
			repository.getLinkFormatter().setFileAddedFormat(getFileAddedFormat());
			repository.getLinkFormatter().setFileDeletedFormat(getFileDeletedFormat());
			repository.getLinkFormatter().setFileModifiedFormat(getFileModifiedFormat());
			repository.getLinkFormatter().setFileReplacedFormat(getFileReplacedFormat());
			repository.getLinkFormatter().setFileViewFormat(getFileViewFormat());
			
			try {
				repositoryServiceHelper.getRepositoryService(repository).activate(repository);
			} catch (final RepositoryException re) {
				logger.error("Error activating repository: " + repository.getDisplayName(), re);
				repositoryId = String.valueOf(repository.getId());
				addErrorMessage(re.getMessage());
				addErrorMessage(getText("admin.errors.occured.when.updating"));
				return ERROR;
			}
		} finally {
			getRepositoryManager().saveRepository(repository);
		}
		return getRedirect("ViewGitRepositories.jspa");
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(final String repositoryId) {
		this.repositoryId = repositoryId;
	}
}
