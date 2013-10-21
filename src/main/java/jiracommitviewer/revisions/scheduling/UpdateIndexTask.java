package jiracommitviewer.revisions.scheduling;

import java.util.Map;

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

    private final static Logger logger = LoggerFactory.getLogger(UpdateIndexTask.class);

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

            for (final GitRepository repository : repositoryManager.getRepositoryList(GitRepository.class)) {
            	gitCommitIndexer.index(repository);
            }
        } catch (final Exception e) {
            logger.error("Error indexing changes", e);
        }
    }
}
