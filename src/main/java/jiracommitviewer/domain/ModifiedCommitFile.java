package jiracommitviewer.domain;


/**
 * Represents a modified file as part of a {@link LogEntry}.
 * 
 * @author mark
 */
public final class ModifiedCommitFile extends AbstractPathCommitFile {

	public ModifiedCommitFile(final String path) {
		super(path);
	}
}
