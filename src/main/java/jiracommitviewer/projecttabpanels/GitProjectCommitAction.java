package jiracommitviewer.projecttabpanels;

import java.util.Map;

import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.issuetabpanels.GitCommitAction;

import org.ofbiz.core.util.UtilMisc;

import com.atlassian.jira.plugin.projectpanel.ProjectTabPanelModuleDescriptor;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * One item in the 'Git Commits' project tab.
 *
 * This class extends {@link GitCommitAction} (basically, there is no issue to group by here,
 * and we need to use a ProjectTabPanelModuleDescriptor in stead of an IssueTabPanelModuleDescriptor)
 */
public class GitProjectCommitAction extends GitCommitAction {
	
    protected final ProjectTabPanelModuleDescriptor projectDescriptor;

    public GitProjectCommitAction(final LogEntry<GitRepository, GitCommitKey> logEntry, final ProjectTabPanelModuleDescriptor descriptor) {
        super(logEntry, null);
        this.projectDescriptor = descriptor;
    }

    public String getHtml(final JiraWebActionSupport webAction) {
        Map<String, Object> params = UtilMisc.toMap("webAction", webAction, "action", this);
        return descriptor.getHtml("view", params);
    }
}
