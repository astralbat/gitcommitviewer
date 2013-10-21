package jiracommitviewer.domain;

import org.apache.commons.lang3.Validate;

/**
 * Represents an additional file that was copied from another file as part of a {@link LogEntry}.
 * 
 * @author mark
 */
public class CopiedCommitFile implements CommitFile {

	private final String fromPath;
	private final String toPath;
	
	public CopiedCommitFile(final String fromPath, final String toPath) {
		Validate.notNull(fromPath, "fromPath must not be null");
		Validate.notNull(toPath, "toPath must not be null");
		
		this.fromPath = fromPath;
		this.toPath = toPath;
	}
	
	/**
	 * Gets the path of the source file
	 * 
	 * @return the old path. Never {@code null}
	 */
	public String getFromPath() {
		return fromPath;
	}
	
	/**
	 * Gets the path of the new file
	 * 
	 * @return the new path. Never {@code null}
	 */
	public String getToPath() {
		return toPath;
	}
}
