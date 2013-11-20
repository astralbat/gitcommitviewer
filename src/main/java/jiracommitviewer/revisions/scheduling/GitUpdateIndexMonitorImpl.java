package jiracommitviewer.revisions.scheduling;

import java.util.Date;
import java.util.HashMap;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.index.GitCommitIndexer;

import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.atlassian.core.exception.InfrastructureException;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;

public class GitUpdateIndexMonitorImpl implements UpdateIndexMonitor, LifecycleAware, DisposableBean {
	
	private static final String JOB_NAME = GitUpdateIndexMonitorImpl.class.getName() + ":job";
    private final static Logger logger = LoggerFactory.getLogger(GitUpdateIndexMonitorImpl.class);
    private final RepositoryManager repositoryManager;
	private final PluginScheduler pluginScheduler;
    private final GitCommitIndexer gitCommitIndexer;

    private static final long DEFAULT_INDEX_INTERVAL = DateTimeConstants.MILLIS_PER_MINUTE;

	public GitUpdateIndexMonitorImpl(final RepositoryManager repositoryManager, 
			final PluginScheduler pluginScheduler, final GitCommitIndexer gitCommitIndexer) {
		this.repositoryManager = repositoryManager;
		this.pluginScheduler = pluginScheduler;
        this.gitCommitIndexer =  gitCommitIndexer;
	}
	
	public void onStart() {
        schedule();
	}
	
	@SuppressWarnings("serial")
	public void schedule() {
		pluginScheduler.scheduleJob(
                JOB_NAME,
                UpdateIndexTask.class,
                new HashMap<String, Object>() {{
                    put("UpdateIndexMonitorImpl:instance", GitUpdateIndexMonitorImpl.this);
                    put("GitCommitIndexer", gitCommitIndexer);
                    put("RepositoryManager", repositoryManager);
                }},
                new Date(),
                DEFAULT_INDEX_INTERVAL);
        logger.info(String.format("UpdateIndexMonitorImpl scheduled to run every %dms", DEFAULT_INDEX_INTERVAL));
	}

    @Override
    public void destroy() {
        try {
            pluginScheduler.unscheduleJob(JOB_NAME);
        } catch (Exception e) {
            throw new InfrastructureException("Error unschedule update index job " + e);
        }
    }
}
