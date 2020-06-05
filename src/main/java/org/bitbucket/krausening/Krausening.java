package org.bitbucket.krausening;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * In brewing, krausening (KROI-zen-ing) refers to adding a small amount of fresh wort to prime finished beer for carbonation. 
 * In Java, Krausening is a project to populate finished archives for deployment. This approach allows Properties files to be 
 * externalized from deployment units, enabling the same deployment unit to be leveraged repeatedly without the need to rebuild 
 * or hacking the archive.
 *
 * To use, you need to minimally set a system property "KRAUSENING_BASE", which points to a directory where your {@link Properties}
 * files will be located.  Krausening will load those up and make them available via the {@code getProperties(<file name>)} method.
 *
 * You can then use a system property called "KRAUSENING_EXTENSIONS" to set up extensions to the {@link Properties} files located 
 * in "KRAUSENING_BASE".  The first the base properties will be loaded, with anything in the extensions location being added on 
 * to the top.  This allows value to be added or overridden, which is especially useful when you have a standard configuration
 * defined in your base files, but need to specialize some values for different deployments.
 *
 * Only .properties files will be loaded.  Any other file encountered will be skipped.
 */
public final class Krausening {

	private static final Logger LOGGER = LoggerFactory.getLogger(Krausening.class);

	/** Location of the base set of {@link Properties} files. */
	public static final String BASE_LOCATION = "KRAUSENING_BASE";

	/** Location of a set of extension {@link Properties} files. */
	public static final String EXTENSIONS_LOCATION = "KRAUSENING_EXTENSIONS";

	/** Value of a set a master encryption password. */
	public static final String KRAUSENING_PASSWORD = "KRAUSENING_PASSWORD";

	/** Contains the set of all properties managed by the base + extension locations. */
	private Map<String, Properties> managedProperties;

	/** The location which contains base properties. */
	private String baseLocation;

	/** The location which contains extension properties. */
	private String extensionsLocation;

	/** Whether or not KRAUSENING_PASSWORD is non-blank. */
	private boolean hasMasterPassword;

	/** Singleton instance of this class. */
	private static Krausening instance = new Krausening();

	/**
	 * Private constructor to prevent external construction of this singleton instance, automatically
	 * triggers a loading of properties.
	 */
	private Krausening() {
		loadProperties();
	}

	/**
	 * Returns the singleton instance of this class.
	 * @return singleton reference to Krausening
	 */
	public static Krausening getInstance() {
		return instance;
	}

	/**
	 * Loads the properties defined by the base and extension locations, making them 
	 * accessible for use by Krausening clients.  This method can also be leveraged to
	 * refresh values at any time.
	 */
	public void loadProperties() {
		Map<String, Properties> bases = new HashMap<>();
		Map<String, Properties> extensions = new HashMap<>();

		long start = System.currentTimeMillis();
		LOGGER.debug("Loading Krausening properties...");

		managedProperties = new ConcurrentHashMap<>();

		boolean hasLocations = setLocations();

		setEncryptionFoundation();

		if (hasLocations) {
			File baseLocationAsFile = new File(baseLocation);
			if (baseLocationAsFile.exists()) {
				bases.putAll(loadPropertiesFromLocation(baseLocationAsFile));

			} else {
				logFileDoesNotExist(baseLocationAsFile, BASE_LOCATION);

			}

			if (StringUtils.isNotBlank(extensionsLocation)) {
				File extensionsLocationAsFile = new File(extensionsLocation);
				if (extensionsLocationAsFile.exists()) {
					extensions.putAll(loadPropertiesFromLocation(extensionsLocationAsFile));

				} else {
					logFileDoesNotExist(extensionsLocationAsFile, EXTENSIONS_LOCATION);

				}
			}
		}

		final Properties properties = managedProperties.get("application-configuration.properties");
		if (properties != null) {
			final String filePath = properties.getProperty("configuration.file");
			final File configFile = new File(filePath);
			if (configFile.exists()) {
				final Config fileConfig = ConfigFactory.parseFile(configFile).getConfig("application");
				final Config config = ConfigFactory.load(fileConfig);

				final List<PropertyConfig> baseConfigs = config.getConfigList("base")
						.stream()
						.map(PropertyConfig::new)
						.collect(Collectors.toList());

				final List<PropertyConfig> extensionConfigs = config.getConfigList("extension")
						.stream()
						.map(PropertyConfig::new)
						.collect(Collectors.toList());

				verifyProperties(baseConfigs, bases);
				verifyProperties(extensionConfigs, extensions);
			} else {
				throw new KrauseningException("No application configuration file found at " + filePath);
			}
		}

		long stop = System.currentTimeMillis();
		LOGGER.debug("Loaded Krausening properties in {}ms", (stop - start));

	}

	protected void setEncryptionFoundation() {
		String masterPassword = System.getProperty(KRAUSENING_PASSWORD);
		if (StringUtils.isBlank(masterPassword)) {
			LOGGER.warn("No {} set, Krausening will not support encrypted property values!", KRAUSENING_PASSWORD);

		} else {
			LOGGER.info("{} configured, Krausening will support encrypted property values.", KRAUSENING_PASSWORD);
			hasMasterPassword = true;
		}
	}

