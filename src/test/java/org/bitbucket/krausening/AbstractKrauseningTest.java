package org.bitbucket.krausening;

public abstract class AbstractKrauseningTest {

	protected static final String BASE_PROPERTIES_LOCATION = "./src/test/resources/base";
	protected static final String EXTENSIONS_PROPERTIES_LOCATION = "./src/test/resources/extensions";
	protected static final String OVERRIDDEN_EXTENSIONS_LOCATION = "./src/test/resources/war-specific-overrides";
	protected static final String WAR_1_PROPERTIES = "/test-war-1";
	protected static final String WAR_2_PROPERTIES = "/test-war-2";
	protected static final String FOO_PROPERTY_KEY = "foo";
	protected static final String FOO_PROPERTY_VALUE = "bar";
	protected static final String OVERRIDDEN_PROPERTY_KEY = "override.me";
	protected static final String OVERRIDDEN_PROPERTY_VALUE = "some-localized-value";
	protected static final String WAR_1_OVERRIDDEN_PROPERTY_VALUE = "test-war-1-specific-value";
	protected static final String WAR_2_OVERRIDDEN_PROPERTY_VALUE = "test-war-2-specific-value";
	protected static final String EXAMPLE_PROPERTIES_FILE_NAME = "example.properties";
	protected static final String NO_LOCATION = "";
	
	protected Krausening getKrausening(String baseLocation, String extensionLocation) {
		return getKrausening(baseLocation, extensionLocation, NO_LOCATION);
	}
	
	protected Krausening getKrausening(String baseLocation, String extensionLocation, String overrideLocation) {
        System.setProperty(Krausening.BASE_LOCATION, baseLocation);
        System.setProperty(Krausening.EXTENSIONS_LOCATION, extensionLocation);
        System.setProperty(Krausening.OVERRIDE_EXTENSIONS_LOCATION, overrideLocation);
        Krausening krausening = Krausening.getInstance();
        return krausening;
    }
}
