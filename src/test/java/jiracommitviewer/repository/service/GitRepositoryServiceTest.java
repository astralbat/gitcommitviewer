package jiracommitviewer.repository.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jiracommitviewer.RepositoryManager;
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
import jiracommitviewer.repository.RepositoryTestUtils;
import jiracommitviewer.repository.exception.RepositoryException;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.atlassian.jira.config.util.IndexPathManager;

/**
 * Tests surrounding {@link RepositoryManager}.
 * 
 * @author mark
 */
public class GitRepositoryServiceTest {
	
	@Mocked
	private IndexPathManager indexPathManager;
	
	private GitRepositoryService gitRepositoryService = new DefaultGitRepositoryService();

	/**
	 * Tests that a repository can be activated.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException 
	 * @throws RepositoryException 
	 */
	@Test
	public void testActivate() throws URISyntaxException, IOException, RepositoryException {
		// Delete the target repository so it can be recreated
		final URL repositoryTargetUrl = ClassLoader.getSystemResource("repository");
		final File repositoryTarget = new File(new File(repositoryTargetUrl.toURI()), "target");
		if (repositoryTarget.exists()) {
			FileUtils.deleteDirectory(repositoryTarget);
		}
		
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.activate(repository);
	}
	
	/**
	 * Tests that activate is successful and doesn't damage the repository if it already exists.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws RepositoryException 
	 */
	@Test
	public void testActivateWhenClonedRepositoryAlreadyExists() throws URISyntaxException, IOException, RepositoryException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		// Clone the repository
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		
		// Activate it again for the test
		gitRepositoryService.activate(repository);
		
