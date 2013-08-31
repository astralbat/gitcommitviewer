package jiracommitviewer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jiracommitviewer.revisions.CommitIndexer;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.core.exception.InfrastructureException;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class MultipleGitRepositoryManagerImpl implements MultipleGitRepositoryManager {
	
	/** The sub directory within the JIRA plugin index directory to home our repositories and indexes. */
	private static final String PLUGIN_INDEX_DIRECTORY = "gitRepositoryViewer";
	/** The List<String> plugin setting that stores all properties for all repositories. */
	private static final String SETTINGS_REPOSITORY_LIST = "repositoryList";
	
    private final static Logger log = LoggerFactory.getLogger(MultipleGitRepositoryManagerImpl.class);

    private CommitIndexer revisionIndexer;

    private final Map<Object, GitManager> managers;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final IndexPathManager indexPathManager;

    public MultipleGitRepositoryManagerImpl(
            final VersionManager versionManager,
            final IssueManager issueManager,
            final PermissionManager permissionManager,
            final ChangeHistoryManager changeHistoryManager,
            final PluginSettingsFactory pluginSettingsFactory,
            final IndexPathManager indexPathManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.indexPathManager = indexPathManager;
        
        managers = loadGitManagers();

        // create revision indexer once we know we have succeed initializing our repositories
        revisionIndexer = new CommitIndexer(this, versionManager, issueManager, permissionManager, changeHistoryManager, indexPathManager);
    }

    /**
     * Loads git managers from stored configuration.
     */
    Map<Object, GitManager> loadGitManagers() {
        return loadManagersFromJiraProperties();
    }

    /**
     * Creates all GitManagers from stored configuration.
     * 
     * @return the map of Git manager identifiers to managers. Never {@code null}
     */
    @SuppressWarnings("unchecked")
	private Map<Object, GitManager> loadManagersFromJiraProperties() {
        final PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();

        final Map<Object, GitManager> managers = new HashMap<Object, GitManager>();
        final List<Properties> allManagerSettings = (List<Properties>)pluginSettings.get(SETTINGS_REPOSITORY_LIST);
        for (final Properties managerSettings : allManagerSettings) {
        	managers.put(managerSettings.getProperty(GitManager.GIT_REPOSITORY_NAME), createManagerFromPropertySet(managerSettings));
        }
        return managers;
    }

    GitManager createManagerFromPropertySet(final Properties properties) {
        return new GitManagerImpl(this, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitManager createRepository(final String id) {
        GitManager gitManager = new GitManagerImpl(this, id);
        managers.put(id, gitManager);
        if (isIndexingRevisions()) {
            revisionIndexer.addRepository(gitManager);
        }

        return gitManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRepository(final Object id) {
    	Validate.notNull(id, "id must not be null");
    	
    	final GitManager gitManager = managers.get(id);
    	if (gitManager == null) {
    		log.warn("Attempt to remove repository that does not exist: " + id);
    		return;
    	}
		managers.remove(id);
		try {
			revisionIndexer.removeEntries(gitManager);
		} catch (final IndexException e) {
			log.error("Failed to remove entries from index for repository: " + id, e);
		} catch (final IOException e) {
			log.error("Failed to remove entries from index for repository: " + id, e);
		}
    }

    public boolean isIndexingRevisions() {
        return revisionIndexer != null;
    }

    public CommitIndexer getRevisionIndexer() {
        return revisionIndexer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<GitManager> getRepositoryList() {
        return Collections.unmodifiableCollection(managers.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitManager getRepository(final Object id) {
        return managers.get(id);
    }

    void startRevisionIndexer() {
        getRevisionIndexer().start();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public File getIndexPath() {
		return new File(indexPathManager.getPluginIndexRootPath() + File.separator + PLUGIN_INDEX_DIRECTORY);
	}
    
    /**
     * {@inheritDoc}
     */
    @Override
    public File getRepositoryPath(final Object id) {
    	return new File(getIndexPath(), id + File.separator + "repo");
    }

    public void start() {
        try {
            if (isIndexingRevisions()) {
                startRevisionIndexer();
            }
        } catch (InfrastructureException ie) {
            /* Log error, don't throw. Otherwise, we get SVN-234 */
            log.error("Error starting " + getClass(), ie);
        }
    }
}
