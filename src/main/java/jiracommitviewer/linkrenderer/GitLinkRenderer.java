package jiracommitviewer.linkrenderer;

import jiracommitviewer.domain.AbstractPathCommitFile;
import jiracommitviewer.domain.AddedCommitFile;
import jiracommitviewer.domain.CopiedCommitFile;
import jiracommitviewer.domain.DeletedCommitFile;
import jiracommitviewer.domain.GitCommitKey;
import jiracommitviewer.domain.GitRepository;
import jiracommitviewer.domain.LinkFormatter;
import jiracommitviewer.domain.LogEntry;
import jiracommitviewer.domain.ModifiedCommitFile;
import jiracommitviewer.domain.RenamedCommitFile;

import org.apache.commons.lang3.Validate;

/**
 * The default link renderer for Git
 */
public class GitLinkRenderer implements LinkRenderer<GitRepository, GitCommitKey> {
	
	private LinkFormatter linkFormatter;
    
    public GitLinkRenderer(final LinkFormatter linkFormatter) {
    	this.linkFormatter = linkFormatter;
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public String getChangesetLink(final LogEntry<GitRepository, GitCommitKey> logEntry) {
		Validate.notNull(logEntry, "logEntry must not be null");
		
		return replacePlaceholders(linkFormatter.getChangesetFormat(), logEntry.getCommitKey().getCommitHash(), 
			logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), null, 
			logEntry.getCommitKey().getCommitHash().substring(0, 5) + "...");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileAddedLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final AddedCommitFile commitFile) {
		Validate.notNull(logEntry, "logEntry must not be null");
		Validate.notNull(commitFile, "commitFile must not be null");
		
		return replacePlaceholders(linkFormatter.getFileAddedFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getPath(),
				commitFile.getPath());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileModifiedLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final ModifiedCommitFile commitFile) {
		Validate.notNull(logEntry, "logEntry must not be null");
		Validate.notNull(commitFile, "commitFile must not be null");
		
		return replacePlaceholders(linkFormatter.getFileModifiedFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getPath(),
				commitFile.getPath());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileReplacedLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final RenamedCommitFile commitFile) {
		Validate.notNull(logEntry, "logEntry must not be null");
		Validate.notNull(commitFile, "commitFile must not be null");
		
		return replacePlaceholders(linkFormatter.getFileReplacedFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getFromPath(),
				commitFile.getFromPath());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileRenamedLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final RenamedCommitFile commitFile) {
		Validate.notNull(logEntry, "logEntry must not be null");
		Validate.notNull(commitFile, "commitFile must not be null");
		
		return replacePlaceholders(linkFormatter.getFileAddedFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getToPath(),
				commitFile.getToPath());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileDeletedLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final DeletedCommitFile commitFile) {
		return replacePlaceholders(linkFormatter.getFileDeletedFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getPath(),
				commitFile.getPath());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileCopiedLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final CopiedCommitFile commitFile) {
		return replacePlaceholders(linkFormatter.getFileAddedFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getFromPath(),
				commitFile.getFromPath());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileReplacedLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final CopiedCommitFile commitFile) {
		return replacePlaceholders(linkFormatter.getFileReplacedFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getToPath(),
				commitFile.getToPath());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileViewLink(final LogEntry<GitRepository, GitCommitKey> logEntry, final AbstractPathCommitFile commitFile) {
		return replacePlaceholders(linkFormatter.getFileViewFormat(), logEntry.getCommitKey().getCommitHash(), 
				logEntry.getParentCommitKey() == null ? null : logEntry.getParentCommitKey().getCommitHash(), commitFile.getPath(),
				commitFile.getPath());
	}

	/**
	 * Replaces placeholders found within the string form of {@code url} with the arguments provided.
	 * Equivalent placeholders replaced are:
	 * <pre>
	 * ${id}
	 * ${parent}
	 * ${path}
	 * </pre>
	 * 
	 * @param url the url whose placeholders to replace. Must not be {@code null}
	 * @param id the identifier for the commit, used to replace ${id}. Ignored if {@code null}
	 * @param parent the parent identifier of the parent commit used to replace ${parent}. Ignored if {@code null}
	 * @param path the path of the particular commit file, used to replace ${path}. Ignored if {@code null}
	 * @parma linkText the text of the link. Must not be {@code null}
	 * @return the complete HTML anchor tag. Never {@code null}
	 */
	private String replacePlaceholders(final String url, String id, final String parent, final String path,
			final String linkText) {
		assert url != null : "url must not be null";
		assert linkText != null : "linkText must not be null";
		
		String sUrl = url;
		if (id != null) {
			sUrl = sUrl.replace("${id}", id);
		}
		if (parent != null) {
			sUrl = sUrl.replace("${parent}", parent);
		}
		if (path != null) {
			sUrl = sUrl.replace("${path}", path);
		}
		
		return "<a href=\"" + sUrl + "\">" + linkText + "</a>";
	}
}
