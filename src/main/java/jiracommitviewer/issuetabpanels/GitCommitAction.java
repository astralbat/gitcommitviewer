/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 16, 2004
 * Time: 2:00:52 PM
 */
package jiracommitviewer.issuetabpanels;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jiracommitviewer.domain.AddedCommitFile;
import jiracommitviewer.domain.CommitFile;
import jiracommitviewer.domain.DeletedCommitFile;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.domain.ModifiedCommitFile;
import jiracommitviewer.domain.RenamedCommitFile;
import jiracommitviewer.linkrenderer.GitLinkRenderer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;

import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.util.JiraKeyUtils;

/**
 * Represents a single commit file in the commit tab.
 * <p>
 * These actions are created from {@link GitCommitTabPanel}.
 */
public class GitCommitAction extends AbstractIssueAction {

    private final LogEntry<GitRepository, GitCommitKey> logEntry;
    protected final IssueTabPanelModuleDescriptor descriptor;

    public GitCommitAction(final LogEntry<GitRepository, GitCommitKey> logEntry, final IssueTabPanelModuleDescriptor descriptor) {
        super(descriptor);
        this.descriptor = descriptor;
        this.logEntry = new LogEntry<GitRepository, GitCommitKey>(logEntry.getRepository(), logEntry.getBranches(), logEntry.getCommitKey(), 
        		logEntry.getParentCommitKey(), logEntry.getAuthorName(), logEntry.getDate(), rewriteLogMessage(logEntry.getMessage()), 
        		logEntry.getCommitFiles(), logEntry.isMerge());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    protected void populateVelocityParams(@SuppressWarnings("rawtypes") Map params) {
        params.put("stringUtils", new StringUtils());
        params.put("git", this);
    }

    /**
     * Gets the renderer responsible for formatting links to be displayed in the view.
     * 
     * @return the link renderer. Never {@code null}
     */
    public GitLinkRenderer getLinkRenderer() {
        return new GitLinkRenderer(logEntry.getRepository().getLinkFormatter());
    }

    /**
     * Gets the repository's display name.
     * 
     * @return the repository's display name. Never {@code null}
     */
    public String getRepositoryDisplayName() {
        return logEntry.getRepository().getDisplayName();
    }

    /**
     * Gets the date of the commit.
     * 
     * @return the date. Never {@code null}
     */
    public Date getTimePerformed() {
        if (logEntry.getDate() == null) {
            throw new UnsupportedOperationException("no revision date for this log entry");
        }
        return logEntry.getDate();
    }

    /**
     * Gets the log entry for this commit.
     * 
     * @return the log entry. Never {@code null}
     */
    public LogEntry<GitRepository, GitCommitKey> getLogEntry() {
        return logEntry;
    }

    /**
     * Gets whether the specified {@code commitFile} is of type add.
     * 
     * @param commitFile the commit file to test. Must not be {@code null}
     * @return true if and only if the commit file is of type add
     */
    public boolean isAdded(final CommitFile commitFile) {
    	Validate.notNull(commitFile, "commitFile must not be null");
    	
        return commitFile instanceof AddedCommitFile;
    }

    /**
     * Gets whether the specified {@code commitFile} is of type modified.
     * 
     * @param commitFile the commit file to test. Must not be {@code null}
     * @return true if and only if the commit file is of type modified
     */
    public boolean isModified(final CommitFile commitFile) {
    	Validate.notNull(commitFile, "commitFile must not be null");
    	
        return commitFile instanceof ModifiedCommitFile;
    }

    /**
     * Gets whether the specified {@code commitFile} is of type rename.
     * 
     * @param commitFile the commit file to test. Must not be {@code null}
     * @return true if and only if the commit file is of type rename
     */
    public boolean isReplaced(final CommitFile commitFile) {
    	Validate.notNull(commitFile, "commitFile must not be null");
    	
        return commitFile instanceof RenamedCommitFile;
    }

    /**
     * Gets whether the specified {@code commitFile} is of type delete.
     * 
     * @param commitFile the commit file to test. Must not be {@code null}
     * @return true if and only if the commit file is of type delete
     */
    public boolean isDeleted(final CommitFile commitFile) {
    	Validate.notNull(commitFile, "commitFile must not be null");
    	
        return commitFile instanceof DeletedCommitFile;
    }

    /**
     * Converts all lower case JIRA issue keys to upper case so that they can be
     * correctly rendered in the Velocity macro, makelinkedhtml.
     *
     * @param logMessageToBeRewritten
     * The SVN log message to be rewritten.
     * @return
     * The rewritten SVN log message.
     * @see
     * <a href="http://jira.atlassian.com/browse/SVN-93">SVN-93</a>
     */
    private String rewriteLogMessage(final String logMessageToBeRewritten) {
        String logMessage = logMessageToBeRewritten;
        final String logMessageUpperCase = StringUtils.upperCase(logMessage);
        final Set <String>issueKeys = new HashSet<String>(getIssueKeysFromCommitMessage(logMessageUpperCase));

        for (final String issueKey : issueKeys) {
            logMessage = logMessage.replaceAll("(?ium)" + issueKey, issueKey);
        }

        return logMessage;
    }

    /**
     * Gets a list of all issue keys mentioned with in the specified {@code logMessageUpperCase}.
     * 
     * @param logMessageUpperCase the uppercase log message. Must not be {@code null}
     * @return the list of issue keys found. Never {@code null}
     */
    private List<String> getIssueKeysFromCommitMessage(final String logMessageUpperCase) {
    	assert logMessageUpperCase != null : "logMessageUpperCase must not be null";
    	
        return JiraKeyUtils.getIssueKeysFromString(logMessageUpperCase);
    }
}
