package jiracommitviewer.revisions.scheduling;

import com.atlassian.sal.api.scheduling.PluginJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import jiracommitviewer.MultipleGitRepositoryManager;
import jiracommitviewer.revisions.CommitIndexer;

public class UpdateIndexTask implements PluginJob {

    private final static Logger logger = LoggerFactory.getLogger(UpdateIndexTask.class);

    @Override
    public void execute(Map<String, Object> jobDataMap) {

        final UpdateIndexMonitorImpl monitor = (UpdateIndexMonitorImpl) jobDataMap.get("UpdateIndexMonitorImpl:instance");
        final MultipleGitRepositoryManager multipleGitRepositoryManager = (MultipleGitRepositoryManager) jobDataMap.get("MultipleSubversionRepositoryManager");
        assert monitor != null;

        try {
            if (null == multipleGitRepositoryManager) {
                return; // Just return --- the plugin is disabled. Don't log anything.
            }

            CommitIndexer revisionIndexer = multipleGitRepositoryManager.getRevisionIndexer();
            if (revisionIndexer != null) {
                revisionIndexer.updateIndex();
            } else {
                logger.warn("Tried to index changes but SubversionManager has no revision indexer?");
            }
        } catch (Exception e) {
            logger.error("Error indexing changes: " + e);
        }
    }
}
