package jiracommitviewer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import jiracommitviewer.GitManager;
import jiracommitviewer.GitManagerImpl;
import jiracommitviewer.MultipleGitRepositoryManagerImpl;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests surrounding {@link GitManager}.
 * 
 * @author mark
 */
public class GitManagerTest {
	
	@Mocked
	private MultipleGitRepositoryManagerImpl repositoryManager;

	/**
	 * Tests that a repository can be activated.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException 
	 */
	@Test
	public void testActivate() throws URISyntaxException, IOException {
		// Delete the target repository so it can be recreated
		final URL repositoryTargetUrl = ClassLoader.getSystemResource("repository");
		final File repositoryTarget = new File(new File(repositoryTargetUrl.toURI()), "target");
		if (repositoryTarget.exists()) {
			FileUtils.deleteDirectory(repositoryTarget);
		}
		
		new NonStrictExpectations() {{
			repositoryManager.getRepositoryPath("id"); result = repositoryTarget;
		}};
		
		Properties properties = new Properties();	
		properties.setProperty(GitManager.GIT_REPOSITORY_NAME, "id");
		properties.setProperty(GitManager.GIT_URI, "file://" + new File(ClassLoader.getSystemResource("repository/source").toURI()).getAbsolutePath());
		GitManager gitManager = new GitManagerImpl(repositoryManager, properties);
		
		Assert.assertTrue("Repository not active: " + gitManager.getInactiveMessage(), gitManager.isActive());
	}
	
	/**
	 * Tests that activate is successful and doesn't damage the repository if it already exists.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Test
	@Ignore("Still to complete")
	public void testActivateWhenClonedRepositoryAlreadyExists() throws URISyntaxException, IOException {
		final URL repositoryTargetUrl = ClassLoader.getSystemResource("repository");
		final File repositoryTarget = new File(new File(repositoryTargetUrl.toURI()), "target");
		testActivate();
		
		new NonStrictExpectations() {{
			repositoryManager.getRepositoryPath("id"); result = repositoryTarget;
		}};
		
		Properties properties = new Properties();	
		properties.setProperty(GitManager.GIT_REPOSITORY_NAME, "id");
		properties.setProperty(GitManager.GIT_URI, "file://" + new File(ClassLoader.getSystemResource("repository/source").toURI()).getAbsolutePath());
		GitManager gitManager = new GitManagerImpl(repositoryManager, properties);
		
		Assert.assertTrue("Repository not active: " + gitManager.getInactiveMessage(), gitManager.isActive());
	}
}
