/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 8:13:56 AM
 */
package jiracommitviewer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import jiracommitviewer.linkrenderer.GitLinkRenderer;

import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GitManagerImpl implements GitManager {
	
	private final static Logger logger = LoggerFactory.getLogger(GitManagerImpl.class);

	private final MultipleGitRepositoryManager repositoryManager;
	private final Properties properties;
    private GitLinkRenderer linkRenderer;
    private Map logEntryCache;
    private boolean active;
    private String inactiveMessage;
	
	/**
	 * Creates a new manager with the specified id
	 * 
	 * @param repositoryManager the repository manager. Must not be {@code null}
	 * @param id the repository to create. Must not be {@code null}
	 */
	public GitManagerImpl(final MultipleGitRepositoryManager repositoryManager, final String id) {
		Validate.notNull(repositoryManager, "repositoryManager must not be null");
		Validate.notNull(id, "id must not be null");
		Validate.isTrue(repositoryManager.getRepository(id) == null, "a repository with this id already exists: " + id);
		
		this.repositoryManager = repositoryManager;
		properties = new Properties();
		properties.setProperty(GitManager.GIT_REPOSITORY_NAME, id);
    }
	
	/**
	 * Recreates a manager from a saved list of properties.
	 * 
	 * @param repositoryManager the repository manager. Must not be {@code null}
	 * @param properties the list of properties. Must not be {@code null} and must contain at least an id
	 */
	public GitManagerImpl(final MultipleGitRepositoryManager repositoryManager, final Properties properties) {
		Validate.notNull(repositoryManager, "repositoryManager must not be null");
		Validate.notNull(properties, "properties must not be null");
		Validate.notNull(properties.getProperty(GitManager.GIT_REPOSITORY_NAME), "repository id must not be null");
		
		this.repositoryManager = repositoryManager;
        this.properties = properties;
        setup();
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return getDisplayName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDisplayName() {
	    return properties.getProperty(GitManager.GIT_REPOSITORY_NAME);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUri() {
		return properties.getProperty(GitManager.GIT_URI);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCommitLinkFormat() {
		return properties.getProperty(GitManager.GIT_LINKFORMAT_COMMIT);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getModifiedFileLinkFormat() {
		return properties.getProperty(GitManager.GIT_LINKFORMAT_FILE_MODIFIED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void activate() {
		logger.debug("Activating repository: " + getId());
		
		try {
			// Initialise a new repository at the set location. If it already exists, then this does
			// nothing.
			Git.init()
				.setDirectory(repositoryManager.getRepositoryPath(getId()))
				.call();
			
			// Check the source repository
			URIish sourceUri = new URIish(getUri());
			// See if we can open a transport to it
			if (sourceUri.isRemote()) {
				Transport.open(sourceUri);
			// Must be a file repository
			} else {
				FileRepository fileRepository = new FileRepository(new File(sourceUri.getRawPath()));
				if (!fileRepository.getConfig().getFile().exists()) {
					active = false;
					inactiveMessage = "Git configuration files does not exist: " + fileRepository.getConfig().getFile();
					return;
				}
			}
			active = true;
		} catch (final IOException ioe) {
			active = false;
			inactiveMessage = ioe.getClass().getName() + ": " + ioe.getMessage();	
		} catch (final GitAPIException e) {
			active = false;
			inactiveMessage = e.getClass().getName() + ": " + e.getMessage();
		} catch (final URISyntaxException e) {
			active = false;
			inactiveMessage = e.getClass().getName() + ": " + e.getMessage();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isActive() {
		return active;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInactiveMessage() {
		return inactiveMessage;
	}
	
    protected void setup() {
        activate();
    }
    
//
//    public synchronized Collection getLogEntries(long revision) {
//        final Collection logEntries = new ArrayList();
//
//        // if connection isn't up, don't even try
//        if (!isActive()) {
//            return logEntries;
//        }
//
//        long latestRevision;
//        try {
//            latestRevision = repository.getLatestRevision();
//        }
//        catch (SVNException e) {
//            // connection was active, but apparently now it's not
//            log.error("Error getting the latest revision from the repository.", e);
//            deactivate(e.getMessage());
//            return logEntries;
//        }
//
//        if (log.isDebugEnabled()) {
//            log.debug("Latest revision in repository=" + getRoot() + "  is : " + latestRevision);
//        }
//
//        if (latestRevision > 0 && latestRevision <= revision) {
//            if (log.isDebugEnabled()) {
//                log.debug("Have all the commits for repository=" + getRoot() + " - doing nothing.");
//            }
//            return logEntries;
//        }
//
//        long retrieveStart = revision + 1;
//        if (retrieveStart < 0) {
//            retrieveStart = 0;
//        }
//
//        if (log.isDebugEnabled()) {
//            log.debug("Retrieving revisions to index (between " + retrieveStart + " and " + latestRevision + ") for repository=" + getRoot());
//        }
//
//        try {
//            repository.log(new String[]{""}, retrieveStart, latestRevision, true, true, new ISVNLogEntryHandler() {
//                public void handleLogEntry(SVNLogEntry logEntry) {
//                    if (log.isDebugEnabled()) {
//                        log.debug("Retrieved #" + logEntry.getRevision() + " : " + logEntry.getMessage());
//                    }
//
//                    if (TextUtils.stringSet(logEntry.getMessage()) && JiraKeyUtils.isKeyInString(StringUtils.upperCase(logEntry.getMessage()))) {
//                        logEntries.add(logEntry);
//                    }
//                }
//            });
//        }
//        catch (SVNException e) {
//            log.error("Error retrieving changes from the repository.", e);
//            deactivate(e.getMessage());
//        }
//        if (log.isDebugEnabled()) {
//            log.debug("Retrieved " + logEntries.size() + " relevant revisions to index (between " + retrieveStart + " and " + latestRevision + ") from repository=" + getRoot());
//        }
//
//        // temp log comment
//        if (log.isDebugEnabled()) {
//            log.debug("log entries size = " + logEntries.size() + " for " + getRoot());
//        }
//        return logEntries;
//    }
//
//    public synchronized SVNLogEntry getLogEntry(long revision) {
//        if (!isActive()) {
//            throw new IllegalStateException("The connection to the repository is not active");
//        }
//        final SVNLogEntry[] logEntry = new SVNLogEntry[]{(SVNLogEntry) logEntryCache.get(new Long(revision))};
//
//        if (logEntry[0] == null) {
//            try {
//                if (log.isDebugEnabled()) {
//                    log.debug("No cache - retrieving log message for revision: " + revision);
//                }
//
//                repository.log(new String[]{""}, revision, revision, true, true, new ISVNLogEntryHandler() {
//                    public void handleLogEntry(SVNLogEntry entry) {
//                        logEntry[0] = entry;
//                        ensureCached(entry);
//                    }
//                });
//            }
//            catch (SVNException e) {
//                log.error("Error retrieving logs: " + e, e);
//                deactivate(e.getMessage());
//                throw new InfrastructureException(e);
//            }
//        } else if (log.isDebugEnabled()) {
//            log.debug("Found cached log message for revision: " + revision);
//        }
//        return logEntry[0];
//    }
//
//
//    public long getId() {
//        return id;
//    }
//
//    /**
//     * Make sure a single log message is cached.
//     */
//    private void ensureCached(SVNLogEntry logEntry) {
//        synchronized (logEntryCache) {
//            logEntryCache.put(new Long(logEntry.getRevision()), logEntry);
//        }
//    }
//
//    public PropertySet getProperties() {
//        return properties;
//    }
//
//    public String getDisplayName() {
//        return !properties.exists(MultipleSubversionRepositoryManager.SVN_REPOSITORY_NAME) ? getRoot() : properties.getString(MultipleSubversionRepositoryManager.SVN_REPOSITORY_NAME);
//    }
//
//    public String getRoot() {
//        return properties.getString(MultipleSubversionRepositoryManager.SVN_ROOT_KEY);
//    }
//
//    public String getUsername() {
//        return properties.getString(MultipleSubversionRepositoryManager.SVN_USERNAME_KEY);
//    }
//
//    public String getPassword() {
//        try {
//            return decryptPassword(properties.getString(MultipleSubversionRepositoryManager.SVN_PASSWORD_KEY));
//        } catch(IOException e) {
//            log.error("Couldn't decrypt the password. Reseting it to null.", e);
//            return null;
//        }
//    }
//
//    public int getRevisioningCacheSize() {
//        return properties.getInt(MultipleSubversionRepositoryManager.SVN_REVISION_CACHE_SIZE_KEY);
//    }
//
//    public String getPrivateKeyFile() {
//        return properties.getString(MultipleSubversionRepositoryManager.SVN_PRIVATE_KEY_FILE);
//    }
//
//    public boolean isActive() {
//        return active;
//    }
//
//    public String getInactiveMessage() {
//        return inactiveMessage;
//    }
//
//    public void activate() {
//        try {
//            final SVNURL url = parseSvnUrl();
//            repository = createRepository(url);
//            final ISVNAuthenticationManager authManager;
//            if (null != getPrivateKeyFile()) {
//                authManager = new BasicAuthenticationManager(getUsername(), new File(getPrivateKeyFile()), getPassword(), 22);
//            } else {
//                authManager = SVNWCUtil.createDefaultAuthenticationManager(getUsername(), getPassword());
//            }
//            repository.setAuthenticationManager(new ISVNAuthenticationManagerDelegator(authManager));
//
//            repository.testConnection();
//            active = true;
//        }
//        catch (SVNException e) {
//            log.error("Connection to Subversion repository " + getRoot() + " failed: " + e, e);
//            // We don't want to throw an exception here because then the system won't start if the repo is down
//            // or there is something wrong with the configuration.  We also still want this repository to show up
//            // in our configuration so the user has a chance to fix the problem.
//            active = false;
//            inactiveMessage = e.getMessage();
//        }
//    }
//
//    SVNURL parseSvnUrl()
//            throws SVNException
//    {
//        return SVNURL.parseURIEncoded(getRoot());
//    }
//
//    SVNRepository createRepository(SVNURL url)
//            throws SVNException
//    {
//        return SVNRepositoryFactory.create(url);
//    }
//
//    private void deactivate(String message) {
//        if (repository != null) {
////			try {
//                repository.closeSession();
////			}
////			catch (SVNException e) {
//                // ignore, we're throwing the repository away anyways
////			}
//            repository = null;
//        }
//        active = false;
//        inactiveMessage = message;
//    }
//
//    public ViewLinkFormat getViewLinkFormat() {
//        if (!isViewLinkSet) {
//            final String type = properties.getString(MultipleSubversionRepositoryManager.SVN_LINKFORMAT_TYPE);
//            final String linkPathFormat = properties.getString(MultipleSubversionRepositoryManager.SVN_LINKFORMAT_PATH_KEY);
//            final String changesetFormat = properties.getString(MultipleSubversionRepositoryManager.SVN_LINKFORMAT_CHANGESET);
//            final String fileAddedFormat = properties.getString(MultipleSubversionRepositoryManager.SVN_LINKFORMAT_FILE_ADDED);
//            final String fileModifiedFormat = properties.getString(MultipleSubversionRepositoryManager.SVN_LINKFORMAT_FILE_MODIFIED);
//            final String fileReplacedFormat = properties.getString(MultipleSubversionRepositoryManager.SVN_LINKFORMAT_FILE_REPLACED);
//            final String fileDeletedFormat = properties.getString(MultipleSubversionRepositoryManager.SVN_LINKFORMAT_FILE_DELETED);
//
//            if (linkPathFormat != null || changesetFormat != null || fileAddedFormat != null || fileModifiedFormat != null || fileReplacedFormat != null || fileDeletedFormat != null)
//                viewLinkFormat = new ViewLinkFormat(type, changesetFormat, fileAddedFormat, fileModifiedFormat, fileReplacedFormat, fileDeletedFormat, linkPathFormat);
//            else
//                viewLinkFormat = null; /* [SVN-190] This could happen if the user clears all the fields in the Subversion repository web link configuration */
//            isViewLinkSet = true;
//        }
//
//        return viewLinkFormat;
//    }
//
//    public SubversionLinkRenderer getLinkRenderer() {
//        return linkRenderer;
//    }
//
//
//    protected static String decryptPassword(String encrypted) throws IOException {
//        if (encrypted == null)
//            return null;
//
//        byte[] result = Base64.decodeBase64(encrypted);
//
//        return new String(result, 0, result.length);
//    }
//
//    protected static String encryptPassword(String password) {
//        if (password == null)
//            return null;
//
//        return Base64.encodeBase64String(password.getBytes());
//    }

}
