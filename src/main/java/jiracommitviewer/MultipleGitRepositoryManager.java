package jiracommitviewer;

import java.io.File;
import java.util.Collection;

import jiracommitviewer.revisions.CommitIndexer;

import com.atlassian.jira.extension.Startable;

/**
 * Main component of the Git plugin.
 */
public interface MultipleGitRepositoryManager extends Startable {
	
    String SVN_ROOT_KEY = "svn.root";
    String SVN_REPOSITORY_NAME = "svn.display.name";
    String SVN_USERNAME_KEY = "svn.username";
    String SVN_PASSWORD_KEY = "svn.password";
    String SVN_PRIVATE_KEY_FILE = "svn.privatekeyfile";
    String SVN_REVISION_INDEXING_KEY = "revision.indexing";
    String SVN_REVISION_CACHE_SIZE_KEY = "revision.cache.size";

    String SVN_LINKFORMAT_TYPE = "linkformat.type";
    String SVN_LINKFORMAT_CHANGESET = "linkformat.changeset";
    String SVN_LINKFORMAT_FILE_ADDED = "linkformat.file.added";
    String SVN_LINKFORMAT_FILE_MODIFIED = "linkformat.file.modified";
    String SVN_LINKFORMAT_FILE_REPLACED = "linkformat.file.replaced";
    String SVN_LINKFORMAT_FILE_DELETED = "linkformat.file.deleted";

    String SVN_LINKFORMAT_PATH_KEY = "linkformat.copyfrom";

    String SVN_LOG_MESSAGE_CACHE_SIZE_KEY = "logmessage.cache.size";
    
    /**
     * Gets the top-level directory at which to store the cloned repositories and indexes.
     * <p>
     * The structure is as follows:
     * <pre>
     * /{id}/repo
     * /{id}/indexes
     * </pre>
     * 
     * @return the index path. Never {@code null}
     */
    File getIndexPath();
    
    /**
     * Gets the directory at which the cloned repository is stored.
     * 
     * @param id the identifier of the git repository. Must not be {@code null}
     * @return the repository path. Never {@code null}
     */
    File getRepositoryPath(final Object id);

    boolean isIndexingRevisions();

    CommitIndexer getRevisionIndexer();

    /**
     * Returns a Collection of GitManager instances, one for each repository.
     *
     * @return the managers.
     */
    Collection<GitManager> getRepositoryList();

    /**
     * Gets the repository with the specified {@code id}.
     * 
     * @param id the identifier of the repository to search for. Must not be {@code null}
     * @return the found repository or {@code null} if it does not exist
     */
    GitManager getRepository(Object id);

    /**
     * Creates a new repository.
     * 
     * @param id the identifier for the new repository
     * @return the new repository
     */
    GitManager createRepository(final String id);

    /**
     * Removes the repository specified by {@code id} from settings.
     * 
     * @param id the identifier for this repository. Must not be {@code null}
     */
    void removeRepository(Object id);
}
