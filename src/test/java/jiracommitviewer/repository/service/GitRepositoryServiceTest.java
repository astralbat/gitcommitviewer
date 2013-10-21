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
import jiracommitviewer.domain.CommitFile;
import jiracommitviewer.domain.DeletedCommitFile;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.domain.ModifiedCommitFile;
import jiracommitviewer.domain.RenamedCommitFile;
import jiracommitviewer.repository.exception.RepositoryException;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
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
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, 
				new GitCommitKey("7bf726cd31e2194fadf211ffb16e66328932b9a7", 
				getGitDateFromString("Mon Sep 9 20:33:21 2013 +0100")));
		Assert.assertTrue(enumerator.hasNext());
		
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		Assert.assertEquals(repository, logEntry.getRepository());
		Assert.assertEquals("firstbranch", logEntry.getBranch());
		Assert.assertEquals("Mark Barrett", logEntry.getAuthor());
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
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, 
				new GitCommitKey("7bf726cd31e2194fadf211ffb16e66328932b9a7", 
				getGitDateFromString("Mon Sep 9 20:33:21 2013 +0100")));
		Assert.assertTrue(enumerator.hasNext());
		
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		List<CommitFile> committedFiles = logEntry.getCommittedFiles();
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
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, 
				new GitCommitKey("1d255e4add5a2954ce2f7dd8959d0496382a3357", 
				getGitDateFromString("Mon Sep 9 20:34:40 2013 +0100")));
		Assert.assertTrue(enumerator.hasNext());
		
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		List<CommitFile> committedFiles = logEntry.getCommittedFiles();
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
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, 
				new GitCommitKey("bf817175eeb8ac21985bca39e95598f7e879f6f2", 
				getGitDateFromString("Mon Sep 9 20:35:30 2013 +0100")));
		Assert.assertTrue(enumerator.hasNext());
		
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		List<CommitFile> committedFiles = logEntry.getCommittedFiles();
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
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, 
				new GitCommitKey("5588f1c9292b9c5e9ef553620098e48cf8d165e4", 
				getGitDateFromString("Mon Sep 9 20:39:10 2013 +0100")));
		Assert.assertTrue(enumerator.hasNext());
		
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		List<CommitFile> committedFiles = logEntry.getCommittedFiles();
		Assert.assertEquals(1, committedFiles.size());
		Assert.assertTrue(committedFiles.get(0) instanceof DeletedCommitFile);
		Assert.assertEquals("file2.txt", ((DeletedCommitFile)committedFiles.get(0)).getPath());
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
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, 
				new GitCommitKey("153e661a733fc8d1dbd0ec5b6c14c572c1bef8ae", 
				getGitDateFromString("Mon Sep 9 20:54:20 2013 +0100")));
		Assert.assertTrue(enumerator.hasNext());
		
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		List<CommitFile> committedFiles = logEntry.getCommittedFiles();
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
		
		LogEntryEnumerator<GitRepository, GitCommitKey> enumerator = gitRepositoryService.getLogEntries(repository, 
				new GitCommitKey("c1c33efe62aeeb02aa568e9075577f61c48a5568", 
				getGitDateFromString("Wed Sep 18 19:55:02 2013 +0100")));
		Assert.assertTrue(enumerator.hasNext());
		
		LogEntry<GitRepository, GitCommitKey> logEntry = enumerator.next();
		Assert.assertEquals("firstbranch", logEntry.getBranch());
		List<CommitFile> committedFiles = logEntry.getCommittedFiles();
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
