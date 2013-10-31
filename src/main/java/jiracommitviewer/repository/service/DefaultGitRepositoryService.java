package jiracommitviewer.repository.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import jiracommitviewer.domain.AbstractPathCommitFile;
import jiracommitviewer.domain.AddedCommitFile;
import jiracommitviewer.domain.Commit;
import jiracommitviewer.domain.CommitFile;
import jiracommitviewer.domain.CopiedCommitFile;
import jiracommitviewer.domain.DeletedCommitFile;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.domain.ModifiedCommitFile;
import jiracommitviewer.domain.RenamedCommitFile;
import jiracommitviewer.repository.exception.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.Connection;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Repository service for working with Git repositories.
 * <p>
 * Thread safe.
 * 
 * @author mark
 */
public class DefaultGitRepositoryService extends AbstractRepositoryService<GitRepository, GitCommitKey> implements GitRepositoryService {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultGitRepositoryService.class);
	
	/** Branch tracking structure for commit walking. We can use/update this so that we know which branch a particular
	 * commit is on while walking. */
	private Map<ObjectId, String> branchTracker;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void activate(final GitRepository repository) throws RepositoryException {
		Validate.notNull(repository, "repository must not be null");
		
		logger.debug("Activating repository: " + repository.getId());
		
		try {
			// Initialise a new repository at the set location. If it already exists, then this does
			// nothing.
			final Git git = Git.init()
				.setBare(true)
				.setDirectory(getRepositoryPath(repository.getId()))
				.call();
			
			// Check the source repository
			final URIish sourceUri = new URIish(repository.getUri());
			// See if we can open a transport to it
			if (sourceUri.isRemote()) {
				final Transport transport = Transport.open(git.getRepository(), sourceUri);
				configureTransport(transport, repository);
				final Connection conn = transport.openFetch();
				conn.close();
			// Must be a file repository
			} else {
				FileRepository fileRepository = new FileRepository(new File(sourceUri.getRawPath()));
				if (!fileRepository.getConfig().getFile().exists()) {
					repository.setActive(false);
					throw new RepositoryException("Git configuration file does not exist: " + fileRepository.getConfig().getFile());
				}
			}
			repository.setActive(true);
		} catch (final IOException ioe) {
			repository.setActive(false);
			throw new RepositoryException("Failed to activate repository: " + repository.getDisplayName() + ": " + ioe.getMessage(), ioe);
		} catch (final GitAPIException e) {
			repository.setActive(false);
			throw new RepositoryException("Failed to activate repository: " + repository.getDisplayName() + ": " + e.getMessage(), e);
		} catch (final URISyntaxException e) {
			repository.setActive(false);
			throw new RepositoryException("Failed to activate repository: " + repository.getDisplayName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(final GitRepository repository) throws RepositoryException {
		Validate.notNull(repository, "repository must not be null");
		
		try {
			FileUtils.deleteDirectory(getRepositoryPath(repository.getId()));
		} catch (final IOException ioe) {
			throw new RepositoryException("Cannot delete repository clone: " + ioe.getMessage(), ioe);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void cloneRepository(final GitRepository repository) throws RepositoryException {
		Validate.notNull(repository, "repository must not be null");
		
		if (!isCloned(repository)) {
			try {
				FileUtils.deleteDirectory(getRepositoryPath(repository.getId()));
			} catch (final IOException ioe) {
				throw new RepositoryException("Cannot delete repository while preparing for new clone", ioe);
			}

			try {
				Git.cloneRepository()
				.setBare(true)
				.setCloneAllBranches(true)
				.setURI(repository.getUri())
				.setDirectory(getRepositoryPath(repository.getId()))
				.setTransportConfigCallback(new TransportConfigCallback() {
					@Override
					public void configure(final Transport transport) {
						configureTransport(transport, repository);
					}
				})
				.call();
			} catch (final InvalidRemoteException e) {
				throw new RepositoryException("Invalid remote", e);
			} catch (final TransportException e) {
				throw new RepositoryException("Transport error", e);
			} catch (final GitAPIException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean isCloned(final GitRepository repository) throws RepositoryException {
		try {
			final FileRepository fileRepository = getFileRepository(repository);
			for (final RemoteConfig remoteConfig : RemoteConfig.getAllRemoteConfigs(fileRepository.getConfig())) {
				for (final URIish uri : remoteConfig.getURIs()) {
					if (uri.equals(new URIish(repository.getUri()))) {
						return true;
					}
				}
			}
		} catch (final URISyntaxException urise) {
			throw new RepositoryException("URI syntax exception, please check that the syntax is correct", urise);
		}
		logger.debug("Remote repository is not yet cloned: " + repository.getUri());
		return false;
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void fetch(final GitRepository repository) throws RepositoryException {
		Validate.notNull(repository, "repository must not be null");
		
		logger.debug("Fetching from remote repository: " + repository.getUri());
		
		final FileRepository fileRepository = getFileRepository(repository);
		try {
			final FetchResult result = Git.wrap(fileRepository)
				.fetch()
				.setTransportConfigCallback(new TransportConfigCallback() {
					@Override
					public void configure(final Transport transport) {
						configureTransport(transport, repository);
					}
				})
				.call();
			logger.debug(result.getMessages());
		} catch (final InvalidRemoteException e) {
			throw new RuntimeException(e);
		} catch (final TransportException e) {
			throw new RepositoryException("Transport error whilst fetching for repository: " + repository.getId(), e);
		} catch (final GitAPIException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized LogEntryEnumerator<GitRepository, GitCommitKey> getLogEntries(final GitRepository repository, 
			final Set<GitCommitKey> commitKeys) throws RepositoryException {
		Validate.notNull(repository, "repository must not be null");
		
		final FileRepository fileRepository = getFileRepository(repository);
		
		return new LogEntryEnumerator<GitRepository, GitCommitKey>() {
			private static final int WALK_REFRESH_INTERVAL = 10000;
			
			private RevWalk walk;
			private RevCommit lastCommit;
			private RevCommit lastLastCommit;
			/** Will be false if we've already read the next entry and awaiting next() to be called to consume it. */
			private boolean isNextConsumed = true;
			private String currentBranchName;
			
			// Represents the number of next() calls until we renew the walk. We do this to conserve memory
			// following the advice  of the JGit documentation.
			private int walkRefreshInterval = WALK_REFRESH_INTERVAL;
			
			{
				if (commitKeys != null) {
					final Set<ObjectId> objectIds = new HashSet<ObjectId>();
					for (final GitCommitKey commitKey : commitKeys) {
						objectIds.add(ObjectId.fromString(commitKey.getCommitHash()));
					}
					walk = createRevWalker(fileRepository, objectIds);
				} else {
					walk = createRevWalker(fileRepository, null);
				}
			}
			
			@Override
			public LogEntry<GitRepository, GitCommitKey> next() throws RepositoryException {
				if (isNextConsumed) {
					readNext();
				}
				// Mark next entry as consumed
				isNextConsumed = true;
				if (lastCommit == null) {
					throw new NoSuchElementException();
				}
				
				try {
					return new LogEntry<GitRepository, GitCommitKey>(
							repository,
							currentBranchName.substring("refs/heads/".length()),
							new GitCommitKey(lastCommit.getId().getName(), lastCommit.getCommitTime()),
							lastLastCommit != null ? new GitCommitKey(lastLastCommit.getId().getName(), lastLastCommit.getCommitTime()) 
												   : null,
							lastCommit.getAuthorIdent().getName(),
							new Date((long)lastCommit.getCommitTime() * 1000),
							lastCommit.getFullMessage(),
							getAllFilesFromCommit(fileRepository, lastCommit, walk)
					);
				} catch (final MissingObjectException e) {
					throw new RepositoryException("An expected object is missing", e);
				} catch (final IncorrectObjectTypeException e) {
					throw new RuntimeException(e);
				} catch (IOException ioe) {
					throw new RepositoryException("IO error while reading repository: " + repository.getId(), ioe);
				}
			}
			
			@Override
			public boolean hasNext() throws RepositoryException {
				if (isNextConsumed) {
					readNext();
				}
				return lastCommit != null;
			}
			
			/**
			 * Reads the next commit from the {@code walk} and updates the internal state.
			 * 
			 * @throws RepositoryException
			 */
			private void readNext() throws RepositoryException {
				try {
					lastLastCommit = lastCommit;
					lastCommit = walk.next();
					if (lastCommit != null) {
						currentBranchName = updateBranchTracker(lastCommit);
					}
					
					// Renew the walk after WALK_REFRESH_INTERVAL
					if (--walkRefreshInterval == 0) {
						walkRefreshInterval = WALK_REFRESH_INTERVAL;
						walk.dispose();
						walk = createRevWalker(fileRepository, branchTracker.keySet());
					}
					isNextConsumed = false;
				} catch (final MissingObjectException e) {
					throw new RepositoryException("An expected object is missing", e);
				} catch (final IncorrectObjectTypeException e) {
					throw new RuntimeException(e);
				} catch (final IOException e) {
					throw new RepositoryException("Repository access IO error for repository: " + repository.getId(), e);
				}
			}
		};
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public LogEntry<GitRepository, GitCommitKey> getLogEntry(final GitRepository repository, final GitCommitKey commitKey) 
			throws RepositoryException {
		Validate.notNull(repository, "repository must not be null");
		Validate.notNull(commitKey, "commitKey must not be null");
		
		final FileRepository fileRepository = getFileRepository(repository);
		final RevWalk walk = new RevWalk(fileRepository);
		
		final MutableObjectId objectId = new MutableObjectId();
		objectId.fromString(commitKey.getCommitHash());
		try {
			walk.markStart(walk.parseCommit(objectId));
			final RevCommit commit = walk.next();
			final RevCommit parentCommit = walk.next();
			
			return new LogEntry<GitRepository, GitCommitKey>(
					repository,
					null,
					new GitCommitKey(commit.getId().getName(), commit.getCommitTime()),
					parentCommit != null ? new GitCommitKey(parentCommit.getId().getName(), parentCommit.getCommitTime()) : null,
					commit.getAuthorIdent().getName(),
					new Date((long)commit.getCommitTime() * 1000),
					commit.getFullMessage(),
					getAllFilesFromCommit(fileRepository, commit, walk)
			);
		} catch (final MissingObjectException e1) {
			throw new RepositoryException("Could not locate a commit for repository: " + repository.getId() + 
					" and commit key: " + commitKey.marshal() + ". Does the commit for commitKey exist?", e1);
		} catch (final IncorrectObjectTypeException e1) {
			throw new RepositoryException("Could not locate a commit for repository: " + repository.getId() + 
					" and commit key: " + commitKey.marshal() + ". The object identified by commitKey doesn't appear to be a commit", e1);
		} catch (final IOException e1) {
			throw new RepositoryException("Repository access IO error for repository: " + repository.getId(), e1);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void create(final GitRepository repository) {
		Validate.notNull(repository, "repository must not be null");
		
		URIish uri;
		try {
			uri = new URIish(repository.getUri());
			if (!"file".equals(uri.getScheme())) {
				throw new IllegalArgumentException("Only file URI scheme is supported for creating new repositories");
			}
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException("URI syntax exception", e);
		}
		
		try {
			Git.init().setBare(false).setDirectory(new File(uri.getPath())).call();
		} catch (final GitAPIException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void commit(final GitRepository repository, final Commit<GitRepository> commit) throws RepositoryException {
		Validate.notNull(repository, "repository must not be null");
		Validate.notNull(commit, "commit must not be null");
		
		FileRepository fileRepository;
		try {
			final URIish uri = new URIish(repository.getUri());
			if (!"file".equals(uri.getScheme())) {
				throw new IllegalArgumentException("Only file:// URI scheme supported");
			}
			fileRepository = new FileRepository(uri.getPath());
			if (!fileRepository.getObjectDatabase().exists()) {
				fileRepository = (FileRepository)Git.init().setBare(false).setDirectory(new File(uri.getPath())).call()
						.getRepository();
			}
		} catch (final IOException ioe) {
			throw new RepositoryException("Cannot access the repository: " + ioe.getMessage(), ioe);
		} catch (final URISyntaxException urise) {
			throw new RepositoryException("Malformed URI: " + urise.getMessage(), urise);
		} catch (final GitAPIException gitapie) {
			throw new RuntimeException(gitapie);
		}
		
		final AddCommand addCommand = Git.wrap(fileRepository).add();
		final RmCommand rmCommand = Git.wrap(fileRepository).rm();
		boolean haveAdditions = false, haveRemovals = false;
		for (final CommitFile commitFile : commit.getCommitFiles()) {
			if (commitFile instanceof AddedCommitFile || commitFile instanceof ModifiedCommitFile) {
				logger.debug("Adding file " + ((AbstractPathCommitFile)commitFile).getPath());
				addCommand.addFilepattern(((AbstractPathCommitFile)commitFile).getPath());
				haveAdditions = true;
			} else if (commitFile instanceof DeletedCommitFile) {
				logger.debug("Removing file " + ((DeletedCommitFile)commitFile).getPath());
				rmCommand.addFilepattern(((DeletedCommitFile)commitFile).getPath());
				haveRemovals = true;
			}
		}
		
		try {
			try {
				if (haveAdditions) {
					addCommand.call();
				}
				if (haveRemovals) {
					rmCommand.call();
				}
			} catch (final NoFilepatternException e) {
				// Don't expect this state to be ever possible
				throw new RuntimeException(e);
			}
			
			try {
				final RevCommit revCommit = Git.wrap(fileRepository)
					.commit()
					.setMessage(commit.getMessage())
					.setAuthor(commit.getAuthorName(), commit.getAuthorName())
					.call();
				logger.debug("All files committed; head is at " + revCommit.getName());
			} catch (final NoHeadException e) {
				// Don't expect this state to be ever possible
				throw new RuntimeException(e);
			} catch (final NoMessageException e) {
				throw new IllegalArgumentException("No message. Check that a commit message has been supplied", e);
			} catch (UnmergedPathsException e) {
				// Don't expect this state to be ever possible
				throw new RuntimeException(e);
			} catch (final ConcurrentRefUpdateException e) {
				// Don't expect this state to be ever possible
				throw new RuntimeException(e);
			} catch (final WrongRepositoryStateException e) {
				// Don't expect this state to be ever possible
				throw new RuntimeException(e);
			}
		} catch (final GitAPIException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Gets a list of files committed against the specified {@code commit}.
	 * 
	 * @param fileRepository the repository to examine. Must not be {@code null}
	 * @param commit the commit to get files for. Must not be {@code null}
	 * @param walk the walker containing the {@code commit}. Must not be {@code null}
	 * @return the list of files. Never {@code null}
	 * @throws IOException 
	 * @throws IncorrectObjectTypeException 
	 * @throws MissingObjectException 
	 */
	private List<CommitFile> getAllFilesFromCommit(final FileRepository fileRepository, final RevCommit commit, final RevWalk walk) 
			throws MissingObjectException, IncorrectObjectTypeException, IOException {
		assert fileRepository != null : "fileRepository must not be null";
		assert commit != null : "commit must not be null";
		assert walk != null : "walk must not be null";
		
		final List<CommitFile> commitFiles = new ArrayList<CommitFile>();
		
		// Initial commit. Any files on the commit's tree must be new
		if (commit.getParentCount() == 0) {
			TreeWalk tw = new TreeWalk(fileRepository);
			tw.reset();
			tw.setRecursive(true);
			tw.addTree(commit.getTree());
			while (tw.next()) {
				commitFiles.add(new AddedCommitFile(tw.getPathString()));
			}
			tw.release();
			return commitFiles;
		}
		
		// Create a diff between this commit and the parent's and examine all the diff entries.
		for (final RevCommit parent : commit.getParents()) {
			walk.parseCommit(parent.getId());
			final DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
			df.setRepository(fileRepository);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);
			final List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
			for (final DiffEntry diff : diffs) {
				switch (diff.getChangeType()) {
				case ADD:
					commitFiles.add(new AddedCommitFile(diff.getNewPath()));
					break;
				case MODIFY:
					commitFiles.add(new ModifiedCommitFile(diff.getOldPath()));
					break;
				case RENAME:
					commitFiles.add(new RenamedCommitFile(diff.getOldPath(), diff.getNewPath()));
					break;
				case DELETE:
					commitFiles.add(new DeletedCommitFile(diff.getOldPath()));
					break;
				case COPY:
					commitFiles.add(new CopiedCommitFile(diff.getOldPath(), diff.getNewPath()));
					break;
				}
			}
		}
		return commitFiles;
	}
	
	/**
	 * Creates a new repository walker that walks all branches on the supplied {@code fileRepository}.
	 * <p>
	 * Commits are walked backwards in time starting from the leaves (the most recent commits). By providing a {@code commitKeyMap}
	 * the later commits can be skipped by starting the search at the {@code commitKey}'s time and retrieving all commits
	 * before it. The first commit walked by the returned walker will be directly after the {@code commitKey}'s.
	 * <p>
	 * Should history have changed in the repository that renders the {@code commitKey} missing, a {@code RepositoryException}
	 * will be thrown.
	 * <p>
	 * For large repositories, it is advised that the revision walker is discarded at frequent intervals to prevent memory from
	 * filling up. This is done by calling {@link RevWalk#dispose()}.
	 * 
	 * @param fileRepository the git repository to walk. Must not be {@code nul}
	 * @param commitKeys the keys whose identity will be used to exclude these commmits and their parents. If {@code null} then all 
	 * commits will be walked from all branch leafs
	 * @return the revision walker instance. Never {@code null}
	 * @throws RepositoryException if an error occurs while reading the repository
	 */
	private RevWalk createRevWalker(final FileRepository fileRepository, final Set<ObjectId> commitKeys) 
			throws RepositoryException {
		assert fileRepository != null : "fileRepository must not be null";
		
		try {
			final RevWalk walk = new RevWalk(fileRepository);
			branchTracker = new HashMap<ObjectId, String>();
			
			for (final Ref branch : Git.wrap(fileRepository).branchList().call()) {
				branchTracker.put(branch.getObjectId(), branch.getName());
				walk.markStart(walk.parseCommit(branch.getObjectId()));
			}
			
			// Create a revision filter to filter out all commits equal or being a parent of any of the commitKeys specified.
			// We must also update the branchTracker as we progress.
			if (commitKeys != null) {
				walk.setRevFilter(new NewCommitsRevisionFilter(commitKeys) {
					@Override
					public boolean include(final RevWalk walker, final RevCommit cmit) throws StopWalkException, MissingObjectException,
							IncorrectObjectTypeException, IOException {
						final boolean include = super.include(walker, cmit);
						if (!include) {
							updateBranchTracker(cmit);
						}
						return include;
					}
					@Override
					public RevFilter clone() {
						return super.clone();
					}
				});
			}
			
			return walk;
		} catch (final IOException ioe) {
			throw new RepositoryException("IOError while attempting to read repository logs", ioe);
		} catch (final GitAPIException gitapie) {
			throw new RuntimeException(gitapie);
		}
	}
	
	/**
	 * Updates the branch tracker with the {@code commit} specified. This must be called with the next
	 * {@code commit} in sequence while walking.
	 * 
	 * @param commit the commit. Must not be {@code null}
	 * @return the branch that the specified {@code commit} belongs to. Never {@code null}
	 * @throws IllegalStateException if the commit's branch isn't known (internal error)
	 */
	private String updateBranchTracker(final RevCommit commit) {
		assert commit != null : "commit must not be null";
		
		final String branchName = branchTracker.get(commit);
		if (branchName == null) {
			throw new IllegalStateException("No branch name found for commit: " + commit.getName());
		}
		branchTracker.remove(commit);
		
		final RevCommit[] parents = commit.getParents();
		for (int i = 0; i < parents.length; i++) {
			if (i == 0 || !branchTracker.containsKey(parents[i])) {
				branchTracker.put(parents[i], branchName);
			}
		}
		return branchName;
	}
	
	/**
	 * Gets the underlying native Git repository object from domain object.
	 *  
	 * @param repository the repository to get for. Must not be {@code null}
	 * @return the native Git repository. Never {@code null}
	 * @throws RepositoryException if a problem occurs while trying to open it
	 */
	private FileRepository getFileRepository(final GitRepository repository) throws RepositoryException {
		assert repository != null : "repository must not be null";
		
		try {
			return new FileRepository(getRepositoryPath(repository.getId()));
		} catch (final IOException ioe) {
			throw new RepositoryException("IOError while attempting to open repository", ioe);
		}
	}
    
    /**
     * Gets the directory at which the cloned repository is stored.
     * 
     * @param id the identifier of the git repository. Must not be {@code null}
     * @return the repository path. Never {@code null}
     */
    private File getRepositoryPath(final Object id) {
    	assert id != null : "id must not be null";
    	
    	return new File(getIndexPath(), id + File.separator + "repo");
    }
    
    /**
     * Gets the an SshSessionFactory that is customised to allow for authentication for the specified
     * {@code repository}. This is used when connecting to a Git repository over an SSH tunnel.
     * 
     * @param repository the repository to get the session factory for. Must not be {@code null}
     * @return the session factory. Never {@code null}
     */
    private SshSessionFactory getSshSessionFactory(final GitRepository repository) {
    	assert repository != null : "repository must not be null";
    	
    	final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
    		@Override
    		protected void configure(final Host hc, final Session session) {
    		}
			@Override
			protected JSch createDefaultJSch(final FS fs) throws JSchException {
				JSch.setConfig("StrictHostKeyChecking", "no");
				
				final JSch jsch = new JSch();
				if (repository.getPrivateKeyPath() != null) {
					jsch.addIdentity(repository.getPrivateKeyPath().getPath());
				}
		    	return jsch;
			}
		};
		return sshSessionFactory;
    }
    
    /**
     * Configures the Git transport before using it.
     * 
     * @param transport the transport to configure. Must not be {@code null}
     * @param repository the repository configuration being used. Must not be {@code null}
     */
    private void configureTransport(final Transport transport, final GitRepository repository) {
    	assert transport != null : "transport must not be null";
    	assert repository != null : "repository must not be null";
    	
    	// Configure private key for SSH
    	if (transport instanceof SshTransport) {
			((SshTransport)transport).setSshSessionFactory(getSshSessionFactory(repository));
		}
    }
    
    /**
     * A revision filter that maintains a stateful collection of SHA-1 'stops'. Whenever a commit is reached with a SHA-1 sum
     * equal to any stop, it and all its parents will never be included. This essentially forces only 'new' commits to ever be
     * included.
     * <p>
     * Because of its statefulness, instances cannot be used between multiple instances of {@link RevWalk}s.
     * 
     * @author mark
     */
    private class NewCommitsRevisionFilter extends RevFilter {
    	
		private final Set<ObjectId> sha1Stops;
		
		/**
		 * Initialises a new revision filter with the specified stops.
		 * 
		 * @param sha1Stops the stops to initialise with. Must not be {@code null}
		 */
		public NewCommitsRevisionFilter(final Set<ObjectId> sha1Stops) {
			Validate.notNull(sha1Stops, "sha1Stops must not be null");
			
			this.sha1Stops = sha1Stops;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean include(final RevWalk walker, final RevCommit cmit) throws StopWalkException, MissingObjectException,
				IncorrectObjectTypeException, IOException {
			boolean include = true;
			
			if (sha1Stops.contains(cmit.getId().getName())) {
				sha1Stops.remove(cmit.getId().getName());
				for (final RevCommit parent : cmit.getParents()) {
					sha1Stops.add(parent.getId());
				}
				include = false;
			}
			if (!include) {
				updateBranchTracker(cmit);
			}
			return include;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public RevFilter clone() {
			return new NewCommitsRevisionFilter(new HashSet<ObjectId>(sha1Stops));
		}
    }
}
