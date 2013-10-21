package jiracommitviewer.domain;


/**
 * Represents a deleted file as part of a {@link LogEntry}.
 * 
 * @author mark
 */
public final class DeletedCommitFile extends AbstractPathCommitFile {

	public DeletedCommitFile(final String path) {
		super(path);
	}
}
