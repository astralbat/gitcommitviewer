package jiracommitviewer.repository.index;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jiracommitviewer.RepositoryManager;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.index.CommitIndexer;
import jiracommitviewer.index.GitCommitIndexer;
import jiracommitviewer.index.exception.IndexException;
import jiracommitviewer.repository.exception.RepositoryException;
import jiracommitviewer.repository.service.DefaultGitRepositoryService;
import jiracommitviewer.repository.service.GitRepositoryService;
import mockit.Deencapsulation;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.NonStrictExpectations;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.JiraKeyUtils;

/**
 * Tests for {@link GitCommitIndexer}.
 * 
 * @author mark
 */
public class GitCommitIndexerTest {

	@Injectable
	private RepositoryManager repositoryManager;
	@Injectable
	private GitRepositoryService gitRepositoryService;
	@Injectable
	private VersionManager versionManager;
	@Injectable
	private IssueManager issueManager;
	@Injectable
	private PermissionManager permissionManager;
	@Injectable
	private ChangeHistoryManager changeHistoryManager;
	@Injectable
	private IndexPathManager indexPathManager;
	
	private CommitIndexer<GitRepository, GitCommitKey> commitIndexer;
	private final GitRepositoryService realRepositoryService = new DefaultGitRepositoryService();
	
	@Before
	public void init() {
		commitIndexer = new GitCommitIndexer();
		Deencapsulation.setField(commitIndexer, repositoryManager);
		Deencapsulation.setField(commitIndexer, realRepositoryService);
		Deencapsulation.setField(commitIndexer, versionManager);
		Deencapsulation.setField(commitIndexer, issueManager);
		Deencapsulation.setField(commitIndexer, permissionManager);
		Deencapsulation.setField(commitIndexer, changeHistoryManager);
		Deencapsulation.setField(commitIndexer, indexPathManager);
		Deencapsulation.setField(realRepositoryService, indexPathManager);
	}
	
