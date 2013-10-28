package jiracommitviewer.index;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jiracommitviewer.domain.AbstractCommitKey;
import jiracommitviewer.domain.AbstractRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.index.exception.IndexException;
import jiracommitviewer.repository.exception.RepositoryException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.version.Version;

/**
 * The indexing service that indexes commit history for fast retrieval.
 * 
 * @author mark
 *
 * @param <T>
 */
public interface CommitIndexer<R extends AbstractRepository, K extends AbstractCommitKey<K>> {
	
	// These are names of the fields in the Lucene documents that contain revision info.
    public static final String FIELD_COMMITKEY = "commitkey";
    public static final String FIELD_BRANCH = "branch";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_AUTHOR = "author";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_ISSUEKEY = "key";
    public static final String FIELD_PROJECTKEY = "project";
    public static final String FIELD_REPOSITORY = "repository";

    public static final Analyzer ANALYZER = new LimitTokenCountAnalyzer(new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_30), 10000);

    public static final int MAX_COMMITS = 500;
	
	/**
	 * Notifies the indexer that all indexes for a repository must be removed.
	 * 
	 * @param repository the repository. Must not be {@code null}
	 * @throws IOException if an error occurs whilst removing
	 */
	void removeEntries(R repository) throws IndexException;
	
	/**
	 * Indexes the clone the specified {@code repository}.
	 * 
	 * @param repository the repository to index. Must not be {@code null}
	 * @throws RepositoryException if a problem occurs
	 */
	void index(R repository) throws IndexException, RepositoryException;
	
	/**
     * Gets the path at which indexes are stored for all repositories
     * 
     * @return the index path. Never {@code null}
     */
	File getIndexPath();
	
	/**
	 * Gets all log entries for the specified {@code issue}.
	 * <p>
	 * Only log entries for the specified repository type are returned.
	 * 
	 * @param issue the issue to get log entries for. Must not be {@code null}
	 * @param pageNumber the page of results to get. The first page is 0
	 * @param pageSize the number of results that are in a page
	 * @return the log entries sorted by date in reverse order. Never {@code null}
	 * @throws IndexException if there is a problem reading the index
	 */
	List<LogEntry<R, K>> getAllLogEntriesByIssue(Issue issue, int pageNumber, int pageSize) throws IndexException;
	
	/**
	 * Gets all log entries for the specified {@code project}.
	 * <p>
	 * Entries are only retrieved one page at a time and only for those entries whose issues the requesting {@code user}
	 * has permissions to view.
	 * <p>
	 * Only log entries for the specified repository type are returned.
	 * 
	 * @param projectKey the identifier for the project to get log entries for. Must not be {@code null}
	 * @param user the requesting user. Must not be {@code null}
	 * @param pageNumber the page of results to get. The first page is 0
	 * @param pageSize the number of results that are in a page
	 * @return the requested page. Never {@code null}
	 * @throws IndexException if there is a problem reading the index
	 */
	List<LogEntry<R, K>> getAllLogEntriesByProject(String projectKey, User user, int pageNumber, int pageSize) throws IndexException;
	
	/**
	 * Gets all the log entries for the specified {@code version}.
	 * <p>
	 * Entries are only retrieved one page at a time and only for those entries whose issues the requesting {@code user}
	 * has permissions to view.
	 * <p>
	 * Only log entries for the specified repository type are returned.
	 * 
	 * @param version the version to get the log entries for. Must not be {@code null}
	 * @param user the requesting user. Must not be {@code null}
	 * @param pageNumber the page of results to get. The first page is 0
	 * @param pageSize hte number of results that are in a page
	 * @return the requested page. Never {@code null}
	 * @throws IndexException if there is a problem reading the index
	 */
	List<LogEntry<R, K>> getAllLogEntriesByVersion(Version version, User user, int pageNumber, int pageSize) throws IndexException;
}
