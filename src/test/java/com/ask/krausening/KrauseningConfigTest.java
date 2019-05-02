package com.ask.krausening;

import org.aeonbits.owner.Config.HotReload;
import org.aeonbits.owner.Config.HotReloadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.KrauseningConfig;
import org.aeonbits.owner.KrauseningConfig.KrauseningSources;
import org.aeonbits.owner.KrauseningConfigFactory;
import org.apache.commons.io.IOUtils;
import org.bitbucket.krausening.Krausening;
import org.bitbucket.krausening.KrauseningException;
import org.bitbucket.krausening.RequiredOverride;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KrauseningConfigTest extends AbstractKrauseningTest {

    protected static final String NON_EXISTENT_PROPERTY_DEFAULT_VALUE = "12345";
    protected static final String NON_EXISTENT_PROPERTY_DEFAULT_KEY = "not.defined.property.key";
    protected static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";
    protected static final String PI_PROPERTY_KEY = "pi";

    protected static final double PI_PROPERTY_VALUE = 3.14159;
    protected static final String BASE_PROPERTIES_LOCATION_IN_OUTPUT_DIR = "./target/test-classes/base";
    protected static final String EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR = "./target/test-classes/extensions";
    protected static final String A_FOO_PROPERTY_VALUE = "blah";
    protected static final String A_FOO_PROPERTY_KEY = "foo";
    protected static final String BASE_OVERRIDE_LOCATION = "./src/test/resources/base-override";

    @BeforeEach
    public void beforeTestExecution() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_PROPERTIES_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
        Krausening krausening = Krausening.getInstance();
        krausening.loadProperties();
    }

    @Test
    public void testDelegateToKrauseningToLoadSinglePropertyFile() throws Exception {
        SinglePropertyFileConfig singlePropertyFileConfig = KrauseningConfigFactory
                .create(SinglePropertyFileConfig.class);
        assertNotNull(singlePropertyFileConfig);
        assertEquals(FOO_PROPERTY_VALUE, singlePropertyFileConfig.getFoo());
        assertEquals(OVERRIDDEN_PROPERTY_VALUE, singlePropertyFileConfig.getOverriddenProperty());
        assertEquals(Integer.parseInt(NON_EXISTENT_PROPERTY_DEFAULT_VALUE),
                singlePropertyFileConfig.getIntegerProperty());
    }

    @Test
    public void testDelegateToKrauseningToLoadMultiplePropertyFiles() throws Exception {
        MultiplePropertyFileConfig multiplePropertyFileConfig = KrauseningConfigFactory
                .create(MultiplePropertyFileConfig.class);
        assertNotNull(multiplePropertyFileConfig);
        assertEquals(PI_PROPERTY_VALUE, multiplePropertyFileConfig.getPi());
        assertEquals(FOO_PROPERTY_VALUE, multiplePropertyFileConfig.getFoo());
        assertEquals(OVERRIDDEN_PROPERTY_VALUE, multiplePropertyFileConfig.getOverriddenProperty());
        assertEquals("The value of PI is 3.14159", multiplePropertyFileConfig.getPropertyWithVariableExpansion());
    }

    @Test
    public void testLoadPropertyFilesWithDuplicatePropertyKeys() throws Exception {
        assertThrows(RuntimeException.class, () -> {
            KrauseningConfigFactory.create(DuplicatePropertyKeysConfig.class);
        });
    }

    @Test
    public void testCombineOwnerAndKrauseningPropertyLoading() throws Exception {
        KrauseningAndOwnerLoadedPropertiesConfig ownerAndKrauseningLoadedConfig = KrauseningConfigFactory
                .create(KrauseningAndOwnerLoadedPropertiesConfig.class);
        assertNotNull(ownerAndKrauseningLoadedConfig);
        assertEquals(PI_PROPERTY_VALUE, ownerAndKrauseningLoadedConfig.getPi());
        assertEquals("consoleAppender", ownerAndKrauseningLoadedConfig.getLog4JAppenderName());
    }

    @Test
    public void testRenamingProperties() throws Exception {
        SinglePropertyFileConfig singlePropertyFileConfig = KrauseningConfigFactory
                .create(SinglePropertyFileConfig.class, "a-example.properties");
        assertNotNull(singlePropertyFileConfig);
        assertEquals(A_FOO_PROPERTY_VALUE, singlePropertyFileConfig.getFoo());
    }

    @Test
    public void testGettingProperties() throws Exception {
        SinglePropertyFileConfig singlePropertyFileConfig = KrauseningConfigFactory
                .create(SinglePropertyFileConfig.class, "a-example.properties");
        assertNotNull(singlePropertyFileConfig);
        Properties properties = new Properties();
        singlePropertyFileConfig.fill(properties);
        assertNotNull(properties);
        testPropertyKeyValue(properties, A_FOO_PROPERTY_KEY, A_FOO_PROPERTY_VALUE);
        testPropertyKeyValue(properties, NON_EXISTENT_PROPERTY_DEFAULT_KEY, NON_EXISTENT_PROPERTY_DEFAULT_VALUE);
    }

    private void testPropertyKeyValue(Properties properties, String expectedKey, Object expectedValue) {
        assertTrue(properties.keySet().contains(expectedKey),
                "Dynamic property set unexpectedly did not contain property " + expectedKey);
        assertEquals(expectedValue, properties.getProperty(expectedKey),
                "Dynamic property set unexpectedly did not match value for " + expectedKey);
    }

    @Test
    public void testSynchronousHotReloadOfKrauseningProperties() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_PROPERTIES_LOCATION_IN_OUTPUT_DIR);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR);
        Krausening krausening = Krausening.getInstance();
        krausening.loadProperties();

        HotReloadablePropertiesConfig hotReloadablePropertiesConfig = KrauseningConfigFactory
                .create(HotReloadablePropertiesConfig.class);
        assertNotNull(hotReloadablePropertiesConfig);
        assertEquals(PI_PROPERTY_VALUE, hotReloadablePropertiesConfig.getPi());

        String configPropertiesFilePath = EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR + "/"
                + CONFIG_PROPERTIES_FILE_NAME;

        Properties updatedConfigProperties = new Properties();
        Reader configPropertiesFileReader = new FileReader(configPropertiesFilePath);
        try {
            updatedConfigProperties.load(configPropertiesFileReader);
        } finally {
            IOUtils.closeQuietly(configPropertiesFileReader);
        }

        Random random = new Random();
        double updatedPiValue = random.nextDouble();
        updatedConfigProperties.setProperty(PI_PROPERTY_KEY, String.valueOf(updatedPiValue));
        Writer updatedConfigPropertiesFileWriter = new FileWriter(configPropertiesFilePath);
        try {
            updatedConfigProperties.store(updatedConfigPropertiesFileWriter, null);
        } finally {
            IOUtils.closeQuietly(updatedConfigPropertiesFileWriter);
        }

        long maxWaitTime = 10000L;
        long interval = 1000L;
        long elapsedWaitTime = 0L;
        while (elapsedWaitTime <= maxWaitTime) {
            Thread.sleep(interval);
            elapsedWaitTime += interval;
            if (updatedPiValue == hotReloadablePropertiesConfig.getPi()) {
                break;
            }
        }
        assertEquals(updatedPiValue, hotReloadablePropertiesConfig.getPi());
    }

    @Test
    public void testUnderlyingPropertiesFileDoesNotExist() throws Exception {
        DoesNotExistPropertiesConfig doesNotExistConfig = KrauseningConfigFactory
                .create(DoesNotExistPropertiesConfig.class);
        assertNotNull(doesNotExistConfig);
        assertEquals(Double.valueOf(3.14), Double.valueOf(doesNotExistConfig.getPi()),
                "Should have defaulted to the Owner default value!");
    }

    @Test
    public void testRequiredClassWorking() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_PROPERTIES_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
        Krausening krausening = Krausening.getInstance();
        krausening.loadProperties();
        NeedsOverridePropertiesConfig overriddenConfig = KrauseningConfigFactory
                .create(NeedsOverridePropertiesConfig.class);
        assertNotNull(overriddenConfig);
        assertEquals("overridden",
                overriddenConfig.getRequiredOverrideField(), "Overridden value is not correct");
    }

    @Test
    public void testRequiredClassOverrideMissingLocation() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_OVERRIDE_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, "./not-real");
        Krausening krausening = Krausening.getInstance();

        assertThrows(KrauseningException.class, krausening::loadProperties);

    }

    @Test
    public void testRequiredClassOverrideMissingFile() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_OVERRIDE_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, "./src/test/resources/extensions-missing-file");
        Krausening krausening = Krausening.getInstance();
        assertThrows(KrauseningException.class, krausening::loadProperties);
    }

    @Test
    public void testRequiredClassOverrideMissingProperty() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_OVERRIDE_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, "./src/test/resources/extensions-missing-property");
        Krausening krausening = Krausening.getInstance();
        assertThrows(KrauseningException.class, krausening::loadProperties);
    }

    @Test
    public void testRequiredClassNotRequiredByEnvironment() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_OVERRIDE_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, "./src/test/resources/extensions-no-required-overrides-by-environment");
        Krausening krausening = Krausening.getInstance();
        assertDoesNotThrow(krausening::loadProperties, "Exception thrown when class override is not required.");
    }

    @Test
    public void testRequiredMethodRequiredByEnvironment() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_OVERRIDE_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
        Krausening krausening = Krausening.getInstance();
        krausening.loadProperties();
        NeedsOverrideByEnvironmentPropertiesConfig overriddenConfig = KrauseningConfigFactory
                .create(NeedsOverrideByEnvironmentPropertiesConfig.class);
        assertNotNull(overriddenConfig);
        assertEquals("environment-required",
                overriddenConfig.getRequiredOverrideField(), "Overridden value is not correct per environment");
    }

    @Test
    public void testRequiredClassRequiredByEnvironment() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_OVERRIDE_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
        Krausening krausening = Krausening.getInstance();
        krausening.loadProperties();
        NeedsOverrideByEnvironmentForAllPropertiesConfig overriddenConfig = KrauseningConfigFactory
                .create(NeedsOverrideByEnvironmentForAllPropertiesConfig.class);
        assertNotNull(overriddenConfig);
        assertEquals("environment-required",
                overriddenConfig.getRequiredOverrideField(), "Overridden value is not correct per environment");
        assertEquals(3.1415926,
                overriddenConfig.getPi(), "Overridden value is not correct per environment");
    }

    @Test
    public void testMultipleErrors() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_OVERRIDE_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, "./not-real");
        Krausening krausening = Krausening.getInstance();
        try {
            krausening.loadProperties();
        } catch (KrauseningException e) {
            String errorMsg = e.getMessage();
            assertTrue(errorMsg.contains("example-override-environment-all.properties"));
            assertTrue(errorMsg.contains("example-override.properties"));
        }

    }

    @KrauseningSources(EXAMPLE_PROPERTIES_FILE_NAME)
    protected interface SinglePropertyFileConfig extends KrauseningConfig {

        @Key(FOO_PROPERTY_KEY)
        String getFoo();

        @Key(OVERRIDDEN_PROPERTY_KEY)
        String getOverriddenProperty();

        @Key(NON_EXISTENT_PROPERTY_DEFAULT_KEY)
        @DefaultValue(NON_EXISTENT_PROPERTY_DEFAULT_VALUE)
        int getIntegerProperty();
    }

    @KrauseningSources({ CONFIG_PROPERTIES_FILE_NAME, EXAMPLE_PROPERTIES_FILE_NAME })
    protected interface MultiplePropertyFileConfig extends KrauseningConfig {
        @Key(PI_PROPERTY_KEY)
        double getPi();

        @Key("string.property.expansion")
        String getPropertyWithVariableExpansion();

        @Key(FOO_PROPERTY_KEY)
        String getFoo();

        @Key(OVERRIDDEN_PROPERTY_KEY)
        String getOverriddenProperty();
    }

    @KrauseningSources({ EXAMPLE_PROPERTIES_FILE_NAME, "example-properties-with-same-keys.properties" })
    protected interface DuplicatePropertyKeysConfig extends KrauseningConfig {
        @Key(FOO_PROPERTY_KEY)
        String getFoo();

        @Key(OVERRIDDEN_PROPERTY_KEY)
        String getOverriddenProperty();
    }

    @Sources("file:./src/test/resources/not-just-properties-files/some-other-configuration-file.xml")
    @KrauseningSources(CONFIG_PROPERTIES_FILE_NAME)
    protected interface KrauseningAndOwnerLoadedPropertiesConfig extends KrauseningConfig {
        @Key(PI_PROPERTY_KEY)
        double getPi();

        @Key("log4j:configuration.appender.name")
        String getLog4JAppenderName();
    }

    @HotReload(value = 1, unit = TimeUnit.SECONDS, type = HotReloadType.SYNC)
    @KrauseningSources(CONFIG_PROPERTIES_FILE_NAME)
    protected interface HotReloadablePropertiesConfig extends KrauseningConfig {
        @Key(PI_PROPERTY_KEY)
        double getPi();
    }

    @KrauseningSources("does-not-exist.properties")
    protected interface DoesNotExistPropertiesConfig extends KrauseningConfig {
        @Key(PI_PROPERTY_KEY)
        @DefaultValue("3.14")
        double getPi();
    }

    @KrauseningSources("example-override.properties")
    protected interface NeedsOverridePropertiesConfig extends KrauseningConfig {
        @Key(PI_PROPERTY_KEY)
        @DefaultValue("3.14")
        double getPi();

        @Key("required.override")
        @DefaultValue("default")
        @RequiredOverride
        String getRequiredOverrideField();
    }

    @KrauseningSources("example-override-environment.properties")
    protected interface NeedsOverrideByEnvironmentPropertiesConfig extends KrauseningConfig {
        @Key(PI_PROPERTY_KEY)
        @DefaultValue("3.14")
        double getPi();

        @Key("required.override.by.environment")
        @DefaultValue("default")
        @RequiredOverride(environments = "extensions")
        String getRequiredOverrideField();
    }

    @RequiredOverride(environments = {"extensions", "not-real"})
    @KrauseningSources("example-override-environment-all.properties")
    protected interface NeedsOverrideByEnvironmentForAllPropertiesConfig extends KrauseningConfig {
        @Key(PI_PROPERTY_KEY)
        @DefaultValue("3.14")
        double getPi();

        @Key("required.override.by.environment")
        @DefaultValue("default")
        String getRequiredOverrideField();
    }
}
