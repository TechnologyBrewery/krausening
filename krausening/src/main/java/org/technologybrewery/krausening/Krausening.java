package org.technologybrewery.krausening;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In brewing, krausening (KROI-zen-ing) refers to adding a small amount of
 * fresh wort to prime finished beer for carbonation. In Java, Krausening is a
 * project to populate finished archives for deployment. This approach allows
 * Properties files to be externalized from deployment units, enabling the same
 * deployment unit to be leveraged repeatedly without the need to rebuild or
 * hacking the archive.
 * 
 * To use, you need to minimally set a system property "KRAUSENING_BASE", which
 * points to a directory where your {@link Properties} files will be located.
 * Krausening will load those up and make them available via the
 * {@code getProperties(<file name>)} method.
 * 
 * You can then use a system property called "KRAUSENING_EXTENSIONS" to set up
 * extensions to the {@link Properties} files located in "KRAUSENING_BASE". The
 * first the base properties will be loaded, with anything in the extensions
 * location being added on to the top. This allows value to be added or
 * overridden, which is especially useful when you have a standard configuration
 * defined in your base files, but need to specialize some values for different
 * deployments.
 * 
 * Only .properties files will be loaded. Any other file encountered will be
 * skipped.
 */
public final class Krausening {

    private static final Logger LOGGER = LoggerFactory.getLogger(Krausening.class);

    /** Location of the base set of {@link Properties} files. */
    public static final String BASE_LOCATION = "KRAUSENING_BASE";

    /** Location of a set of extension {@link Properties} files. */
    public static final String EXTENSIONS_LOCATION = "KRAUSENING_EXTENSIONS";

    /** Location of a set of classloader/war-specific extensions. */
    public static final String OVERRIDE_EXTENSIONS_LOCATION = "KRAUSENING_OVERRIDE_EXTENSIONS";

    /**
     * Param for reading in the path for the subfolder within
     * classloader/war-specific extensions.
     */
    public static final String OVERRIDE_EXTENSIONS_SUBFOLDER_PARAM = "override.extensions.subfolder";

    /** Value of a set a master encryption password. */
    public static final String KRAUSENING_PASSWORD = "KRAUSENING_PASSWORD";

    /**
     * Contains the set of all properties managed by the base + extension
     * locations.
     */
    private Map<String, Properties> managedProperties;

    /** The location which contains base properties. */
    private String baseLocation;

    /** The location which contains extension properties. */
    private String extensionsLocation;

    /**
     * The location which contains properties that will override the extensions.
     */
    private String overrideExtensionSubfolder;

    /** Whether or not KRAUSENING_PASSWORD is non-blank. */
    private boolean hasMasterPassword;
    
    private static Map<String, Krausening> instanceMap = new HashMap<>();
    
    private static String defaultInstanceKey = UUID.randomUUID().toString();

    /**
     * Private constructor to prevent external construction of this singleton
     * instance, automatically triggers a loading of properties.
     */
    private Krausening() {}

    /**
     * Returns the singleton instance of this class.
     * 
     * @return singleton reference to Krausening
     */
    public static Krausening getInstance() {
    	Krausening instance = instanceMap.get(defaultInstanceKey);
		if(instance == null) {
			instance = new Krausening();
			instance.loadProperties();
			instanceMap.put(defaultInstanceKey, instance);
		}
		return instance;
    }
    
    protected void updateDefaultInstance() {
		instanceMap.put(defaultInstanceKey, this);
    }
    
    /**
     * Returns an instance of the class based on the overrideExtensionsSubfolder requested.
     * 
     * @param overrideExtensionsSubfolder
     * @return
     */
    public static Krausening getInstance(String overrideExtensionsSubfolder) {
    	Krausening overrideInstance = instanceMap.get(overrideExtensionsSubfolder);
		if(overrideInstance == null) {
			overrideInstance = new Krausening();
			overrideInstance.setOverrideExtensionsSubfolder(overrideExtensionsSubfolder);
			overrideInstance.loadProperties();
			instanceMap.put(overrideExtensionsSubfolder, overrideInstance);
		}
		return overrideInstance;
	}

