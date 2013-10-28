package jiracommitviewer.repository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.repository.service.GitRepositoryService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.transport.URIish;

public class RepositoryTestUtils {

	/**
	 * Gets a newly created, non-bare repository useful for testing.
	 * <p>
	 * The repository is recreated each time this is called.
	 * 
	 * @param gitRepositoryService the repository service to use for creating the repository. Must not be {@code null}
	 * @return the newly created repository. Never {@code null}
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static GitRepository getCreatedRepository(final GitRepositoryService gitRepositoryService) throws URISyntaxException, 
		IOException {
		Validate.notNull(gitRepositoryService, "gitRepositoryService must not be null");
		
		final GitRepository repository = new GitRepository("id");
		final File repositoryLocation = new File(new File(ClassLoader.getSystemResource("jiracommitviewer/repository").toURI()), 
				"singletest");
		repository.setUri("file://" + repositoryLocation.getAbsolutePath());
		// Delete it in case it already exists
		if (repositoryLocation.isDirectory()) {
			FileUtils.deleteDirectory(repositoryLocation);
		}
		gitRepositoryService.create(repository);
		return repository;
	}
	
	/**
	 * Creates a new file with the repository specified by {@code gitRepository} with the specified
	 * {@code filename} and having content, {@code content}.
	 * 
	 * @param gitRepository the repository to create the new file in. Must have a valid file:// URI and must not be {@code null}
	 * @param path the path of the new file relative to the repository's root location. Must not be {@code null}
	 * @param content the content of the file. Must not be {@code null}
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void createRepositoryFile(final GitRepository gitRepository, final File path, 
			byte[] content) throws IOException, URISyntaxException {
		Validate.notNull(gitRepository, "gitRepository must not be null");
		Validate.notNull(path, "path must not be null");
		Validate.notNull(content, "content must not be null");
		
		final URIish uri = new URIish(gitRepository.getUri());
		if (!"file".equals(uri.getScheme())) {
			throw new IllegalArgumentException("Only file:// URI scheme supported");
		}
		final File repositoryDirectory = new File(uri.getPath());
		final File newFile = new File(repositoryDirectory.getPath() + File.separator + path.getPath());
		
		FileUtils.writeByteArrayToFile(newFile, content);
	}
}