		Assert.assertTrue(gitRepositoryService.isCloned(repository));
	}
	
	/**
	 * Gets a single log entry from a commit and checks its basic attributes.
	 * 
	 * @throws RepositoryException
	 * @throws URISyntaxException
	 * @throws ParseException
	 */
	@Test
	public void testGetLogEntry() throws RepositoryException, URISyntaxException, ParseException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		LogEntry<GitRepository, GitCommitKey> logEntry = null;
		while (enumerator.hasNext()) {
			logEntry = enumerator.next();
			if (logEntry.getCommitKey().getCommitHash().equals("c66feec2fc0ad9887949c04839232e17b440c690")) {
				break;
			}
		}
		
		Assert.assertNotNull("No log entries", logEntry);
		Assert.assertEquals(repository, logEntry.getRepository());
		Assert.assertEquals("firstbranch", logEntry.getBranch());
		Assert.assertEquals("Mark Barrett", logEntry.getAuthorName());
		Assert.assertEquals("GCV-1 Added a file", logEntry.getMessage().trim());
		Assert.assertEquals("c66feec2fc0ad9887949c04839232e17b440c690", ((GitCommitKey)logEntry.getCommitKey()).getCommitHash());
		Assert.assertEquals(getGitDateFromString("Mon Sep 9 20:16:53 2013 +0100"), ((GitCommitKey)logEntry.getCommitKey()).getCommitTime());
		Assert.assertEquals(new Date((long)getGitDateFromString("Mon Sep 9 20:16:53 2013 +0100") * 1000), logEntry.getDate());
	}
	
	/**
	 * Checks for a committed file of type ADD.
	 * 
	 * @throws URISyntaxException 
	 * @throws RepositoryException 
	 * @throws ParseException 
	 */
	@Test
	public void testGetAddCommittedFile() throws URISyntaxException, RepositoryException, ParseException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		LogEntry<GitRepository, GitCommitKey> logEntry = null;
		while (enumerator.hasNext()) {
			logEntry = enumerator.next();
			if (logEntry.getCommitKey().getCommitHash().equals("c66feec2fc0ad9887949c04839232e17b440c690")) {
				break;
			}
		}
		
		Assert.assertNotNull("No log entries", logEntry);
		List<CommitFile> committedFiles = logEntry.getCommitFiles();
		Assert.assertEquals(1, committedFiles.size());
		Assert.assertTrue(committedFiles.get(0) instanceof AddedCommitFile);
		Assert.assertEquals("file1.txt", ((AddedCommitFile)committedFiles.get(0)).getPath());
	}
	
	/**
	 * Checks for a committed file of type RENAME.
	 * 
	 * @throws URISyntaxException 
	 * @throws RepositoryException 
	 * @throws ParseException 
	 */
	@Test
	public void testGetRenameCommittedFile() throws URISyntaxException, RepositoryException, ParseException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		LogEntry<GitRepository, GitCommitKey> logEntry = null;
		while (enumerator.hasNext()) {
			logEntry = enumerator.next();
			if (logEntry.getCommitKey().getCommitHash().equals("7bf726cd31e2194fadf211ffb16e66328932b9a7")) {
				break;
			}
		}
		
		Assert.assertNotNull("No log entries", logEntry);
		List<CommitFile> committedFiles = logEntry.getCommitFiles();
		Assert.assertEquals(1, committedFiles.size());
		Assert.assertTrue(committedFiles.get(0) instanceof RenamedCommitFile);
		Assert.assertEquals("file1.txt", ((RenamedCommitFile)committedFiles.get(0)).getFromPath());
		Assert.assertEquals("file2.txt", ((RenamedCommitFile)committedFiles.get(0)).getToPath());
	}
	
	/**
	 * Checks for a committed file of type UPDATE.
	 * 
	 * @throws URISyntaxException 
	 * @throws RepositoryException 
	 * @throws ParseException 
	 */
	@Test
	public void testGetModifiedCommittedFile() throws URISyntaxException, RepositoryException, ParseException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		LogEntry<GitRepository, GitCommitKey> logEntry = null;
		while (enumerator.hasNext()) {
			logEntry = enumerator.next();
			if (logEntry.getCommitKey().getCommitHash().equals("1d255e4add5a2954ce2f7dd8959d0496382a3357")) {
				break;
			}
		}
		
		Assert.assertNotNull("No log entries", logEntry);
		List<CommitFile> committedFiles = logEntry.getCommitFiles();
		Assert.assertEquals(1, committedFiles.size());
		Assert.assertTrue(committedFiles.get(0) instanceof ModifiedCommitFile);
		Assert.assertEquals("file2.txt", ((ModifiedCommitFile)committedFiles.get(0)).getPath());
	}
	
	/**
	 * Checks for a committed file of type DELETE.
	 * 
	 * @throws URISyntaxException 
	 * @throws RepositoryException 
	 * @throws ParseException 
	 */
	@Test
	public void testGetDeleteCommittedFile() throws URISyntaxException, RepositoryException, ParseException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		LogEntry<GitRepository, GitCommitKey> logEntry = null;
		while (enumerator.hasNext()) {
			logEntry = enumerator.next();
			if (logEntry.getCommitKey().getCommitHash().equals("bf817175eeb8ac21985bca39e95598f7e879f6f2")) {
				break;
			}
		}
		
		Assert.assertNotNull("No log entries", logEntry);
		List<CommitFile> committedFiles = logEntry.getCommitFiles();
		Assert.assertEquals(1, committedFiles.size());
		Assert.assertTrue(committedFiles.get(0) instanceof DeletedCommitFile);
		Assert.assertEquals("file2.txt", ((DeletedCommitFile)committedFiles.get(0)).getPath());
	}
	
	/**
	 * Checks for a committed file of type COPY.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws RepositoryException
	 */
	@Test
	@Ignore("JGit copy detection isn't great. This will have to wait")
	public void testGetCopyCommittedFile() throws URISyntaxException, IOException, RepositoryException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		// Create a repository and commit a directory
		final GitRepository repository = RepositoryTestUtils.getCreatedRepository(gitRepositoryService);
		RepositoryTestUtils.createRepositoryFile(repository, new File("sourcedir/testfile"), "somecontent".getBytes());
		gitRepositoryService.commit(repository, new Commit<GitRepository>(repository, "author", "message", 
				new AddedCommitFile("sourcedir")));
		
		// Copy that directory and commit again
		FileUtils.copyDirectory(new File(RepositoryTestUtils.getRepositoryBase(repository), "sourcedir"), 
				new File(RepositoryTestUtils.getRepositoryBase(repository), "destdir"));
		gitRepositoryService.commit(repository, new Commit<GitRepository>(repository, "author", "message", 
				new AddedCommitFile("destdir")));
		
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		// Get both log entries, ignoring the first
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		Assert.assertTrue(enumerator.hasNext());
		enumerator.next();
		Assert.assertTrue(enumerator.hasNext());
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		
		// Check that the commit file on the second log entry is a copy commit file with the correct path
		List<CommitFile> commitFiles = logEntry.getCommitFiles();
		Assert.assertEquals(1, commitFiles.size());
		Assert.assertTrue(commitFiles.get(0) instanceof CopiedCommitFile);
		Assert.assertEquals("destdir", ((CopiedCommitFile)commitFiles.get(0)).getToPath());
	}
	
	/**
	 * A merge has two parents and is more complex. Check that files from both lineages are included.
	 * 
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 * @throws ParseException
	 */
	@Test
	public void testCommittedFilesOfMerge() throws URISyntaxException, RepositoryException, ParseException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		LogEntry<GitRepository, GitCommitKey> logEntry = null;
		while (enumerator.hasNext()) {
			logEntry = enumerator.next();
			if (logEntry.getCommitKey().getCommitHash().equals("970d660290bc24299fa8997f298bd02a310776d3")) {
				break;
			}
		}
		
		Assert.assertNotNull("No log entries", logEntry);
		List<CommitFile> committedFiles = logEntry.getCommitFiles();
		Assert.assertEquals(2, committedFiles.size());
		Assert.assertTrue(committedFiles.get(0) instanceof AddedCommitFile);
		Assert.assertTrue(committedFiles.get(1) instanceof AddedCommitFile);
		Assert.assertEquals("firstbranchfile.txt", ((AddedCommitFile)committedFiles.get(0)).getPath());
		Assert.assertEquals("secondbranchfile.txt", ((AddedCommitFile)committedFiles.get(1)).getPath());
	}
	
	/**
	 * Test that we can see commits from a branch other than master.
	 * 
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 * @throws ParseException
	 */
	@Test
	public void testCommittedFilesOfAlternateBranch() throws URISyntaxException, RepositoryException, ParseException {
		new NonStrictExpectations() {{
			setField(gitRepositoryService, indexPathManager);
			indexPathManager.getPluginIndexRootPath(); result = new File(ClassLoader.getSystemResource("indexes").toURI()).getPath();
		}};
		
		GitRepository repository = getSourceRepository();
		gitRepositoryService.cloneRepository(repository);
		gitRepositoryService.fetch(repository);
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, null);
		LogEntry<GitRepository, GitCommitKey> logEntry = null;
		while (enumerator.hasNext()) {
			logEntry = enumerator.next();
			if (logEntry.getCommitKey().getCommitHash().equals("095014f90aac621901d29e1e3986ad5f9e52361a")) {
				break;
			}
		}
		
		Assert.assertNotNull("No log entries", logEntry);
		Assert.assertEquals("firstbranch", logEntry.getBranch());
		List<CommitFile> committedFiles = logEntry.getCommitFiles();
		Assert.assertEquals(1, committedFiles.size());
		Assert.assertTrue(committedFiles.get(0) instanceof AddedCommitFile);
	}
	
	/**
	 * Gets the source repository used for testing.
	 * 
	 * @return the source repository. Never {@code null}
	 * @throws URISyntaxException if there was a programmatic error getting the repository
	 */
	private GitRepository getSourceRepository() throws URISyntaxException {
		GitRepository repository = new GitRepository("id");
		repository.setUri("file://" + new File(ClassLoader.getSystemResource("repository/source").toURI()).getAbsolutePath());
		return repository;
	}
	
	/**
	 * Gets a Git date integer from the supplied date string in the default git log format.
	 * 
	 * @param s the date string. Must not be {@code null}
	 * @return the date integer
	 * @throws ParseException if the supplied date string is not in the correct format
	 */
	private int getGitDateFromString(final String s) throws ParseException {
		assert s != null : "s must not be null";
		
		SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
		return (int) (df.parse(s).getTime() / 1000);
	}
}
