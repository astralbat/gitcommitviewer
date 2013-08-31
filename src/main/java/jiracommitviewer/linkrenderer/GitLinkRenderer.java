/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 1:44:30 PM
 */
package jiracommitviewer.linkrenderer;

import jiracommitviewer.repository.CommitEntryPath;
import jiracommitviewer.repository.LogEntry;

/**
 * Service that renders HTML links.
 * 
 * @author mark
 */
public interface GitLinkRenderer {
	
    String getRevisionLink(LogEntry revision);

    String getChangePathLink(CommitEntryPath changePath);
}