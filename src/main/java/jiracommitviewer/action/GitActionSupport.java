package jiracommitviewer.action;

import jiracommitviewer.MultipleGitRepositoryManager;

import org.apache.commons.lang.StringEscapeUtils;

import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.velocity.htmlsafe.HtmlSafe;

/**
 * Base class for the Subversion plugins actions.
 */
@SuppressWarnings("serial")
public class GitActionSupport extends JiraWebActionSupport {

	private MultipleGitRepositoryManager multipleRepoManager;
//	private List webLinkTypes;

	public GitActionSupport(final MultipleGitRepositoryManager manager) {
		this.multipleRepoManager = manager;
	}

	protected MultipleGitRepositoryManager getMultipleRepoManager() {
		return multipleRepoManager;
	}

	public boolean hasPermissions() {
		return hasPermission(Permissions.ADMINISTER);
	}

	public String doDefault() {
		if (!hasPermissions()) {
			return PERMISSION_VIOLATION_RESULT;
		}

		return INPUT;
	}

//	public List getWebLinkTypes() throws IOException {
//		if (webLinkTypes == null) {
//			webLinkTypes = new ArrayList();
//			Properties properties = new Properties();
//			properties.load(getClass().getResourceAsStream("/weblinktypes.properties"));
//
//			String[] types = properties.getProperty("types", "").split(" ");
//			for (int i = 0; i < types.length; i++) {
//				webLinkTypes.add(new WebLinkType(
//								types[i],
//								properties.getProperty(types[i] + ".name", types[i]),
//								properties.getProperty(types[i] + ".view"),
//								properties.getProperty(types[i] + ".changeset"),
//								properties.getProperty(types[i] + ".file.added"),
//								properties.getProperty(types[i] + ".file.modified"),
//								properties.getProperty(types[i] + ".file.replaced"),
//								properties.getProperty(types[i] + ".file.deleted")
//				));
//			}
//		}
//		return webLinkTypes;
//	}

    @HtmlSafe
    public String escapeJavaScript(String javascriptUnsafeString) {
        return StringEscapeUtils.escapeJavaScript(javascriptUnsafeString);
    }
}