	/**
	 * Tests that indexing works at a basic level by indexing the test repository.
	 * 
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 * @throws IndexException 
	 * @throws IOException 
	 */
	@Test
	public void testIndex() throws URISyntaxException, RepositoryException, IndexException, IOException {
		GitRepository repository = new GitRepository("id");
		repository.setUri("file://" + new File(ClassLoader.getSystemResource("repository/source").toURI()).getAbsolutePath());
		
		new NonStrictExpectations() {{
			new MockUp<JiraKeyUtils>() {
				@Mock
				public boolean isKeyInString(final String s) {
					return true;
				}
				@Mock
				public List<String> getIssueKeysFromString(final String s) {
					Set<String> keys = new HashSet<String>();
					Pattern issuePattern = Pattern.compile("GCV-[0-9]+");
					Matcher matcher = issuePattern.matcher(s);
					int index = 0;
					while (matcher.find(index)) {
						keys.add(matcher.group());
						index = matcher.end();
					}
					return new ArrayList<String>(keys);
				}
			};
			
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		FileUtils.deleteDirectory(commitIndexer.getIndexPath());
		commitIndexer.index(repository);
	}
	
	/**
	 * Expects to find a particular commit in the log entries for an issue.
	 * 
	 * @param issue
	 * @throws IndexException
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	@Test
	public void testFindCommitInLogEntriesForIssue(final Issue issue) throws IndexException, URISyntaxException, RepositoryException, IOException {
		new NonStrictExpectations() {{
			issue.getKey(); result = "GCV-1"; minTimes = 1;
			issue.getId(); result = 1L;
			changeHistoryManager.getPreviousIssueKeys(anyLong); result = new ArrayList<String>();
			repositoryManager.parseRepositoryId("id"); result = "id"; minTimes = 1;
			repositoryManager.getRepository("id"); result = new GitRepository("id"); minTimes = 1;
		}};
		
		// Create the index for the test repository
		testIndex();
		
		// Look for a particular commit
		final Iterator<LogEntry<GitRepository, GitCommitKey>> logEntries = commitIndexer.getAllLogEntriesByIssue(issue, 0, 5).iterator();
		Assert.assertTrue("Expected to find marker commit in history", hasCommit("095014f90aac621901d29e1e3986ad5f9e52361a", logEntries));
	}
	
	/**
	 * Expects a particular commit not to be in the log entries for an issue.
	 * 
	 * @param issue
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 * @throws IndexException
	 * @throws IOException
	 */
	@Test
	public void testNotFindCommitInLogEntriesForIssue(final Issue issue) throws URISyntaxException, RepositoryException, IndexException, IOException {
		new NonStrictExpectations() {{
			issue.getKey(); result = "GCV-1"; minTimes = 1;
			issue.getId(); result = 1L;
			changeHistoryManager.getPreviousIssueKeys(anyLong); result = new ArrayList<String>();
			repositoryManager.parseRepositoryId("id"); result = "id"; minTimes = 1;
			repositoryManager.getRepository("id"); result = new GitRepository("id"); minTimes = 1;
		}};
		
		// Create the index for the test repository
		testIndex();
		
		// Look for a particular commit
		final Iterator<LogEntry<GitRepository, GitCommitKey>> logEntries = commitIndexer.getAllLogEntriesByIssue(issue, 0, 5).iterator();
		Assert.assertFalse("Expected to not find marker commit in history", hasCommit("c1c33efe62aeeb02aa568e9075577f61c48a5568", logEntries));
	}
	
	/**
	 * Tests that we find a particular commit in the log entries for a project.
	 * 
	 * @param user
	 * @param issue
	 * @throws IndexException
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testFindCommitInLogEntriesForProject(final User user, final MutableIssue issue) throws IndexException, URISyntaxException, RepositoryException, IOException {
		new NonStrictExpectations() {{
			issueManager.getIssueObject(anyString); result = issue;
			permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, withAny(issue), user); result = true;
			changeHistoryManager.getPreviousIssueKeys(anyLong); result = new ArrayList<String>();
			repositoryManager.parseRepositoryId("id"); result = "id"; minTimes = 1;
			repositoryManager.getRepository("id"); result = new GitRepository("id"); minTimes = 1;
		}};
		
		// Create the index for the test repository
		testIndex();
		
		// Look for a particular commit
		final Iterator<LogEntry<GitRepository, GitCommitKey>> logEntries = 
				commitIndexer.getAllLogEntriesByProject("GCV", user, 0, 5).iterator();
		Assert.assertTrue("Expected to find marker commit in history", hasCommit("095014f90aac621901d29e1e3986ad5f9e52361a", logEntries));
	}
	
	/**
	 * Tests that we find a particular commit in the log entries for a version.
	 * 
	 * @param version
	 * @param user
	 * @param issue
	 * @throws IndexException
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testFindCommitInLogEntriesForVersion(final Version version, final User user, final MutableIssue issue) throws IndexException, URISyntaxException, RepositoryException, IOException {
		new NonStrictExpectations() {{
			versionManager.getIssuesWithFixVersion(version); result = Arrays.asList(new Issue[] { issue });
			issueManager.getIssueObject(anyString); result = issue;
			issue.getKey(); result = "GCV-1";
			permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, withAny(issue), user); result = true;
			changeHistoryManager.getPreviousIssueKeys(anyLong); result = new ArrayList<String>();
			repositoryManager.parseRepositoryId("id"); result = "id"; minTimes = 1;
			repositoryManager.getRepository("id"); result = new GitRepository("id"); minTimes = 1;
		}};
		
		// Create the index for the test repository
		testIndex();
		
		// Look for a particular commit
		final Iterator<LogEntry<GitRepository, GitCommitKey>> logEntries = 
				commitIndexer.getAllLogEntriesByVersion(version, user, 0, 5).iterator();
		Assert.assertTrue("Expected to find marker commit in history", hasCommit("095014f90aac621901d29e1e3986ad5f9e52361a", logEntries));
	}
	
	/**
	 * Checks that a particular commit hash appears in the list of supplied {@code logEntries}.
	 * 
	 * @param hash the has to check for. Must not be {@code null}
	 * @param logEntries the log entries to check. Must not be {@code null}
	 * @return true if found; false if not
	 */
	private boolean hasCommit(final String hash, final Iterator<LogEntry<GitRepository, GitCommitKey>> logEntries) {
		assert hash != null : "hash must not be null";
		assert logEntries != null : "logEntries must not be null";
		
		while (logEntries.hasNext()) {
			final LogEntry<GitRepository, GitCommitKey> logEntry = logEntries.next();
			if (logEntry.getCommitKey().getCommitHash().equals(hash)) {
				return true;
			}
		}
		return false;
	}
}
