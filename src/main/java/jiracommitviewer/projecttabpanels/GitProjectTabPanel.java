package jiracommitviewer.projecttabpanels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.index.GitCommitIndexer;
import jiracommitviewer.index.exception.IndexException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import webwork.action.ActionContext;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.projectpanel.ProjectTabPanel;
import com.atlassian.jira.plugin.projectpanel.impl.AbstractProjectTabPanel;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.webresource.WebResourceManager;

/**
 * This class provides a tab panel for the JIRA project view.
 */
public class GitProjectTabPanel extends AbstractProjectTabPanel implements ProjectTabPanel {
    
    /** A special value for the &quot;selectedVersion&quot; request parameter that tells this panel
     * that it should return all commits for <em>all</em> issues in all versions. */
    public static final int ALL_VERSIONS = -1;
    /** The initial number of commits to show initially. */
    public static final int NUMBER_OF_REVISIONS = 100;
    /** The flag that indicates if archived versions should be considered when rendering commits. Currently
     * set to <tt>false</tt>. */
    public static final boolean INCLUDE_ARCHIVED_VERSIONS = false;
    
    private static final Logger logger = LoggerFactory.getLogger(GitProjectTabPanel.class);

    @Autowired
    private VersionManager versionManager;
    @Autowired
    private PermissionManager permissionManager;
    @Autowired
    private WebResourceManager webResourceManager;
    @Autowired
    private GitCommitIndexer gitCommitIndexer;

    /**
     * Instantiates a new instance.
     *
     * @param authenticationContext The authentication context.
     */
    public GitProjectTabPanel(final JiraAuthenticationContext authenticationContext) {
        super(authenticationContext);
    }
    
    public String getHtml(final BrowseContext browseContext) {
        if (logger.isDebugEnabled()) {
            logger.debug("Rendering commits for " + browseContext.getProject().getKey());
        }
        
        webResourceManager.requireResource("jiracommitviewer.jiracommitviewer:git-resource-js");

        final Map<String, Object> startingParams = new HashMap<String, Object>();
        final Project project = browseContext.getProject();
        final String key = project.getKey();
        final User user = browseContext.getUser();
        
        startingParams.put("action", authenticationContext.getI18nHelper());
        startingParams.put("project", project);
        startingParams.put("projectKey", key);

        // Get selected versionNumber, if any
        startingParams.put("versionManager", versionManager);
        final long versionNumber = getVersionRequestParameter();
        Version version = null;
        if (versionNumber != ALL_VERSIONS) {
            // The reason for the cast is Velocity's intelligence. It can't do Long comparisons.
            startingParams.put("versionNumber", (int) versionNumber);
            version = versionManager.getVersion(versionNumber);
            startingParams.put("selectedVersion", version);
        }

        // Get the list of recently updated issues and add it to the velocity context
        final int pageSize = getPageSizeRequestParameter();
        List<GitProjectCommitAction> recentCommits = getRecentCommits(key, version, user, 
        		getPageRequestParameter() * pageSize, pageSize);

        if (recentCommits.size() > 0 && recentCommits.size() == pageSize) {
            startingParams.put("moreAvailable", true);
            recentCommits = recentCommits.subList(0, pageSize);
        }

        startingParams.put("commits", recentCommits);

        // Get all versions. Used for the "Select versionNumber" drop-down list
        startingParams.put("releasedVersions", versionManager.getVersionsReleased(project.getId(), INCLUDE_ARCHIVED_VERSIONS));
        startingParams.put("unreleasedVersions", versionManager.getVersionsUnreleased(project.getId(), INCLUDE_ARCHIVED_VERSIONS));
        startingParams.put("stringUtils", new StringUtils());

        // Merge with velocity template and return HTML.
        return descriptor.getHtml("view", startingParams);
    }

