package com.ask.krausening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.aeonbits.owner.Config.HotReload;
import org.aeonbits.owner.Config.HotReloadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.KrauseningConfig;
import org.aeonbits.owner.KrauseningConfig.KrauseningSources;
import org.aeonbits.owner.KrauseningConfigFactory;
import org.apache.commons.io.IOUtils;
import org.bitbucket.krausening.Krausening;
import org.junit.Before;
import org.junit.Test;

public class KrauseningConfigTest extends AbstractKrauseningTest {

	protected static final String NON_EXISTENT_PROPERTY_DEFAULT_VALUE = "12345";
	protected static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";
	protected static final String PI_PROPERTY_KEY = "pi";
	protected static final double PI_PROPERTY_VALUE = 3.14159;
	protected static final String BASE_PROPERTIES_LOCATION_IN_OUTPUT_DIR = "./target/test-classes/base";
	protected static final String EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR = "./target/test-classes/extensions";

	@Before
	public void reloadKrausening() throws Exception {
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
		assertEquals(PI_PROPERTY_VALUE, multiplePropertyFileConfig.getPi(), 0);
		assertEquals(FOO_PROPERTY_VALUE, multiplePropertyFileConfig.getFoo());
		assertEquals(OVERRIDDEN_PROPERTY_VALUE, multiplePropertyFileConfig.getOverriddenProperty());
		assertEquals("The value of PI is 3.14159", multiplePropertyFileConfig.getPropertyWithVariableExpansion());
	}

	@Test(expected = RuntimeException.class)
	public void testLoadPropertyFilesWithDuplicatePropertyKeys() throws Exception {
		KrauseningConfigFactory.create(DuplicatePropertyKeysConfig.class);
	}

	@Test
	public void testCombineOwnerAndKrauseningPropertyLoading() throws Exception {
		KrauseningAndOwnerLoadedPropertiesConfig ownerAndKrauseningLoadedConfig = KrauseningConfigFactory
				.create(KrauseningAndOwnerLoadedPropertiesConfig.class);
		assertNotNull(ownerAndKrauseningLoadedConfig);
		assertEquals(PI_PROPERTY_VALUE, ownerAndKrauseningLoadedConfig.getPi(), 0);
		assertEquals("consoleAppender", ownerAndKrauseningLoadedConfig.getLog4JAppenderName());
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
		assertEquals(PI_PROPERTY_VALUE, hotReloadablePropertiesConfig.getPi(), 0);

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

		Thread.sleep(1500);
		assertEquals(updatedPiValue, hotReloadablePropertiesConfig.getPi(), 0);
	}

	@KrauseningSources(EXAMPLE_PROPERTIES_FILE_NAME)
	protected interface SinglePropertyFileConfig extends KrauseningConfig {

		@Key(FOO_PROPERTY_KEY)
		String getFoo();

		@Key(OVERRIDDEN_PROPERTY_KEY)
		String getOverriddenProperty();

		@Key("not.defined.property.key")
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
}
