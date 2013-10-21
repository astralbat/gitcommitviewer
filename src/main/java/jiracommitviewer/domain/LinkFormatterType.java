package jiracommitviewer.domain;

/**
 * Built-in templates for link formatters used for when commits are displayed within JIRA.
 * 
 * @author mark
 */
public enum LinkFormatterType {

	GITWEB,
	CGIT;
	
	/**
	 * Gets the appropriate formatter type from the display name in {@code weblinktypes.properties}.
	 * 
	 * @return the formatter type or {@code null} if {@code name} is not recognised as a valid type
	 */
	public static LinkFormatterType findByDisplayName(final String name) {
		if ("Git Web".equals(name)) {
			return GITWEB;
		} else if ("Cgit".equals(name)) {
			return CGIT;
		}
		return null;
	}
	
	/**
	 * Gets the display name of this format type.
	 * 
	 * @return the display name. Never {@code null}
	 */
	public String getDisplayName() {
		switch (this) {
		case GITWEB:
			return "Git Web";
		case CGIT:
			return "Cgit";
		default:
			throw new IllegalStateException("Unknown format type");
		}
	}
}
