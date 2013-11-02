package jiracommitviewer.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.index.exception.IndexException;
import jiracommitviewer.repository.exception.RepositoryException;
import jiracommitviewer.repository.service.GitRepositoryService;
import jiracommitviewer.repository.service.LogEntryEnumerator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.JiraKeyUtils;
import com.opensymphony.util.TextUtils;

/**
 * Indexer for Git repositories.
 * 
 * @author mark
 */
public class GitCommitIndexer implements CommitIndexer<GitRepository, GitCommitKey> {

	private final static Logger logger = LoggerFactory.getLogger(GitCommitIndexer.class);
	
    @Autowired
    private RepositoryManager repositoryManager;
    @Autowired
    private GitRepositoryService gitRepositoryService;
    @Autowired
    private VersionManager versionManager;
    @Autowired
    private IssueManager issueManager;
    @Autowired
    private PermissionManager permissionManager;
    @Autowired
    private ChangeHistoryManager changeHistoryManager;
    @Autowired
    private IndexPathManager indexPathManager;
    
    private final Hashtable<Object, Map<List<String>, GitCommitKey>> branchIndexedCommitTbl = new Hashtable<Object, Map<List<String>, GitCommitKey>>();
    private final Hashtable<Object, Set<GitCommitKey>> frequentIndexedCommitTbl = new Hashtable<Object, Set<GitCommitKey>>();
    private int frequentIndexedCount = 0;
    private LuceneIndexAccessor indexAccessor;

    public GitCommitIndexer() {
    	indexAccessor = new DefaultLuceneIndexAccessor();
    }

