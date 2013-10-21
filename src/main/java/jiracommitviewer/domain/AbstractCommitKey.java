package jiracommitviewer.domain;

public abstract class AbstractCommitKey<T> implements Comparable<T> {

	/**
	 * Marshals this key to a String.
	 * 
	 * @return the marshalled string. Never {@code null}
	 */
	public abstract String marshal();
}