    /**
     * Loads the properties defined by the base and extension locations, making
     * them accessible for use by Krausening clients. This method can also be
     * leveraged to refresh values at any time.
     */
    public void loadProperties() {
        long start = System.currentTimeMillis();
        LOGGER.debug("Loading Krausening properties...");

        managedProperties = new ConcurrentHashMap<>();

        boolean hasLocations = setLocations();

        setEncryptionFoundation();

        if (hasLocations) {
            loadPropertiesFromLocation(new File(baseLocation), BASE_LOCATION);

            if (StringUtils.isNotBlank(extensionsLocation)) {
                loadPropertiesFromLocation(new File(extensionsLocation), EXTENSIONS_LOCATION);
            }

            if (StringUtils.isNotBlank(overrideExtensionSubfolder)) {
                String baseOverrideLocation = System.getProperty(OVERRIDE_EXTENSIONS_LOCATION);
                if (StringUtils.isBlank(baseOverrideLocation)) {
                    LOGGER.error("Trying to use a override subfolder {} without the base folder set {}...",
                            overrideExtensionSubfolder, OVERRIDE_EXTENSIONS_LOCATION);
                } else {
                    // Get the path relative to the override extensions location
                    File overrideExtensionLocationPath = new File(baseOverrideLocation, overrideExtensionSubfolder);

                    loadPropertiesFromLocation(overrideExtensionLocationPath, OVERRIDE_EXTENSIONS_LOCATION);
                }
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
     * Log an error for the file and location type when the location does not
     * exist.
     * 
     * @param file
     *            The file that does not exist
     * @param location
     *            The location type (i.e., base or extension)
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
     * 
     * @param location
     *            the location containing properties files
     * @param locationType
     *            the type of properties being loaded (i.e. base, extensions,
     *            override)
     */
    private void loadPropertiesFromLocation(File location, String locationType) {
        if (!location.exists()) {
            logFileDoesNotExist(location, locationType);
        } else {
            File[] files = location.listFiles((FilenameFilter) new SuffixFileFilter(".properties"));

            if ((files == null) || (files.length == 0)) {
                LOGGER.warn("No files were found within: {}", location.getAbsolutePath());

            } else {
                String fileName = null;
                Properties fileProperties = null;
                for (File file : files) {
                    fileName = file.getName();
                    fileProperties = (managedProperties.containsKey(fileName)) ? managedProperties.get(fileName)
                            : createEmptyProperties();
                    try (Reader fileReader = new FileReader(file)) {
                        fileProperties.load(fileReader);
                        managedProperties.put(fileName, fileProperties);

                    } catch (IOException e) {
                        LOGGER.error("Could not read the file: " + file.getAbsolutePath(), e);

                    }
                }
            }
        }
    }

    /**
     * Creates an empty Properties file, either standard or encrypted, based on
     * whether or not the master password is set.
     * 
     * @return An empty properties instance
     */
    private Properties createEmptyProperties() {
        Properties properties;
        if (hasMasterPassword) {
            // TODO: could externalize this so the type is configurable:
            StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
            encryptor.setPassword(System.getProperty(KRAUSENING_PASSWORD));
            // Use PBEWithHMACSHA512AndAES_256 cipher algorithm for more secure encryption
            encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
            encryptor.setIvGenerator(new RandomIvGenerator());

            properties = new EncryptableProperties(encryptor);

        } else {
            properties = new Properties();
        }

        return properties;
    }

    /**
     * Returns the properties file loaded by Krausening for given file name.
     * 
     * @param propertiesFileName
     *            The file name to retrieve
     * @return The file or null if that file name is not know
     */
    public Properties getProperties(String propertiesFileName) {
        return managedProperties.get(propertiesFileName);

    }

    /**
     * Location of properties that are shared within a classloader/war, but not
     * for the entire system. It acts a second level extension effectively. A
     * property is first set in base, the extensions, then finally in a
     * subfolder within the override extension location.
     * 
     * @param overrideExtensionSubfolder
     */
    protected void setOverrideExtensionsSubfolder(String overrideExtensionSubfolder) {

        // The folder of the override extensions within the base override
        // extensions directory
        this.overrideExtensionSubfolder = overrideExtensionSubfolder;
    }
    
    
    
}
