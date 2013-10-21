/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 1:44:30 PM
 */
package jiracommitviewer.linkrenderer;

import jiracommitviewer.domain.AbstractCommitKey;
import jiracommitviewer.domain.AbstractPathCommitFile;
import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.AddedCommitFile;
import jiracommitviewer.domain.CopiedCommitFile;
import jiracommitviewer.domain.DeletedCommitFile;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.domain.ModifiedCommitFile;
import jiracommitviewer.domain.RenamedCommitFile;

/**
 * Service that renders HTML links.
 * 
 * @author mark
 */
public interface LinkRenderer<R extends AbstractRepository, K extends AbstractCommitKey<K>> {
	
	/**
	 * Gets the link for the entire changeset.
	 * 
	 * @param logEntry the log entry of the changeset to render. Must not be {@code null}
	 * @return the link. Never {@code null}
	 */
    String getChangesetLink(LogEntry<R, K> logEntry);
    
    /**
     * Gets the link for the added {@code commitFile}.
     * 
     * @param logEntry the log entry of the changeset. Must not be {@code null}
     * @param the added commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileAddedLink(LogEntry<R, K> logEntry, AddedCommitFile commitFile);
    
    /**
     * Gets the link for the modified {@code commitFile}.
     * 
     * @param logEntry the log entry of the changset. Must not be {@code null}
     * @param the modified commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileModifiedLink(LogEntry<R, K> logEntry, ModifiedCommitFile commitFile);
    
    /**
     * Gets the link for the replaced (destination file) {@code commitFile}.
     * 
     * @param logEntry the log entry of the changeset. Must not be {@code null}
     * @param the replaced commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileRenamedLink(LogEntry<R, K> logEntry, RenamedCommitFile commitFile);
    
    /**
     * Gets the link for the renamed (source file) {@code commitFile}.
     * 
     * @param logEntry the log entry of the changeset. Must not be {@code null}
     * @param commitFile the renamed commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileReplacedLink(LogEntry<R, K> logEntry, RenamedCommitFile commitFile);
    
    /**
     * Gets the link for the deleted {@code commitFile}.
     * 
     * @param logEntry the log entry of the changeset. Must not be {@code null}
     * @param commitFile the deleted commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileDeletedLink(LogEntry<R, K> logEntry, DeletedCommitFile commitFile);

    /**
     * Gets the link for the copied (source file) {@code commitFile}.
     * 
     * @param logEntry the log entry of the changeset. Must not be {@code null}
     * @param commitFile the replaced commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileCopiedLink(LogEntry<R, K> logEntry, CopiedCommitFile commitFile);
    
    /**
     * Gets the link for the copied (source file) {@code commitFile}.
     * 
     * @param logEntry the log entry of the changeset. Must not be {@code null}
     * @param commitFile the copied commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileReplacedLink(LogEntry<R, K> logEntry, CopiedCommitFile commitFile);
    
    /**
     * Gets the link to view the {@code commitFile}.
     * 
     * @param logEntry the log entry of the changeset. Must not be {@code null}
     * @param commitFile the commit file to render. Must not be {@code null}
     * @return the link. Never {@code null}
     */
    String getFileViewLink(LogEntry<R, K> logEntry, AbstractPathCommitFile commitFile);
}