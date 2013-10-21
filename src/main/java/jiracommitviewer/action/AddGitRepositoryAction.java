package jiracommitviewer.action;

import java.io.File;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.RepositoryType;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LinkFormatterType;
import jiracommitviewer.repository.exception.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.util.TextUtils;

/**
 * JIRA action for adding a new Git repository.
 * 
 * @author mark
 */
@SuppressWarnings("serial")
public class AddGitRepositoryAction extends GitActionSupport {
	
	private static final Logger logger = LoggerFactory.getLogger(AddGitRepositoryAction.class);
	
	private String repositoryUri;
	private String displayName;
	private String privateKeyFile;
	private String linkFormatType;
	private String changesetFormat;
	private String fileAddedFormat;
	private String fileModifiedFormat;
	private String fileReplacedFormat;
	private String fileDeletedFormat;
	private String fileViewFormat;

	public AddGitRepositoryAction(final RepositoryManager manager) {
		super(manager);
	}

	public void doValidation() {
		if (!TextUtils.stringSet(getDisplayName())) {
			addError("displayName", getText("git.errors.you.must.specify.a.name.for.the.repository"));
		}

		validateRepositoryParameters();
	}

	/**
	 * Gets the Git connection string.
	 * 
	 * @return the Git connection string
	 */
	public String getRepositoryUri() {
		return repositoryUri;
	}

	/**
	 * Sets the Git connection string.
	 * 
	 * @param repositoryUri the Git connection string
	 */
	public void setRepositoryUri(final String repositoryUri) {
		this.repositoryUri = repositoryUri != null ? repositoryUri.trim() : repositoryUri;
	}

