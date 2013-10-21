package it.jiracommitviewer.linkrenderer;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jiracommitviewer.domain.AddedCommitFile;
import jiracommitviewer.domain.CommitFile;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LinkFormatter;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.linkrenderer.GitLinkRenderer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link GitLinkRenderer} class.
 * 
 * @author mark
 */
public class GitLinkRendererTest {

	/**
	 * Tests that the ${path} placeholder can be replaced.
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void testReplacePath() throws MalformedURLException {
		LinkFormatter formatter = new LinkFormatter();
		formatter.setFileAddedFormat("http://link.to.web/${path}");
		GitLinkRenderer renderer = new GitLinkRenderer(formatter);
		List<CommitFile> commitFiles = new ArrayList<CommitFile>();
		commitFiles.add(new AddedCommitFile("a/path"));
		LogEntry<GitRepository, GitCommitKey> logEntry = createBasicLogEntry(commitFiles);
		
		Assert.assertEquals("<a href=\"http://link.to.web/a/path\">a/path</a>", 
				renderer.getFileAddedLink(logEntry, (AddedCommitFile)commitFiles.get(0)));
	}
	
	/**
	 * Tests that the ${id} placeholder can be replaced.
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void TestReplaceId() throws MalformedURLException {
		LinkFormatter formatter = new LinkFormatter();
		formatter.setFileAddedFormat("http://link.to.web/${id}");
		GitLinkRenderer renderer = new GitLinkRenderer(formatter);
		List<CommitFile> commitFiles = new ArrayList<CommitFile>();
		commitFiles.add(new AddedCommitFile("a/path"));
		LogEntry<GitRepository, GitCommitKey> logEntry = createBasicLogEntry(commitFiles);
		
		Assert.assertEquals("<a href=\"http://link.to.web/1234\">a/path</a>", 
				renderer.getFileAddedLink(logEntry, (AddedCommitFile)commitFiles.get(0)));
	}
	
	/**
	 * Tests that the ${parent} placeholder can be replaced.
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void TestReplaceParent() throws MalformedURLException {
		LinkFormatter formatter = new LinkFormatter();
		formatter.setFileAddedFormat("http://link.to.web/${parent}");
		GitLinkRenderer renderer = new GitLinkRenderer(formatter);
		List<CommitFile> commitFiles = new ArrayList<CommitFile>();
		commitFiles.add(new AddedCommitFile("a/path"));
		LogEntry<GitRepository, GitCommitKey> logEntry = createBasicLogEntry(commitFiles);
		
		Assert.assertEquals("<a href=\"http://link.to.web/12345\">a/path</a>", 
				renderer.getFileAddedLink(logEntry, (AddedCommitFile)commitFiles.get(0)));
	}
	
	/**
	 * Gets a very simple log entry containing the supplied {@code commitFiles}.
	 * 
	 * @param commitFiles the commit files to add to the created log entry. Must not be {@code null}
	 * @return the created log entry. Never {@code null}
	 */
	private LogEntry<GitRepository, GitCommitKey> createBasicLogEntry(final List<CommitFile> commitFiles) {
		assert commitFiles != null : "commitFiles must not be null";
		
		final GitRepository repository = new GitRepository("id");
		return new LogEntry<GitRepository, GitCommitKey>(repository, "master", new GitCommitKey("1234", 0), new GitCommitKey("12345", 0), 
				"author", new Date(), "msg", commitFiles);
	}
}
