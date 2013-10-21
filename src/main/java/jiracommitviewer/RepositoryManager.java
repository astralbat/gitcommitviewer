package jiracommitviewer;

import java.util.Collection;

import jiracommitviewer.domain.AbstractRepository;

/**
 * Main component of the Git plugin.
 */
public interface RepositoryManager {
	
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
     * Gets a collection of all repositories managd.
     *
     * @return the repositories. Never {@code null}
     */
    Collection<AbstractRepository> getRepositoryList();
    
    /**
     * Gets a collection of all repositories of the specified type.
     * 
     * @param clazz the type of repository to filter. Must not be {@code null}
     * @return the matching repositories. Never {@code null}
     */
    <T extends AbstractRepository> Collection<T> getRepositoryList(Class<T> clazz);

    /**
     * Gets the repository with the specified {@code id}.
     * 
     * @param id the identifier of the repository to search for. Must not be {@code null}
     * @return the found repository or {@code null} if it does not exist
     */
    AbstractRepository getRepository(Object id);
    
    /**
     * Gets the repository with the specified {@code id} and of the specified type.
     * 
     * @param id the identifier of the repository to search for. Must not be {@code null}
     * @param clazz the type of repository to retrieve. Must not be {@code null}
     * @return the found repository or {@code null} if it does not exist
     */
    <T extends AbstractRepository> T getRepository(Object id, Class<T> clazz);

    /**
     * Creates a new repository and registers it internally.
     * 
     * @param type the type of repository to create. Must not be {@code null}
     * @return the new repository. Never {@code null}
     */
    AbstractRepository createRepository(final RepositoryType type);
    
    /**
     * Saves all settings for the specified {@code repository}, replacing any existing settings
     * that exist for it.
     * 
     * @param repository the repository to save. Must not be {@code null}
     */
    void saveRepository(final AbstractRepository repository);

    /**
     * Removes the repository specified by {@code id} from settings.
     * 
     * @param id the identifier for this repository. Must not be {@code null}
     */
    void removeRepository(Object id);
    
    /**
     * Parses a repository identifier from a String in to the appropriate type.
     * 
     * @param id the repository identifier as a String. Must not be {@code null}
     * @return the native repository identifier. Never {@code null}
     */
    Object parseRepositoryId(final String id);
}
