package jiracommitviewer.domain;

import org.apache.commons.lang.Validate;

/**
 * A kind of {@link CommitFile} that affects a single path.
 * 
 * @author mark
 */
public abstract class AbstractPathCommitFile implements CommitFile {

	private String path;
	
	public AbstractPathCommitFile(final String path) {
		Validate.notNull(path, "path must not be null");
		
		this.path = path;
	}
	
	/**
	 * Gets the path that was affected
	 * 
	 * @return the affected path. Never {@code null}
	 */
	public String getPath() {
		return path;
	}
}
