
package jiracommitviewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.index.CommitIndexer;
import jiracommitviewer.index.exception.IndexException;
import jiracommitviewer.repository.exception.RepositoryException;
import jiracommitviewer.repository.service.RepositoryServiceHelper;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Manager for collecting repositories and providing access methods for them.
 * 
 * @author mark
 */
public class DefaultRepositoryManager implements RepositoryManager {
	
	/** The property that identifies a repository's type. */
	private static final String PROPERTY_TYPE = "type";

	/** The List<String> plugin setting that stores all properties for all repositories. */
	private static final String SETTINGS_REPOSITORY_LIST = "repositoryList";
	private static final String SETTINGS_REPOSITORY_VALUES = "repositoryValues";
	
    private final static Logger log = LoggerFactory.getLogger(DefaultRepositoryManager.class);

    @Autowired
    private CommitIndexer<GitRepository, GitCommitKey> gitRevisionIndexer;
    @Autowired
    private RepositoryServiceHelper repositoryServiceHelper;

    private final Map<Object, AbstractRepository> repositories = new HashMap<Object, AbstractRepository>();
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultRepositoryManager(
            final VersionManager versionManager,
            final IssueManager issueManager,
            final PermissionManager permissionManager,
            final ChangeHistoryManager changeHistoryManager,
            final PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        
        loadRepositories();
    }

    /**
     * Loads repositories from stored configuration.
     */
    private void loadRepositories() {
        loadRepositoriesFromJiraProperties();
    }

    /**
     * Creates all GitManagers from stored configuration.
     */
    @SuppressWarnings("unchecked")
	private void loadRepositoriesFromJiraProperties() {
        final PluginSettings globalSettings = pluginSettingsFactory.createGlobalSettings();

        List<String> repositoryList = (List<String>)globalSettings.get(SETTINGS_REPOSITORY_LIST);
        
        // New installation, settings don't exist yet
        if (repositoryList == null) {
        	repositoryList = new ArrayList<String>();
        	globalSettings.put(SETTINGS_REPOSITORY_LIST, repositoryList);
        }
        
        for (final String repositoryId : repositoryList) {
        	final PluginSettings repositorySettings = pluginSettingsFactory.createSettingsForKey(repositoryId);
        	createRepositoryFromPropertySet(parseRepositoryId(repositoryId), 
        			(Properties)repositorySettings.get(SETTINGS_REPOSITORY_VALUES));
        }
    }

    /**
     * Creates a repository from the saved {@code properties}.
     * 
     * @param properties the properties to create the repository from. Must not be {@code null}
     * @return the created repository. Never {@code null}
     */
    private AbstractRepository createRepositoryFromPropertySet(final Object repositoryId, final Properties properties) {
    	assert properties != null : "properties must not be null";
    	
        final AbstractRepository repository = createRepository(repositoryId, 
        		RepositoryType.valueOf(properties.getProperty(PROPERTY_TYPE)));
        repository.loadFromProperties(properties);
        return repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractRepository createRepository(final RepositoryType type) {
    	Validate.notNull(type, "type must not be null");
    	
    	return createRepository(UUID.randomUUID(), type);
    }
    
    /**
     * Creates a new empty repository.
     * 
     * @param repositoryId the repository identifier. Must not be {@code null}
     * @param type the type of repository to create. Must not be {@code null}
     * @return the newly created repository. Never {@code null}
     */
    private AbstractRepository createRepository(final Object repositoryId, final RepositoryType type) {
    	assert repositoryId != null : "repositoryId must not be null";
    	assert type != null : "type must not be null";
    	
    	final AbstractRepository repository;
    	if (type == RepositoryType.GIT) {
    		repository = new GitRepository(repositoryId);
    	} else {
    		throw new RuntimeException("Unhandled repository type: " + type);
    	}
        repositories.put(repository.getId(), repository);
        return repository;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveRepository(final AbstractRepository repository) {
    	Validate.notNull(repository, "repository must not be null");
    	
    	final Properties properties = new Properties();
    	properties.setProperty(PROPERTY_TYPE, RepositoryType.fromRepository(repository).name());
    	repository.saveToProperties(properties);
    	
    	// Save identifier within global settings
    	final PluginSettings globalSettings = pluginSettingsFactory.createGlobalSettings();
    	@SuppressWarnings("unchecked")
		final List<String> repositoryList = (List<String>)globalSettings.get(SETTINGS_REPOSITORY_LIST);
    	repositoryList.remove(String.valueOf(repository.getId()));
    	repositoryList.add(String.valueOf(repository.getId()));
    	globalSettings.put(SETTINGS_REPOSITORY_LIST, repositoryList);
    	
    	// Save actual values within keyed settings
    	final PluginSettings repositorySettings = pluginSettingsFactory.createSettingsForKey(String.valueOf(repository.getId()));
    	repositorySettings.put(SETTINGS_REPOSITORY_VALUES, properties);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    public void removeRepository(final Object id) {
    	Validate.notNull(id, "id must not be null");
    	
    	final AbstractRepository repository = repositories.get(id);
    	if (repository == null) {
    		log.warn("Attempt to remove repository that does not exist: " + id);
    		return;
    	}
    	
		repositories.remove(id);
		
		// Remove from global settings
		final PluginSettings globalSettings = pluginSettingsFactory.createGlobalSettings();
		final List<String> allManagerSettings = (List<String>)globalSettings.get(SETTINGS_REPOSITORY_LIST);
		allManagerSettings.remove(String.valueOf(id));
		globalSettings.put(SETTINGS_REPOSITORY_LIST, allManagerSettings);
    	// Wipe settings from keyed settings
    	final PluginSettings repositorySettings = pluginSettingsFactory.createSettingsForKey(String.valueOf(id));
    	final Properties properties = (Properties)repositorySettings.get(SETTINGS_REPOSITORY_VALUES);
    	if (properties != null) {
    		for (final Enumeration<?> e = properties.propertyNames(); e.hasMoreElements() ;) {
    			properties.remove(e.nextElement());
    		}
    	}
    	repositorySettings.put(SETTINGS_REPOSITORY_VALUES, properties);
		
		try {
			if (repository instanceof GitRepository) {
				gitRevisionIndexer.removeEntries((GitRepository)repository);
			}
		} catch (final IndexException ie) {
			log.error("Failed to remove entries from index for repository: " + id, ie);
		}
		
		try {
			repositoryServiceHelper.getRepositoryService(repository).remove(repository);
		} catch (final RepositoryException re) {
			log.error("Failed to remove persistent repository information: " + id, re);
		}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AbstractRepository> getRepositoryList() {
        return Collections.unmodifiableCollection(repositories.values());
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    public <T extends AbstractRepository> Collection<T> getRepositoryList(final Class<T> clazz) {
    	Validate.notNull(clazz, "clazz must not be null");
    	
    	final List<T> repositories = new ArrayList<T>();
    	for (final AbstractRepository repository : getRepositoryList()) {
    		if (repository.getClass().equals(clazz)) {
    			repositories.add((T)repository);
    		}
    	}
    	return Collections.unmodifiableList(repositories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractRepository getRepository(final Object id) {
        return repositories.get(id);
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    public <T extends AbstractRepository> T getRepository(final Object id, final Class<T> clazz) {
    	final AbstractRepository repo = repositories.get(id);
    	if (repo == null) {
    		return null;
    	}
    	return clazz.isAssignableFrom(repo.getClass()) ? (T)repo : null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseRepositoryId(final String id) {
    	Validate.notNull(id, "id must not be null");
    	
    	return UUID.fromString(id);
    }
}