	/**
	 * Gets the name the user identifies this repository by.
	 * 
	 * @return the name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Sets the name the user wishes to identify this repository by.
	 * 
	 * @param displayName the name
	 */
	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}
	
	/**
	 * Gets the path to the private key file used for authenticating with the remote repository.
	 * 
	 * @return the local path to the private key
	 */
	public String getPrivateKeyFile() {
		return privateKeyFile;
	}
	
	/**
	 * Sets the local path to the private key file used for authenticating with the remote repository.
	 * 
	 * @param privateKeyFile the local path to the private key
	 */
	public void setPrivateKeyFile(final String privateKeyFile) {
		this.privateKeyFile = privateKeyFile;
	}
	
	/**
	 * Gets the link format to use for new files in the commit.
	 * 
	 * @return the link format for new files
	 */
	public String getFileAddedFormat() {
		return fileAddedFormat;
	}
	
	/**
	 * Sets the link format to use for new files in the commit.
	 * 
	 * @param fileAddedFormat the link format for new files
	 */
	public void setFileAddedFormat(final String fileAddedFormat) {
		this.fileAddedFormat = fileAddedFormat;
	}
	
	/**
	 * Gets the link format to use for entire changesets (all files in the commit).
	 * 
	 * @return the link format to use for changesets
	 */
	public String getChangesetFormat() {
		return changesetFormat;
	}
	
	/**
	 * Sets the link format to use for entire changesets (all files in the commit).
	 * 
	 * @param changesetFormat the link format to use for changesets
	 */
	public void setChangesetFormat(final String changesetFormat) {
		this.changesetFormat = changesetFormat;
	}
	
	/**
	 * Gets the link format to use for deleted files in the commit.
	 * 
	 * @return the link format for deleted files
	 */
	public String getFileDeletedFormat() {
		return fileDeletedFormat;
	}
	
	/**
	 * Sets the link format to use for deleted files in the commit.
	 * 
	 * @param fileDeletedFormat the link format for deleted files
	 */
	public void setFileDeletedFormat(final String fileDeletedFormat) {
		this.fileDeletedFormat = fileDeletedFormat;
	}
	
	/**
	 * Gets the link format to use for modified files in the commit.
	 * 
	 * @return the link format for modified files
	 */
	public String getFileModifiedFormat() {
		return fileModifiedFormat;
	}
	
	/**
	 * Sets the link format to use for modified files in the commit.
	 * 
	 * @param fileModifiedFormat the link format for modified files
	 */
	public void setFileModifiedFormat(final String fileModifiedFormat) {
		this.fileModifiedFormat = fileModifiedFormat;
	}
	
	/**
	 * Gets the link format to use for files that have been renamed in the commit.
	 * 
	 * @return the link format for renamed files
	 */
	public String getFileReplacedFormat() {
		return fileReplacedFormat;
	}
	
	/**
	 * Sets the link format to use for files that have been renamed in the commit.
	 * 
	 * @param fileReplacedFormat the link format for renamed files
	 */
	public void setFileReplacedFormat(final String fileReplacedFormat) {
		this.fileReplacedFormat = fileReplacedFormat;
	}
	
	/**
	 * Gets the link format for viewing files in the repository.
	 * 
	 * @return the link format for viewing files
	 */
	public String getFileViewFormat() {
		return fileViewFormat;
	}
	
	/**
	 * Sets the link format for viewing files in the repository.
	 * 
	 * @param fileViewFormat the link format for viewing files
	 */
	public void setFileViewFormat(final String fileViewFormat) {
		this.fileViewFormat = fileViewFormat;
	}
	
	/**
	 * Gets the link format type. E.g. CGit, GitWeb etc.
	 * 
	 * @return the link format type
	 */
	public String getLinkFormatType() {
		return linkFormatType;
	}
	
	/**
	 * Sets the link format type.
	 * 
	 * @param linkFormatType the link format type
	 */
	public void setLinkFormatType(final String linkFormatType) {
		this.linkFormatType = linkFormatType;
	}
	
	public String doExecute() {
        if (!hasPermissions()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		final GitRepository repository = (GitRepository)getRepositoryManager().createRepository(RepositoryType.GIT);
		try {
			repository.setDisplayName(displayName);
			if (!StringUtils.isEmpty(linkFormatType)) {
				repository.setLinkFormatterType(LinkFormatterType.findByDisplayName(linkFormatType));
			}
			repository.setPrivateKeyPath(privateKeyFile);
			repository.setUri(repositoryUri);
			repository.getLinkFormatter().setChangesetFormat(changesetFormat);
			repository.getLinkFormatter().setFileAddedFormat(fileAddedFormat);
			repository.getLinkFormatter().setFileDeletedFormat(fileDeletedFormat);
			repository.getLinkFormatter().setFileModifiedFormat(fileModifiedFormat);
			repository.getLinkFormatter().setFileReplacedFormat(fileReplacedFormat);
			repository.getLinkFormatter().setFileViewFormat(fileViewFormat);
		
			try {
				repositoryServiceHelper.getRepositoryService(repository).activate(repository);
			} catch (final RepositoryException re) {
				logger.error("Error activating repository: " + repository.getDisplayName(), re);
				addErrorMessage(re.getMessage());
				addErrorMessage(getText("admin.errors.occured.when.creating"));
				repositoryManager.removeRepository(repository.getId());
				return ERROR;
			}
		} finally {
			getRepositoryManager().saveRepository(repository);
		}

		return getRedirect("ViewGitRepositories.jspa");
	}

	/**
	 * Validates the repository parametesr before saving.
	 */
	private void validateRepositoryParameters() {
		if (!TextUtils.stringSet(getDisplayName())) {
			addError("displayName", getText("admin.errors.you.must.specify.a.name.for.the.repository"));
		}
		if (!TextUtils.stringSet(getRepositoryUri())) {
			addError("root", getText("admin.errors.you.must.specify.the.uri.of.the.repository"));
		}
		if (TextUtils.stringSet(getPrivateKeyFile()) && !new File(getPrivateKeyFile()).isFile()) {
			addError("privateKeyFile", getText("admin.errors.private.key.file.does.not.exist"));
		}
	}
}
