package jiracommitviewer.domain;


/**
 * Represents an additional file as part of a {@link LogEntry}.
 * 
 * @author mark
 */
public final class AddedCommitFile extends AbstractPathCommitFile {

	public AddedCommitFile(final String path) {
		super(path);
	}
}
