package jiracommitviewer.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a commit key for Git repositories.
 * 
 * @author mark
 */
public final class GitCommitKey extends AbstractCommitKey<GitCommitKey> {
	
	private final String commitHash;
	private final int commitTime;
	
	/**
	 * Creates a new key from the supplied {@code commitHash} and {@code commitDate}.
	 * 
	 * @param commitHash the git commit SHA1 hash. Must not be {@code null}
	 * @param commitDate the UTC time of this commit with seconds removed and cast to an integer
	 */
	public GitCommitKey(final String commitHash, final int commitDate) {
		Validate.notNull(commitHash, "commitHash must not be null");
		
		this.commitHash = commitHash;
		this.commitTime = commitDate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(final GitCommitKey key) {
		return commitTime - key.commitTime;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String marshal() {
		String[] commitKeyParts = new String[2];
		commitKeyParts[0] = commitHash;
		commitKeyParts[1] = Integer.toString(commitTime);
		
		return StringUtils.join(commitKeyParts, "-");
	}
	
	/**
	 * Unmarshals this key from a string.
	 * 
	 * @param s the string to unmarshal from. Must not be {@code null}
	 * @return the key. Never {@code null}
	 */
	public static GitCommitKey unmarshal(final String s) {
		Validate.notNull(s, "s must not be null");
		
		final String[] commitKeyParts = StringUtils.split(s, "-");
		return new GitCommitKey(commitKeyParts[0], Integer.parseInt(commitKeyParts[1]));
	}
	
	/**
	 * Gets the commit time. This is a java.util.Date time where the milliseconds
	 * have been discarded and the result casted to an integer.
	 * 
	 * @return the commit time
	 */
	public int getCommitTime() {
		return commitTime;
	}
	
	/**
	 * Gets the commit hash of this commit.
	 * 
	 * @return the commit SHA1 hash
	 */
	public String getCommitHash() {
		return commitHash;
	}
}
