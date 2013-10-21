package jiracommitviewer.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.LinkFormatter;
import jiracommitviewer.repository.service.RepositoryServiceHelper;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.velocity.htmlsafe.HtmlSafe;

/**
 * Base class for the Subversion plugins actions.
 */
@SuppressWarnings("serial")
public class GitActionSupport extends JiraWebActionSupport {

	protected RepositoryManager repositoryManager;
	
	@Autowired
	protected RepositoryServiceHelper repositoryServiceHelper;
	
	private Map<String, LinkFormatter> webLinkTypes;

	public GitActionSupport(final RepositoryManager manager) {
		this.repositoryManager = manager;
	}

	protected RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}

	public boolean hasPermissions() {
		return isHasPermission(Permissions.ADMINISTER);
	}

	public String doDefault() {
		if (!hasPermissions()) {
			return PERMISSION_VIOLATION_RESULT;
		}

		return INPUT;
	}

	/**
	 * Lazy gets a map of link format types to formatters for formatting links for files within
	 * each commit.
	 * 
	 * @return the web link types. Never {@code null}
	 * @throws IOException if an error occurs while accessing the properties file
	 */
	public Map<String, LinkFormatter> getLinkFormatTypes() throws IOException {
		if (webLinkTypes == null) {
			webLinkTypes = new HashMap<String, LinkFormatter>();
			Properties properties = new Properties();
			properties.load(getClass().getResourceAsStream("/weblinktypes.properties"));

			final String[] types = properties.getProperty("types", "").split(" ");
			for (int i = 0; i < types.length; i++) {
				final LinkFormatter formatter = new LinkFormatter();
				formatter.setChangesetFormat(properties.getProperty(types[i] + ".changeset"));
				formatter.setFileAddedFormat(properties.getProperty(types[i] + ".file.added"));
				formatter.setFileDeletedFormat(properties.getProperty(types[i] + ".file.deleted"));
				formatter.setFileModifiedFormat(properties.getProperty(types[i] + ".file.modified"));
				formatter.setFileReplacedFormat(properties.getProperty(types[i] + ".file.replaced"));
				formatter.setFileViewFormat(properties.getProperty(types[i] + ".view"));
				webLinkTypes.put(properties.getProperty(types[i] + ".name"), formatter);
			}
		}
		return webLinkTypes;
	}

    @HtmlSafe
    public String escapeJavaScript(String javascriptUnsafeString) {
        return StringEscapeUtils.escapeJavaScript(javascriptUnsafeString);
    }
}