    /**
     * Looks up the latest commits for the curently selected project in each of the repositories.
     *
     * @param key the JIRA project key of the currently selected project. Must not be {@code null}
     * @param version the JIRA project version to get commits for. If this is {@code null}, the latest commits for the project as a 
     * whole are returned instead.
     * @param user the remote user &mdash; we need to check that the user has "View Version Control" permission for an issue
     * before we show a commit for it. Must not be {@code null}
     * @param startIndex for paging &mdash; the index of the entry that is the first result in the page desired. The first page is at 0
     * @param pageSize for paging &mdash; the size of the page. Must be > 0
     * @return a {@link java.util.List} of {@link GitProjectCommitAction} objects, each of which holds a valid {@link LogEntry}. Never
     * {@code null}
     */
    private List<GitProjectCommitAction> getRecentCommits(final String key, final Version version, final User user, final int startIndex, 
    		final int pageSize) {
    	assert key != null : "key must not be null";
    	assert user != null : "user must not be null";
    	assert startIndex >= 0 : "startIndex must be >= 0";
    	assert pageSize > 0 : "pageSize must be > 0";
    	
        if (logger.isDebugEnabled()) {
            logger.debug("Getting recent commits for project " + key + " and version " + version);
        }

        final List<GitProjectCommitAction> actions = new ArrayList<GitProjectCommitAction>();

        try {
            List<LogEntry<GitRepository, GitCommitKey>> logEntries;

            if (version == null) {
                logEntries = gitCommitIndexer.getAllLogEntriesByProject(key, user, startIndex, pageSize, false);
            } else {
                logEntries = gitCommitIndexer.getAllLogEntriesByVersion(version, user, startIndex, pageSize, false);
            }

            if (logEntries.size() > 0) {
                for (final LogEntry<GitRepository, GitCommitKey> logEntry : logEntries) {
                	actions.add(createProjectCommitAction(logEntry));
                }
            }
        } catch (final IndexException ie) {
            logger.error("There' a problem with the index.", ie);
        }
        return actions;
    }

    /**
     * Creates the action for showing the commit on the panel.
     * 
     * @param logEntry the log entry to show. Must not be {@code null}
     * @return the action. Never {@code null}
     */
    private GitProjectCommitAction createProjectCommitAction(final LogEntry<GitRepository, GitCommitKey> logEntry) {
    	assert logEntry != null : "logEntry must not be null";
    	
        return new GitProjectCommitAction(logEntry, descriptor);
    }

    /**
     * Extracts the {@code selectedVersion} parameter from the HTTP request.
     * The versions are selected by a drop-down list on the Git commit tab.
     *
     * @return a Long containing the parameter value, or {@code null} if the parameter was not set or an error occurred 
     * while parsing the parameter.
     */
    private long getVersionRequestParameter() {
        final HttpServletRequest request = ActionContext.getRequest();

        if (request != null) {
            String selectedVersion = request.getParameter("selectedVersion");
            if (StringUtils.isNotBlank(selectedVersion)) {
                try {
                    return Long.parseLong(selectedVersion);
                } catch (final NumberFormatException e) {
                    logger.error("Unknown version string: " + selectedVersion, e);
                }
            }
        }
        return 0;
    }

    /**
     * Gets the page number being requested.
     * 
     * @return the requested page number where the first page is 0
     */
    private int getPageRequestParameter() {
        final HttpServletRequest req = ActionContext.getRequest();

        if (req != null) {
            String pageIndexString = req.getParameter("pageIndex");
            return StringUtils.isBlank(pageIndexString) ? 0 : Integer.parseInt(pageIndexString);
        }
        return 0;
    }

    /**
     * Gets the page size being requested.
     * 
     * @return the page size or the default value of 100 if no page size requested
     */
    private int getPageSizeRequestParameter() {
        final HttpServletRequest req = ActionContext.getRequest();

        if (req != null) {
            final String pageIndexString = req.getParameter("pageSize");
            return StringUtils.isBlank(pageIndexString) ? NUMBER_OF_REVISIONS : Integer.parseInt(pageIndexString);
        }
        return NUMBER_OF_REVISIONS;
    }

    /**
     * Gets whether to show the panel at all. The panel will be display if the user has permissions to view version control.
     */
    @SuppressWarnings("deprecation")
	@Override
    public boolean showPanel(final BrowseContext browseContext) {
    	Validate.notNull(browseContext, "browseContext must not be null");
    	
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, browseContext.getProject(), browseContext.getUser());
    }
}
