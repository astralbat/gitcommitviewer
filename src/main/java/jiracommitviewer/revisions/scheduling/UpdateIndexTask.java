package jiracommitviewer.revisions.scheduling;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.index.GitCommitIndexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.scheduling.PluginJob;

/**
 * Task that updates the repository indexes. This should be scheduled to execute frequently.
 * 
 * @author mark
 */
public class UpdateIndexTask implements PluginJob {

    private static final Logger logger = LoggerFactory.getLogger(UpdateIndexTask.class);
    private static final ReentrantLock lock = new ReentrantLock();

    @Override
    public void execute(Map<String, Object> jobDataMap) {
        final UpdateIndexMonitorImpl monitor = (UpdateIndexMonitorImpl)jobDataMap.get("UpdateIndexMonitorImpl:instance");
        final GitCommitIndexer gitCommitIndexer = (GitCommitIndexer)jobDataMap.get("GitCommitIndexer");
        final RepositoryManager repositoryManager = (RepositoryManager)jobDataMap.get("RepositoryManager");
        assert monitor != null;

        try {
            if (gitCommitIndexer == null) {
                return; // Just return --- the plugin is disabled. Don't log anything.
            }
            
            // The JIRA scheduler allows multiple jobs of the same type to run concurrently. We want to prevent this
            // as the indexer is not thread-safe.
            if (lock.tryLock()) {
            	try {
            		for (final GitRepository repository : repositoryManager.getRepositoryList(GitRepository.class)) {
            			gitCommitIndexer.index(repository);
            		}
            	} finally {
            		lock.unlock();
            	}
            }
        } catch (final Exception e) {
            logger.error("Error indexing changes", e);
        }
    }
}
