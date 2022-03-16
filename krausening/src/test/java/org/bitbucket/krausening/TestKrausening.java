package org.bitbucket.krausening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestKrausening extends AbstractKrauseningTest {

    protected static final String VALUE_NEW_VALUE = "new.value";
    protected static final String KEY_NEWLY_ADDED_IN_EXTENSIONS = "newly.added.in.extensions";
    protected static final String VALUE_NEWLY_ADDED_IN_OVERRIDDEN_EXTENSIONS = "test-war-1-new-value";
    protected static final String KEY_NEWLY_ADDED_IN_OVERRIDDEN_EXTENSIONS = "newly.added.in.overridden.extensions";
    protected static final String NONEXISTENT_LOCATION = "./src/test/resources/does-not-exist";
    protected static final String EMPTY_LOCATION = "./src/test/resources/empty";
    protected static final String NOT_JUST_PROPERTIES_FILES_LOCATION = "./src/test/resources/not-just-properties-files";
    protected static final String EMPTY_PROPERTIES = "empty.properties";

    @Before
    public void reloadKrausening() throws Exception {
        System.setProperty(Krausening.BASE_LOCATION, BASE_PROPERTIES_LOCATION);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, EXTENSIONS_PROPERTIES_LOCATION);
        Krausening krausening = Krausening.getInstance();
        krausening.loadProperties();
    }

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
    public void testOverrideExtensionPropertyReadOfNewValue() {
        Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION,
                OVERRIDDEN_EXTENSIONS_LOCATION, WAR_1_PROPERTIES_SUBFOLDER);
        krausening.loadProperties();

        Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
        Object value = properties.get(KEY_NEWLY_ADDED_IN_OVERRIDDEN_EXTENSIONS);
        assertEquals(VALUE_NEWLY_ADDED_IN_OVERRIDDEN_EXTENSIONS, value);
    }

    @Test
    public void testOverrideExtensionPropertiesReadOfOverriddenExtension() {
        Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION,
                OVERRIDDEN_EXTENSIONS_LOCATION, WAR_2_PROPERTIES_SUBFOLDER);
        krausening.loadProperties();

        Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
        Object value = properties.get(OVERRIDDEN_PROPERTY_KEY);
        assertEquals(WAR_2_OVERRIDDEN_PROPERTY_VALUE, value);
    }

    @Test
    public void testOverrideExtensionPropertiesReadOfBaseValue() {
        Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION,
                OVERRIDDEN_EXTENSIONS_LOCATION, WAR_2_PROPERTIES_SUBFOLDER);
        krausening.loadProperties();

        Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
        Object value = properties.get(FOO_PROPERTY_KEY);
        assertEquals(FOO_PROPERTY_VALUE, value);
    }

    @Test
    public void testOverriddenExtensionProperties() {
        Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION,
                OVERRIDDEN_EXTENSIONS_LOCATION, WAR_1_PROPERTIES_SUBFOLDER);
        krausening.loadProperties();

        Properties properties = krausening.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
        Object value = properties.get(KEY_NEWLY_ADDED_IN_OVERRIDDEN_EXTENSIONS);
        assertEquals(VALUE_NEWLY_ADDED_IN_OVERRIDDEN_EXTENSIONS, value);
    }
    
    @Test
    public void testOverriddeExtensionPropertiesRuntimeRequest() {
        Krausening krausening1 = getKrausening(BASE_PROPERTIES_LOCATION, EXTENSIONS_PROPERTIES_LOCATION,
                OVERRIDDEN_EXTENSIONS_LOCATION, WAR_1_PROPERTIES_SUBFOLDER);
        krausening1.loadProperties();
        
        // grab a second set of properties based on the second sub folder
        Krausening krausening2 = Krausening.getInstance(WAR_2_PROPERTIES_SUBFOLDER);
        
        // verify that we can get the properties from the default instance
        Properties properties1 = krausening1.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
        Object value1 = properties1.get(OVERRIDDEN_PROPERTY_KEY);
        assertEquals(WAR_1_OVERRIDDEN_PROPERTY_VALUE, value1);
        
        // verify that we can get the properties from the context specific instance
        Properties properties2 = krausening2.getProperties(EXAMPLE_PROPERTIES_FILE_NAME);
        Object value2 = properties2.get(OVERRIDDEN_PROPERTY_KEY);
        assertEquals(WAR_2_OVERRIDDEN_PROPERTY_VALUE, value2);
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
