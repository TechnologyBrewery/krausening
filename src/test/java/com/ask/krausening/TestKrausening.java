package com.ask.krausening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import org.bitbucket.krausening.Krausening;
import org.junit.Test;

public class TestKrausening extends AbstractKrauseningTest {

	protected static final String VALUE_NEW_VALUE = "new.value";
	protected static final String KEY_NEWLY_ADDED_IN_EXTENSIONS = "newly.added.in.extensions";
	protected static final String NONEXISTENT_LOCATION = "./src/test/resources/does-not-exist";
	protected static final String EMPTY_LOCATION = "./src/test/resources/empty";
	protected static final String NOT_JUST_PROPERTIES_FILES_LOCATION = "./src/test/resources/not-just-properties-files";
	protected static final String NO_LOCATION = "";
	protected static final String EMPTY_PROPERTIES = "empty.properties";

	@Test
	public void testBaseLocationFileRead() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		assertNotNull(properties);

	}

	@Test
	public void testBaseLocationPropertyRead() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		Object value = properties.get(FOO_PROPERTY_KEY);
		assertEquals(FOO_PROPERTY_VALUE, value);

	}

	@Test
	public void testExtensionLocationPropertyReadOfNonOverridenValue() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		Object value = properties.get(FOO_PROPERTY_KEY);
		assertEquals(FOO_PROPERTY_VALUE, value);

	}

	@Test
	public void testExtensionLocationPropertyReadOfOverridenValue() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		Object value = properties.get("override.me");
		assertEquals("some-localized-value", value);

	}

	@Test
	public void testExtensionLocationPropertyReadOfNewValue() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		Object value = properties.get(KEY_NEWLY_ADDED_IN_EXTENSIONS);
		assertEquals(VALUE_NEW_VALUE, value);

	}

	@Test
	public void testFileReadWithoutAnyLocation() {
		Krausening krausening = getKrausening(NO_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		assertNull(properties);

	}

	@Test
	public void testFileReadWithNonexistantBaseLocation() {
		Krausening krausening = getKrausening(NONEXISTENT_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		assertNull(properties);

	}

	@Test
	public void testFileReadWithNonexistantExtensionsLocation() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, NONEXISTENT_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		Object value = properties.get(KEY_NEWLY_ADDED_IN_EXTENSIONS);
		assertNull(value);

	}

	@Test
	public void testFileReadOfEmptyProperties() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EMPTY_PROPERTIES);
		Object value = properties.get(KEY_NEWLY_ADDED_IN_EXTENSIONS);
		assertNull(value);

	}

	@Test
	public void testFileReadWithEmptyBaseLocation() {
		Krausening krausening = getKrausening(EMPTY_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		assertNull(properties);

	}

	@Test
	public void testFileReadWithEmptyExtensionsLocation() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EMPTY_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
		Object value = properties.get(FOO_PROPERTY_KEY);
		assertEquals(FOO_PROPERTY_VALUE, value);

	}

	@Test
	public void testFileLoadingWhenNonPropertiesFilesPresent() {
		Krausening krausening = getKrausening(NOT_JUST_PROPERTIES_FILES_LOCATION, EMPTY_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties("some-other-configuration-file.xml");
		assertNull(properties);

	}

}