	/**
	 * Log an error for the file and location type when the location does not exist.
	 * @param file The file that does not exist
	 * @param location The location type (i.e., base or extension)
	 */
	protected void logFileDoesNotExist(File file, String location) {
		LOGGER.warn("{} refers to a location that does not exist: {}", location, file.getAbsolutePath());

	}

	/**
	 * Load system properties from into member variables.
	 */
	private boolean setLocations() {
		boolean hasLocations = false;

		baseLocation = System.getProperty(BASE_LOCATION);
		if (StringUtils.isBlank(baseLocation)) {
			LOGGER.warn("Without a {} set, Krausening cannot load any properties!", BASE_LOCATION);

		} else {
			LOGGER.info("Krausening base location: {}", baseLocation);
			hasLocations = true;

		}

		extensionsLocation = System.getProperty(EXTENSIONS_LOCATION);
		if (StringUtils.isBlank(extensionsLocation)) {
			LOGGER.warn("No {} set...", EXTENSIONS_LOCATION);

		} else {
			LOGGER.info("Krausening extensions location: {}", extensionsLocation);

		}

		return hasLocations;

	}

	/**
	 * Loads all .properties files from the passed location.
	 * @param location the location containing properties files
	 * @returns a map of file names to properties found at the location
	 */
	private Map<String, Properties> loadPropertiesFromLocation(File location) {
		Map<String, Properties> filePropertyMap = new HashMap<>();
		File[] files = location.listFiles((FilenameFilter)new SuffixFileFilter(".properties"));

		if ((files == null) || (files.length == 0)) {
			LOGGER.warn("No files were found within: {}", location.getAbsolutePath());

		} else {
			String fileName;
			Properties fileProperties;

			for (File file : files) {
				fileName = file.getName();
				Properties fileProps = createEmptyProperties();
				try (Reader fileReader = new FileReader(file)) {
					//file props is just for this file, fileProperties is the combined
					fileProps.load(fileReader);
					filePropertyMap.put(fileName, fileProps);

					fileProperties = managedProperties.getOrDefault(fileName, fileProps);
					fileProperties.putAll(fileProps);
					managedProperties.put(fileName, fileProperties);
				} catch (IOException e) {
					LOGGER.error("Could not read the file: " + file.getAbsolutePath(), e);
				}
			}
		}

		return filePropertyMap;
	}

	/**
	 * Creates an empty Properties file, either standard or encrypted, based on whether or not the master password
	 * is set.
	 * @return An empty properties instance
	 */
	private Properties createEmptyProperties() {
		Properties properties;
		if (hasMasterPassword) {
			//TODO: could externalize this so the type is configurable:
			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			encryptor.setPassword(System.getProperty(KRAUSENING_PASSWORD));
			properties = new EncryptableProperties(encryptor);

		} else {
			properties = new Properties();
		}

		return properties;
	}

	/**
	 * Returns the properties file loaded by Krausening for given file name.
	 * @param propertiesFileName The file name to retrieve
	 * @return The file or null if that file name is not know
	 */
	public Properties getProperties(String propertiesFileName) {
		return managedProperties.get(propertiesFileName);

	}

	private void verifyProperties(final List<PropertyConfig> configs, final Map<String, Properties> propertyMap) {
		verifyPropertiesExist(configs, propertyMap);

	}

	private void verifyPropertiesExist(final List<PropertyConfig> configs, final Map<String, Properties> propertyMap) {
		configs.forEach(prop -> {
			final String fileName = prop.getName();
			final List<String> baseProps = prop.getProperties();
			final Properties krauseningProps = propertyMap.get(fileName);

			if (krauseningProps == null) {
				throw new KrauseningException("No property file found for " + fileName);
			} else {
				baseProps.forEach(bp -> {
					String value = krauseningProps.getProperty(bp);
					if (StringUtils.isEmpty(value)) {
						throw new KrauseningException("No property found for " + value + " in file " + fileName);
					}
				});
			}
		});
	}

	private void verifyNoExtraProperties(final List<PropertyConfig> configs, final Map<String, Properties> propertyMap) {
		propertyMap.forEach((fileName,props) -> {
			if (!"application-configuration.properties".equalsIgnoreCase(fileName)) {
				final Optional<PropertyConfig> extraPropertyConfigOptional = configs.stream()
						.filter(cfg -> fileName.equalsIgnoreCase(cfg.getName()))
						.findFirst();

				if (!extraPropertyConfigOptional.isPresent()) {
					throw new KrauseningException("File found that should not exist: " + fileName);
				} else {
					final PropertyConfig config = extraPropertyConfigOptional.get();
					final Enumeration<?> keys = props.propertyNames();

					while (keys.hasMoreElements()) {
						final String key = keys.nextElement().toString();
						if (!config.getProperties().contains(key)) {
							throw new KrauseningException("Property found that should not exist: " + key + " for file: " + fileName);
						}
					}
				}
			}
		});
	}

}
