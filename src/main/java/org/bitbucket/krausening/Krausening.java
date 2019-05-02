package org.bitbucket.krausening;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.KrauseningConfig;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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

    private static final Logger logger = LoggerFactory.getLogger(Krausening.class);

    /** Location of the base set of {@link Properties} files. */
    public static final String BASE_LOCATION = "KRAUSENING_BASE";

    /** Location of a set of extension {@link Properties} files. */
    public static final String EXTENSIONS_LOCATION = "KRAUSENING_EXTENSIONS";

    /** Value of a set a master encryption password. */
    public static final String KRAUSENING_PASSWORD = "KRAUSENING_PASSWORD";

    /** Contains the set of all properties managed by the base + extension locations. */
    private Map<String, Properties> managedProperties;

    /** Contains the set of all extension properties */
    private List<String> extensions;

    /** Contains the set of all base properties */
    private List<String> bases;

    /** Contains the list of property files that have to be overridden */
    private Map<String, List<String>> requiredOverrides;

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
        long start = System.currentTimeMillis();
        logger.debug("Loading Krausening properties...");

        managedProperties = new ConcurrentHashMap<>();

        boolean hasLocations = setLocations();

        setEncryptionFoundation();

        if (hasLocations) {
            File baseLocationAsFile = new File(baseLocation);
            if (baseLocationAsFile.exists()) {
                bases = loadPropertiesFromLocation(baseLocationAsFile);

            } else {
                logFileDoesNotExist(baseLocationAsFile, BASE_LOCATION);

            }

            if (StringUtils.isNotBlank(extensionsLocation)) {
                File extensionsLocationAsFile = new File(extensionsLocation);
                if (extensionsLocationAsFile.exists()) {
                    extensions = loadPropertiesFromLocation(extensionsLocationAsFile);

                } else {
                    extensions = new ArrayList<>();
                    logFileDoesNotExist(extensionsLocationAsFile, EXTENSIONS_LOCATION);
                }
            }
            checkRequiredProperties();
        }

        long stop = System.currentTimeMillis();
        logger.debug("Loaded Krausening properties in {}ms", (stop - start));

    }

    protected void setEncryptionFoundation() {
        String masterPassword = System.getProperty(KRAUSENING_PASSWORD);
        if (StringUtils.isBlank(masterPassword)) {
            logger.warn("No {} set, Krausening will not support encrypted property values!", KRAUSENING_PASSWORD);

        } else {
            logger.info("{} configured, Krausening will support encrypted property values.", KRAUSENING_PASSWORD);
            hasMasterPassword = true;
        }
    }

    /**
     * Log an error for the file and location type when the location does not exist.
     * @param file The file that does not exist
     * @param location The location type (i.e., base or extension)
     */
    protected void logFileDoesNotExist(File file, String location) {
        logger.warn("{} refers to a location that does not exist: {}", location, file.getAbsolutePath());

    }

    /**
     * Load system properties from into member variables.
     */
    private boolean setLocations() {
        boolean hasLocations = false;

        baseLocation = System.getProperty(BASE_LOCATION);
        if (StringUtils.isBlank(baseLocation)) {
            logger.warn("Without a {} set, Krausening cannot load any properties!", BASE_LOCATION);

        } else {
            logger.info("Krausening base location: {}", baseLocation);
            hasLocations = true;

        }

        extensionsLocation = System.getProperty(EXTENSIONS_LOCATION);
        if (StringUtils.isBlank(extensionsLocation)) {
            logger.warn("No {} set...", EXTENSIONS_LOCATION);

        } else {
            logger.info("Krausening extensions location: {}", extensionsLocation);

        }

        return hasLocations;

    }

    /**
     * Loads all .properties files from the passed location.
     * @param location the location containing properties files
     * @returns the list of fileNames that were loaded
     */
    private List<String> loadPropertiesFromLocation(File location) {
        List<String> fileNames = new ArrayList<>();
        File[] files = location.listFiles((FilenameFilter)new SuffixFileFilter(".properties"));

        if ((files == null) || (files.length == 0)) {
            logger.warn("No files were found within: {}", location.getAbsolutePath());


        } else {
            String fileName = null;
            Properties fileProperties = null;
            for (File file : files) {
                fileName = file.getName();
                fileNames.add(fileName);
                fileProperties = (managedProperties.containsKey(fileName))
                        ? managedProperties.get(fileName) : createEmptyProperties();
                try (Reader fileReader = new FileReader(file)) {
                    fileProperties.load(fileReader);
                    managedProperties.put(fileName, fileProperties);

                } catch (IOException e) {
                    logger.error("Could not read the file: " + file.getAbsolutePath(), e);

                }
            }
        }

        return fileNames;
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

    /**
     * Returns a map of property file names to methods that require overrides
     * @return map of property file names to methods that require overrides
     */
    private Map<String, List<String>> getRequiredPropertyOverrides() {
        Map<String, List<String>> overrides = new HashMap<>();
        try {
            overrides = getRequiredOverrides();
        } catch (ReflectionsException ex) {
            logger.warn("Reflection exception when searching for subtypes.", ex);
        }
        return overrides;
    }

    /**
     * Validates all required properties are present. Logs and throws an error if any are missing.
     */
    private void checkRequiredProperties() {
        requiredOverrides = getRequiredPropertyOverrides();
        boolean missing = false;

        if (requiredOverrides.size() > 0) {
            StringBuilder errorMsg = new StringBuilder("Unable to override properties for: ");

            if (extensions == null) {
                extensions = new ArrayList<>();
            }

            for (Map.Entry<String, List<String>> required : requiredOverrides.entrySet()) {

                Optional<String> fileOptional = extensions.stream()
                        .filter(fileName -> fileName.equalsIgnoreCase(required.getKey()))
                        .findFirst();

                if (!fileOptional.isPresent()) {
                    missing = true;
                    errorMsg.append(required).append(", ");
                } else {
                    String fileName = fileOptional.get();
                    try {
                        Scanner scanner = new Scanner(new File(extensionsLocation + "/" + fileName));
                        boolean foundMethod = false;
                        for (String method : requiredOverrides.get(fileName)) {
                            while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                if (line.split("=")[0].equalsIgnoreCase(method)) {
                                    foundMethod = true;
                                }
                            }
                        }
                        missing = !foundMethod;
                    } catch (FileNotFoundException e) {
                        logger.error("Unable to find required file " + fileName, e);
                        errorMsg.append(fileName).append(", ");
                    }

                }
            }

            if (missing) {
                throw new KrauseningException(errorMsg.toString());
            }
        }
    }

    /**
     * Returns file names with their required properties
     * @return map of file names to required properties
     */
    private Map<String, List<String>> getRequiredOverrides() {
        Map<String, List<String>> overrides = new HashMap<>();
        ConfigurationBuilder builder = new ConfigurationBuilder();
        Properties properties = getProperties("required-overrides.properties");

        if (properties != null) {
            String packagesString = properties.getProperty("base.packages.to.scan", null);

            if (packagesString != null) {
                String[] packages = packagesString.split(",");

                for(String pkg: packages) {
                    builder.addUrls(ClasspathHelper.forPackage(pkg));
                }

                Reflections reflections = new Reflections(builder);
                Set<Class<? extends KrauseningConfig>> classes = reflections.getSubTypesOf(KrauseningConfig.class);

                addClassesRequiringOverrides(overrides, classes);
                addMethodsRequiringOverrides(overrides, classes);
            }
        }
        return overrides;
    }

    /**
     * Returns a map of file names to required properties at the class level
     * @param overrides map of file names to required methods
     * @param classes the classes to search
     * @return a map of file names to required properties at the class level
     */
    private Map<String, List<String>> addClassesRequiringOverrides(final Map<String, List<String>> overrides,
                                                                   final Set<Class<? extends KrauseningConfig>> classes) {

        for (Class<? extends KrauseningConfig> clazz : classes) {
            if (clazz.getAnnotation(RequiredOverride.class) != null) {
                String[] environments = clazz.getAnnotation(RequiredOverride.class).environments();
                if ("".equals(environments[0])) {
                    addClassOverrides(overrides).accept(overrides, clazz);
                } else {
                    for (String environment : environments) {

                        File f = new File(extensionsLocation);
                        String extensionsDirectory = f.getName();

                        if (environment.equalsIgnoreCase(extensionsDirectory)) {
                            addClassOverrides(overrides).accept(overrides, clazz);
                        }
                    }
                }
            }
        }

        return overrides;
    }

    /**
     * Returns a map of file names to required properties at the method level
     * @param overrides map of file names to required methods
     * @param classes the classes to search
     * @return a map of file names to required properties at the method level
     */
    private Map<String, List<String>> addMethodsRequiringOverrides(final Map<String, List<String>> overrides,
                                                                   final Set<Class<? extends KrauseningConfig>> classes) {

        for (Class<? extends KrauseningConfig> clazz : classes) {

            Method[] methods = clazz.getMethods();

            for (Method method: methods) {
                if (method.getAnnotation(RequiredOverride.class) != null) {
                    String[] environments = method.getAnnotation(RequiredOverride.class).environments();
                    if ("".equals(environments[0])) {
                        addMethodOverrides(overrides).accept(clazz, method);
                    } else {
                        for (String environment : environments) {

                            File f = new File(extensionsLocation);
                            String extensionsDirectory = f.getName();

                            if (environment.equalsIgnoreCase(extensionsDirectory)) {
                                addMethodOverrides(overrides).accept(clazz, method);
                            }
                        }
                    }
                }
            }
        }
        return overrides;
    }

    /**
     * Adds method overrides
     * @param overrides map of file names to required override methods
     * @return function to add method overrides
     */
    private BiConsumer<Class<? extends KrauseningConfig>, Method> addMethodOverrides(final Map<String, List<String>> overrides) {
        return (clazz, method) -> {
            final List<String> classProperties = Arrays.asList(clazz.getAnnotation(KrauseningConfig.KrauseningSources.class).value());
            classProperties.forEach(fileName -> {
                final List<String> methods;
                if (!overrides.containsKey(fileName)) {
                    methods = new ArrayList<>();
                } else {
                    methods = overrides.get(fileName);
                }
                final String methodProperties = method.getAnnotation(Config.Key.class).value();
                methods.add(methodProperties);
                overrides.put(fileName, methods);
            });
        };
    }

    /**
     * Adds method overrides by required class
     * @param overrides map of file names to required override methods
     * @return function to add class overrides
     */
    private BiConsumer<Map<String, List<String>>, Class<? extends KrauseningConfig>> addClassOverrides(final Map<String, List<String>> overrides) {
        return (map, clazz) -> {
            final List<String> classProperties = Arrays.asList(clazz.getAnnotation(KrauseningConfig.KrauseningSources.class).value());
            classProperties.forEach(fileName -> {
                final List<String> methodsList;
                if (!map.containsKey(fileName)) {
                    methodsList = new ArrayList<>();
                } else {
                    methodsList = overrides.get(fileName);
                }
                final Method[] methods = clazz.getMethods();
                for (final Method method: methods)  {
                    if (method.getAnnotation(Config.Key.class) != null) {
                        final String methodProperties = method.getAnnotation(Config.Key.class).value();
                        methodsList.add(methodProperties);
                    }
                }
                map.put(fileName, methodsList);
            });
        };
    }

}
