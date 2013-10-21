package jiracommitviewer.domain;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * A type of {@link AbstractRepository} that represents a Git repository.
 * 
 * @author mark
 */
public final class GitRepository extends AbstractRepository {
	
	private static final String PROPERTY_URI = "uri";
	private static final String PROPERTY_PRIVATE_KEY_PATH = "privateKeyPath";
	
	private String uri;
	private File privateKeyPath;
	
	public GitRepository(final Object id) {
		super(id);
	}

	/**
	 * Gets the repository URI.
	 * 
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	
	/**
	 * Sets the repository URI.
	 * 
	 * @param uri the uri to set
	 */
	public void setUri(final String uri) {
		this.uri = uri;
	}
	
	/**
	 * Gets the private key file to use for authentication.
	 * 
	 * @return the private key file. May be {@code null} if none set
	 */
	public File getPrivateKeyPath() {
		return privateKeyPath;
	}
	
	/**
	 * Sets the private key file to use for authentication.
	 * 
	 * @param privateKeyPath a valid path to a key file or {@code null} to set no path
	 */
	public void setPrivateKeyPath(final String privateKeyPath) {
		if (StringUtils.isBlank(privateKeyPath)) {
			this.privateKeyPath = null;
			return;
		}
		this.privateKeyPath = new File(privateKeyPath);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFromProperties(final Properties properties) {
		Validate.notNull(properties, "properties must not be null");
		
		super.loadFromProperties(properties);
		this.uri = properties.getProperty(PROPERTY_URI);
		this.privateKeyPath = "".equals(properties.getProperty(PROPERTY_PRIVATE_KEY_PATH)) ? null :
			new File(properties.getProperty(PROPERTY_PRIVATE_KEY_PATH));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveToProperties(final Properties properties) {
		super.saveToProperties(properties);
		properties.put(PROPERTY_URI, getUri());
		properties.put(PROPERTY_PRIVATE_KEY_PATH, privateKeyPath != null ? privateKeyPath.getPath() : "");
	}
}
