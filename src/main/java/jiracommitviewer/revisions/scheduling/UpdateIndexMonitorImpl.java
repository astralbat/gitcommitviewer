package jiracommitviewer.revisions.scheduling;

import com.atlassian.core.exception.InfrastructureException;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;

import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.Date;
import java.util.HashMap;

import jiracommitviewer.MultipleGitRepositoryManager;

public class UpdateIndexMonitorImpl implements UpdateIndexMonitor, LifecycleAware, DisposableBean {
	
	private static final String JOB_NAME = UpdateIndexMonitorImpl.class.getName() + ":job";
    private final static Logger logger = LoggerFactory.getLogger(UpdateIndexMonitorImpl.class);
	private final PluginScheduler pluginScheduler;
    private final MultipleGitRepositoryManager multipleGitRepositoryManager;

    private static final long DEFAULT_INDEX_INTERVAL = DateTimeConstants.MILLIS_PER_HOUR;

	public UpdateIndexMonitorImpl(final PluginScheduler pluginScheduler, final MultipleGitRepositoryManager multipleGitRepositoryManager) {
		this.pluginScheduler = pluginScheduler;
        this.multipleGitRepositoryManager =  multipleGitRepositoryManager;
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
                    put("UpdateIndexMonitorImpl:instance", UpdateIndexMonitorImpl.this);
                    put("MultipleSubversionRepositoryManager", multipleGitRepositoryManager);
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
