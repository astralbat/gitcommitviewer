package jiracommitviewer.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import jiracommitviewer.DefaultRepositoryManager;
import jiracommitviewer.RepositoryType;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.index.GitCommitIndexer;
import jiracommitviewer.index.exception.IndexException;
import jiracommitviewer.repository.exception.RepositoryException;
import jiracommitviewer.repository.service.GitRepositoryService;
import jiracommitviewer.repository.service.RepositoryServiceHelper;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Tests surrounding {@link DefaultRepositoryManager}.
 * 
 * @author mark
 */
public class DefaultRepositoryManagerTest {

	@Mocked
	private VersionManager versionManager;
	@Mocked
	private IssueManager issueManager;
	@Mocked
	private PermissionManager permissionManager;
	@Mocked
	private ChangeHistoryManager changeHistoryManager;
	@Mocked
	private PluginSettingsFactory pluginSettingsFactory;
	@Mocked
	private RepositoryServiceHelper repositoryServiceHelper;
	@Mocked 
	private GitCommitIndexer gitCommitIndexer;
	@Mocked
	private PluginSettings globalSettings;
	@Mocked
	private PluginSettings projectSettings;
	@Mocked
	private List<String> repositoryList;
	
	private DefaultRepositoryManager defaultRepositoryManager;

	@Before
	public void init() {
		new NonStrictExpectations() {{
			pluginSettingsFactory.createGlobalSettings(); result = globalSettings;
			globalSettings.get(anyString); result = repositoryList;
			repositoryList.iterator(); result = Arrays.asList().iterator();
			pluginSettingsFactory.createSettingsForKey(anyString); result = projectSettings;
			projectSettings.get(anyString); result = new Properties();
		}};
		
		defaultRepositoryManager = new DefaultRepositoryManager(versionManager, issueManager,
				permissionManager, changeHistoryManager, pluginSettingsFactory);
		Deencapsulation.setField(defaultRepositoryManager, gitCommitIndexer);
		Deencapsulation.setField(defaultRepositoryManager, repositoryServiceHelper);
	}
	
	/**
	 * A repository can be removed.
	 * 
	 * @throws IndexException 
	 * @throws RepositoryException 
	 */
	@Test
	public void testRemove(@Mocked final GitRepositoryService gitRepositoryService) 
			throws IndexException, RepositoryException {
		
		new Expectations() {{
			repositoryList.remove(anyString);
			gitCommitIndexer.removeEntries(withInstanceOf(GitRepository.class));
			repositoryServiceHelper.getRepositoryService(withInstanceOf(GitRepository.class)); result = gitRepositoryService;
			gitRepositoryService.remove(withInstanceOf(GitRepository.class));
		}};
		
		final GitRepository repository = (GitRepository)defaultRepositoryManager.createRepository(RepositoryType.GIT);
		defaultRepositoryManager.removeRepository(repository.getId());
	}
}