    public GitCommitIndexer(final LuceneIndexAccessor accessor) {
        this.indexAccessor = accessor;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void index(final GitRepository repository) throws IndexException, RepositoryException {
    	Validate.notNull(repository, "repository must not be null");
    	
    	gitRepositoryService.cloneRepository(repository);
    	gitRepositoryService.fetch(repository);
    	if (createIndexIfNeeded()) {
    		branchIndexedCommitTbl.clear();
        	frequentIndexedCommitTbl.clear();
    	}
    	updateIndex(repository);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public File getIndexPath() {
    	return new File(gitRepositoryService.getIndexPath() + File.separator + "indexes");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LogEntry<GitRepository, GitCommitKey>> getAllLogEntriesByIssue(final Issue issue, final int pageNumber, 
    		final int pageSize) throws IndexException {
    	Validate.notNull(issue, "issue must not be null");
    	Validate.isTrue(pageNumber >= 0, "pageNumber must be >= 0");
    	Validate.isTrue(pageSize >= 0, "pageSize must be >= 0");
    	
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving revisions for: " + issue.getKey());
        }

        // Create indexes if necessary to prevent getting an error
        createIndexIfNeeded();
        
        try {
	        final IndexReader reader = indexAccessor.getIndexReader(getIndexPath().getPath());
	        final IndexSearcher searcher = new IndexSearcher(reader);
	
	        try {
	            final TopDocs hits = searcher.search(createQueryByIssueKey(issue), MAX_COMMITS, 
	            		new Sort(new SortField(FIELD_DATE, SortField.STRING, true)));
	            final List<LogEntry<GitRepository, GitCommitKey>> logEntries = 
	            		new ArrayList<LogEntry<GitRepository, GitCommitKey>>(hits.totalHits);
	
	            for (int i = 0; i < Math.min(hits.totalHits, MAX_COMMITS); i++) {
	            	// Skip results until we arrive at our page
	            	if (i < pageNumber * pageSize) {
	            		continue;
	            	}
	            	// Page now filled so break out
	            	if (i >= pageNumber * pageSize + pageSize) {
	            		break;
	            	}
	            	
	                final Document doc = searcher.doc(hits.scoreDocs[i].doc);
	                final String repositoryId = doc.get(FIELD_REPOSITORY);
	                final AbstractRepository repository = repositoryManager.getRepository(
	                		repositoryManager.parseRepositoryId(repositoryId));
	                
	                // Verify that the repository is of the correct type.
	                if (repository instanceof GitRepository) {
	                	final GitCommitKey commitKey = GitCommitKey.unmarshal(doc.get(FIELD_COMMITKEY));
	                	try {
	                		final LogEntry<GitRepository, GitCommitKey> logEntry = gitRepositoryService
	                				.getLogEntry((GitRepository)repository, commitKey);
	                		if (logEntry.getBranches() == null) {
	                			logEntry.setBranches(Arrays.asList(doc.getValues(FIELD_BRANCH)));
	                		}
	                		logEntries.add(logEntry);
	                	} catch (final RepositoryException re) {
	                		// Assume that this is because the commit could not be found due to a change of history. If that's
	                		// the case then expect this to be cleared up by the main indexing activity.
	                		logger.warn("Could not find log message for commit: " + commitKey.marshal(), re);
	            			continue;
	                	}
	                }
	            }
	            return logEntries;
	        } finally {
	            searcher.close();
	            reader.close();
	        }
        } catch (final IOException ioe) {
        	throw new IndexException("Index IO access error", ioe);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<LogEntry<GitRepository, GitCommitKey>> getAllLogEntriesByProject(final String projectKey, final User user, 
    		final int pageNumber, final int pageSize) throws IndexException {
    	Validate.notNull(projectKey, "projectKey must not be null");
    	Validate.notNull(user, "user must not be null");
    	Validate.isTrue(pageNumber >= 0, "pageNumber must be >= 0");
    	Validate.isTrue(pageSize >= 0, "pageSize must be >= 0");
    	
    	// Create indexes if necessary to prevent getting an error
        createIndexIfNeeded();
        
        // Set up and perform a search for all documents having the supplied projectKey,
        // sorted in descending date order
        final TermQuery query = new TermQuery(new Term(FIELD_PROJECTKEY, projectKey));

        try {
	        final IndexReader reader = indexAccessor.getIndexReader(getIndexPath().getPath());
	        final IndexSearcher searcher = new IndexSearcher(reader);
	
	        try {
	            final TopDocs hits = searcher.search(query, new ProjectRevisionFilter(issueManager, permissionManager, user, projectKey), 
	            		MAX_COMMITS, new Sort(new SortField(FIELD_DATE, SortField.LONG, true)));
	            final List<LogEntry<GitRepository, GitCommitKey>> logEntries = new ArrayList<LogEntry<GitRepository, GitCommitKey>>();
	            
	            for (int i = 0, j = Math.min(hits.totalHits, MAX_COMMITS); i < j; ++i) {
	            	// Skip results until we arrive at our page
	            	if (i < pageNumber * pageSize) {
	            		continue;
	            	}
	            	// Page now filled so break out
	            	if (i >= pageNumber * pageSize + pageSize) {
	            		break;
	            	}
	
	                final Document doc = searcher.doc(hits.scoreDocs[i].doc);
	
	                final String repositoryId = doc.get(FIELD_REPOSITORY);
	                final AbstractRepository repository = repositoryManager.getRepository(
	                		repositoryManager.parseRepositoryId(repositoryId));
	                
	                // Verify that the repository is of the correct type.
	                if (repository instanceof GitRepository) {
	                	final GitCommitKey commitKey = GitCommitKey.unmarshal(doc.get(FIELD_COMMITKEY));
	                	try {
	                		final LogEntry<GitRepository, GitCommitKey> logEntry = gitRepositoryService
	                				.getLogEntry((GitRepository)repository, commitKey);
	                		if (logEntry.getBranches() == null) {
	                			logEntry.setBranches(Arrays.asList(doc.getValues(FIELD_BRANCH)));
	                		}
	                		logEntries.add(logEntry);
	                	} catch (final RepositoryException re) {
	                		// Assume that this is because the commit could not be found due to a change of history. If that's
	                		// the case then expect this to be cleared up by the main indexing activity.
	                		logger.warn("Could not find log message for commit: " + commitKey.marshal(), re);
	            			continue;
	                	}
	                }
	            }
	            return logEntries;
	        } finally {
	            searcher.close();
	            reader.close();
	        }
        } catch (final IOException ioe) {
        	throw new IndexException("Index IO access error", ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
	@Override
    public List<LogEntry<GitRepository, GitCommitKey>> getAllLogEntriesByVersion(final Version version, final User user, 
    		final int pageNumber, final int pageSize) throws IndexException {
    	Validate.notNull(version, "version must not be null");
    	Validate.notNull(user, "user must not be null");
    	Validate.isTrue(pageNumber >= 0, "pageNumber must be >= 0");
    	Validate.isTrue(pageSize >= 0, "pageSize must be >= 0");
    	
    	// Create indexes if necessary to prevent getting an error
        createIndexIfNeeded();

        // Find all isuses affected by and fixed by any of the versions:
        final Collection<Issue> issues = new HashSet<Issue>();

        issues.addAll(versionManager.getIssuesWithFixVersion(version));
        issues.addAll(versionManager.getIssuesWithAffectsVersion(version));

        // Construct a query with all the issue keys. Make sure to increase the maximum number of clauses if needed.
        final int maxClauses = BooleanQuery.getMaxClauseCount();
        if (issues.size() > maxClauses) {
            BooleanQuery.setMaxClauseCount(issues.size());
        }

        final BooleanQuery query = new BooleanQuery();
        final Set<String> permittedIssueKeys = new HashSet<String>();

        for (final Issue issue : issues) {
            final Issue theIssue = issueManager.getIssueObject(issue.getKey());

            if (permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, theIssue, user)) {
                final TermQuery termQuery = new TermQuery(new Term(FIELD_ISSUEKEY, issue.getKey()));
                query.add(termQuery, BooleanClause.Occur.SHOULD);
                permittedIssueKeys.add(issue.getKey());
            }
        }

        try {
	        final IndexReader reader = indexAccessor.getIndexReader(getIndexPath().getPath());
	        final IndexSearcher searcher = new IndexSearcher(reader);
	
	        try {
	            // Run the query and sort by date in descending order
	            final TopDocs hits = searcher.search(query, new PermittedIssuesRevisionFilter(issueManager, permissionManager, user, 
	            		permittedIssueKeys), MAX_COMMITS, new Sort(new SortField(FIELD_DATE, SortField.LONG, true)));
	            final List<LogEntry<GitRepository, GitCommitKey>> logEntries = new ArrayList<LogEntry<GitRepository, GitCommitKey>>();
	
	            for (int i = 0, j = Math.min(hits.totalHits, MAX_COMMITS); i < j; ++i) {
	            	// Skip results until we arrive at our page
	            	if (i < pageNumber * pageSize) {
	            		continue;
	            	}
	            	// Page now filled so break out
	            	if (i >= pageNumber * pageSize + pageSize) {
	            		break;
	            	}
	
	                final Document doc = searcher.doc(hits.scoreDocs[i].doc);
	
	                final String repositoryId = doc.get(FIELD_REPOSITORY);
	                final AbstractRepository repository = repositoryManager.getRepository(
	                		repositoryManager.parseRepositoryId(repositoryId));
	                
	                // Verify that the repository is of the correct type.
	                if (repository instanceof GitRepository) {
	                	final GitCommitKey commitKey = GitCommitKey.unmarshal(doc.get(FIELD_COMMITKEY));
	                	try {
	                		final LogEntry<GitRepository, GitCommitKey> logEntry = gitRepositoryService
	                				.getLogEntry((GitRepository)repository, commitKey);
	                		if (logEntry.getBranches() == null) {
	                			logEntry.setBranches(Arrays.asList(doc.getValues(FIELD_BRANCH)));
	                		}
	                		logEntries.add(logEntry);
	                	} catch (final RepositoryException re) {
	                		// Assume that this is because the commit could not be found due to a change of history. If that's
	                		// the case then expect this to be cleared up by the main indexing activity.
	                		logger.warn("Could not find log message for commit: " + commitKey.marshal(), re);
	            			continue;
	                	}
	                }
	            }
	            return logEntries;
	        } finally {
	            searcher.close();
	            reader.close();
	            BooleanQuery.setMaxClauseCount(maxClauses);
	        }
        } catch (final IOException ioe) {
        	throw new IndexException("Index IO access error", ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeEntries(final GitRepository repository) throws IndexException {
        logger.debug("Deleteing log entries for: " + repository.getId());
        
        // Create indexes if necessary to prevent getting an error
        createIndexIfNeeded();

        IndexWriter writer = null;
        try {
	        try {
	            writer = indexAccessor.getIndexWriter(getIndexPath().getPath(), false, ANALYZER);
	            writer.deleteDocuments(new Term(FIELD_REPOSITORY, String.valueOf(repository.getId())));
	            branchIndexedCommitTbl.remove(repository.getId());
	            frequentIndexedCommitTbl.remove(repository.getId());
	        } finally {
	            if (writer != null) {
	            	writer.close();
	            }
	        }
        } catch (final IOException ioe) {
            throw new IndexException("Index IO access error", ioe);
        }
    }
    
    /**
     * Looks for the revision index directory and creates it if it does not already exists.
     *
     * @return {@code true} if the index directory was created; {@code false} if the index already exists
     * @throws IndexException if the index cannot be created
     */
    private boolean createIndexIfNeeded() throws IndexException {
        boolean indexExists = getIndexPath().exists();
        if (!indexExists) {
            try {
                indexAccessor.getIndexWriter(getIndexPath().getPath(), true, ANALYZER).close();
                return true;
            } catch (final IOException ioe) {
                throw new IndexException("Cannot create the repository index", ioe);
            }
        }
        return false;
    }
    
    /**
     * This method updates the index, assuming it already exists.
     *
     * @param repository the repository to index. It must already be active and ready to index. Must not be {@code null}
     * @throws IndexException if there is some problem in the indexing subsystem meaning indexes cannot be updated
     * @throws RepositoryException 
     */
    private void updateIndex(final GitRepository repository) throws IndexException, RepositoryException {
    	assert repository != null : "repository must not be null";
    	
        logger.debug("Updating commit index for repository: " + repository.getId());
        
        // Operate on clones of the global state. This way we shall 'commit' them to the global state only if we
        // are successful. If we don't do this, failures will cause earlier parts of the graph to remain unindexed.
        @SuppressWarnings("unchecked")
		final Hashtable<Object, Map<List<String>, GitCommitKey>> branchIndexedCommitTbl = 
        		(Hashtable<Object, Map<List<String>, GitCommitKey>>)this.branchIndexedCommitTbl.clone();
        @SuppressWarnings("unchecked")
		final Hashtable<Object, Set<GitCommitKey>> frequentIndexedCommitTbl = 
        		(Hashtable<Object, Set<GitCommitKey>>)this.frequentIndexedCommitTbl.clone();
        int frequentIndexedCount = this.frequentIndexedCount;
    	
        try {
        	// Make sure latest/frequent hashes are initialised
            if (branchIndexedCommitTbl.get(repository.getId()) == null) {
            	branchIndexedCommitTbl.put(repository.getId(), new HashMap<List<String>, GitCommitKey>());
            	frequentIndexedCommitTbl.put(repository.getId(), updateFrequentIndexedCommitTable(repository));
            }

            final Set<GitCommitKey> commitKeys = new HashSet<GitCommitKey>();
            commitKeys.addAll(branchIndexedCommitTbl.get(repository.getId()).values());
            commitKeys.addAll(frequentIndexedCommitTbl.get(repository.getId()));
            final LogEntryEnumerator<GitRepository, GitCommitKey> logEntryEnumerator = 
            		gitRepositoryService.getLogEntries(repository, commitKeys);

            final IndexWriter writer = indexAccessor.getIndexWriter(getIndexPath().getPath(), false, ANALYZER);

            try {
                final IndexReader reader = indexAccessor.getIndexReader(getIndexPath().getPath());

                try {
                	while (logEntryEnumerator.hasNext()) {
                		final LogEntry<GitRepository, GitCommitKey> logEntry = logEntryEnumerator.next();
                		if (TextUtils.stringSet(logEntry.getMessage()) && isKeyInString(logEntry)) {
                			if (!hasDocument(repository, logEntry.getCommitKey(), reader)) {
                				final Document doc = getDocument(repository, logEntry);
                				logger.debug("Indexing repository: " + repository.getId() + ", commit: " + 
                						logEntry.getCommitKey().marshal());
                				writer.addDocument(doc);
                				if (frequentIndexedCount++ % 100 == 0) {
                					frequentIndexedCommitTbl.get(repository.getId()).add(logEntry.getCommitKey());
                				}
                				// Update the latest branches indexed. Branches should never be null while indexing
                				if (branchIndexedCommitTbl.get(repository.getId()).get(logEntry.getBranches()) != null) {
                					if (logEntry.getCommitKey().compareTo(branchIndexedCommitTbl.get(repository.getId())
                							.get(logEntry.getBranches())) > 0) {
                						branchIndexedCommitTbl.get(repository.getId()).put(logEntry.getBranches(), logEntry.getCommitKey());
                					}
                				}
                			}
                		}
                	}
                } finally {
                    reader.close();
                }
                // 'Commit' to global state
                this.branchIndexedCommitTbl.clear();
                this.branchIndexedCommitTbl.putAll(branchIndexedCommitTbl);
                this.frequentIndexedCommitTbl.clear();
                this.frequentIndexedCommitTbl.putAll(frequentIndexedCommitTbl);
                this.frequentIndexedCount = frequentIndexedCount;
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            logger.warn("Unable to index repository '" + repository.getDisplayName() + "'", e);
        }
    }
    
    /**
     * Works out whether a given commit for the specified repository is already in the index or not.
     * 
     * @param gitRepository the repository. Must not be {@code null}
     * @param commitKey the commit. Must not be {@code null}
     * @param reader the index reader. Must not be {@code null}
     * @throws IndexException if a problem occurs reading the index
     */
    private boolean hasDocument(final GitRepository gitRepository, final GitCommitKey commitKey, final IndexReader reader) 
    		throws IndexException {
    	assert gitRepository != null : "gitRepository must not be null";
    	assert commitKey != null : "commitKey must not be null";
    	assert reader != null : "reader must not be null";
    	
        final IndexSearcher searcher = new IndexSearcher(reader);
        try {
	        try {
	            final TermQuery repoQuery = new TermQuery(new Term(FIELD_REPOSITORY, String.valueOf(gitRepository.getId())));
	            final TermQuery revQuery = new TermQuery(new Term(FIELD_COMMITKEY, commitKey.marshal()));
	            final BooleanQuery repoAndRevQuery = new BooleanQuery();
	
	            repoAndRevQuery.add(repoQuery, BooleanClause.Occur.MUST);
	            repoAndRevQuery.add(revQuery, BooleanClause.Occur.MUST);
	
	            final TopDocs hits = searcher.search(repoAndRevQuery, MAX_COMMITS);
	
	            if (hits.totalHits == 1) {
	                return true;
	            } else if (hits.totalHits == 0) {
	                return false;
	            } else {
	                throw new IndexException("Found MORE than one document for commit key: " + commitKey.marshal() + ", repository=" + gitRepository.getId());
	            }
	        } finally {
	            searcher.close();
	        }
        } catch (final IOException ioe) {
        	throw new IndexException("Error accessing index", ioe);
        }
    }

    /**
     * Creates the index query that finds all log entries for the specified {@code issue}.
     * 
     * @param issue the issue to get log entries for. Must not be {@code null}
     * @return the query. Never {@code null}
     */
    private Query createQueryByIssueKey(final Issue issue) {
    	Validate.notNull(issue, "issue must not be null");
    	
        final BooleanQuery query = new BooleanQuery();

        // Add current key
        query.add(new TermQuery(new Term(FIELD_ISSUEKEY, issue.getKey())), BooleanClause.Occur.SHOULD);

        // Add all previous keys
        final Collection<String> previousIssueKeys = changeHistoryManager.getPreviousIssueKeys(issue.getId());
        for (final String previousIssueKey : previousIssueKeys) {
            TermQuery termQuery = new TermQuery(new Term(FIELD_ISSUEKEY, previousIssueKey));
            query.add(termQuery, BooleanClause.Occur.SHOULD);
        }

        return query;
    }
    
    /**
     * Creates a new Lucene document for the supplied log entry. This method is used when indexing
     * commits, not during retrieval.
     *
     * @param gitRepository the repository to which {@code logEntry} belongs. Must not be {@code null}
     * @param logEntry the log entry that is about to be indexed. Must not be {@code null}
     * @return a Lucene document object that is ready to be added to an index. Never {@code null}
     */
    private Document getDocument(final GitRepository gitRepository, final LogEntry<GitRepository, GitCommitKey> logEntry) {
    	assert gitRepository != null : "gitRepository must not be null";
    	assert logEntry != null : "logEntry must not be null";
    	
        final Document doc = new Document();

        doc.add(new Field(FIELD_MESSAGE, logEntry.getMessage(), Field.Store.YES, Field.Index.NOT_ANALYZED));

        if (logEntry.getAuthorName() != null) {
            doc.add(new Field(FIELD_AUTHOR, logEntry.getAuthorName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        doc.add(new Field(FIELD_REPOSITORY, String.valueOf(gitRepository.getId()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_COMMITKEY, logEntry.getCommitKey().marshal(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        for (final String branch : logEntry.getBranches()) {
        	doc.add(new Field(FIELD_BRANCH, branch, Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        if (logEntry.getDate() != null) {
            doc.add(new Field(FIELD_DATE, DateTools.dateToString(logEntry.getDate(), Resolution.SECOND), Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        // relevant issue keys
        final List<String> keys = getIssueKeysFromString(logEntry);

        // Relevant project keys. Used to avoid adding duplicate projects.
        final Map<String, String> projects = new HashMap<String, String>();

        for (final String issueKey : keys) {
            doc.add(new Field(FIELD_ISSUEKEY, issueKey, Field.Store.YES, Field.Index.NOT_ANALYZED));
            String projectKey = getProjectKeyFromIssueKey(issueKey);
            if (!projects.containsKey(projectKey)) {
                projects.put(projectKey, projectKey);
                doc.add(new Field(FIELD_PROJECTKEY, projectKey, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
        }

        return doc;
    }
    
    /**
     * Gets the last {@code CommitKey}s indexed by the indexer for repository {@code repository}.
     * 
     * @param repository the repository to get the last indexed commits for. Must not be {@code null}
     * @return the last commit key map of branch -> commit. Never {@code null}
     * @throws IndexException
     * @throws IOException
     */
    private Set<GitCommitKey> updateFrequentIndexedCommitTable(final AbstractRepository repository) throws IndexException {
    	assert repository != null : "repository must not be null";
    	
        if (logger.isDebugEnabled()) {
            logger.debug("Updating last commit indexed.");
        }

        // find all log entries that have already been indexed for the specified repository
        // (i.e. all logs that have been associated with issues in JIRA)
        final Set<GitCommitKey> commitKeySet = new HashSet<GitCommitKey>();

        final IndexReader reader;
        try {
            reader = IndexReader.open(FSDirectory.open(new File(getIndexPath().getPath()), new SimpleFSLockFactory()));
        } catch (final IOException e) {
            throw new IndexException("Problem with path " + getIndexPath().getPath() + ": " + e.getMessage(), e);
        }
        
        final IndexSearcher searcher = new IndexSearcher(reader);
        try {
	        try {
	            final TopDocs hits = searcher.search(new TermQuery(new Term(FIELD_REPOSITORY, String.valueOf(repository.getId()))), 
	            		MAX_COMMITS, new Sort(new SortField(FIELD_DATE, SortField.STRING, true)));
	
	            int count = 0;
	            for (int i = 0; i < Math.min(hits.totalHits, MAX_COMMITS); ++i) {
	                final Document doc = searcher.doc(hits.scoreDocs[i].doc);
	                final GitCommitKey commitKey = GitCommitKey.unmarshal(doc.get(FIELD_COMMITKEY));
	                if (count++ % 100 == 0) {
	                	commitKeySet.add(commitKey);
	                }
	            }
	        } finally {
	        	searcher.close();
	            reader.close();
	        }
        } catch (final IOException ioe) {
        	throw new IndexException("IO error while reading from index for repository: " + repository.getId(), ioe);
        }

        return commitKeySet;
    }
    
    /**
     * Gets the project key from the supplied {@code issueKey}.
     * 
     * @param issueKey the issue key. Must not be {@code null}
     * @return the project key. Never {@code null}
     */
    private String getProjectKeyFromIssueKey(final String issueKey) {
    	assert issueKey != null : "issueKey must not be null";
    	
        final String issueKeyUpperCase = StringUtils.upperCase(issueKey);
        return JiraKeyUtils.getFastProjectKeyFromIssueKey(issueKeyUpperCase);
    }
    
    /**
     * Gets a list of actual issue keys from the supplied {@code logEntry}'s log message.
     * 
     * @param logEntry the log entry. Must not be {@code null}
     * @return the list of issue keys. Never {@code null}
     */
    private List<String> getIssueKeysFromString(final LogEntry<GitRepository, GitCommitKey> logEntry) {
    	assert logEntry != null : "logEntry must not be null";
    	
        final String logMessageUpperCase = StringUtils.upperCase(logEntry.getMessage());
        return JiraKeyUtils.getIssueKeysFromString(logMessageUpperCase);
    }
    
    /**
     * Gets whether there is an issue key within the specified {@code logEntry}.
     * 
     * @param logEntry the log entry to check. Must not be {@code null}
     * @return true if there is an issue key in the entry's message; false otherwise
     */
    private boolean isKeyInString(final LogEntry<GitRepository, GitCommitKey> logEntry) {
    	assert logEntry != null : "logEntry must not be null";
    	
        final String logMessageUpperCase = StringUtils.upperCase(logEntry.getMessage());
        return JiraKeyUtils.isKeyInString(logMessageUpperCase);
    }
}
