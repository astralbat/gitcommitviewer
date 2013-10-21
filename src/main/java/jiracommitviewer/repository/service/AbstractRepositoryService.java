package jiracommitviewer.repository.service;

import java.io.File;

import jiracommitviewer.domain.AbstractCommitKey;
import jiracommitviewer.domain.AbstractRepository;

import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.jira.config.util.IndexPathManager;

/**
 * Base class for all types of repositories.
 * 
 * @author mark
 * @param <T>
 */
public abstract class AbstractRepositoryService<T extends AbstractRepository, K extends AbstractCommitKey<K>> implements RepositoryService<T, K> {
	
	/** The sub directory within the JIRA plugin index directory to home our repositories and indexes. */
	private static final String PLUGIN_INDEX_DIRECTORY = "gitCommitViewer";
	
	@Autowired
	private IndexPathManager indexPathManager;

	/**
     * {@inheritDoc}
     */
	@Override
    public File getIndexPath() {
		return new File(indexPathManager.getPluginIndexRootPath() + File.separator + PLUGIN_INDEX_DIRECTORY);
	}
}
