package com.ask.krausening;

import org.bitbucket.krausening.Krausening;

public abstract class AbstractKrauseningTest {

	protected static final String BASE_PROPERTIES_LOCATION = "./src/test/resources/base";
	protected static final String EXTENSIONS_PROPERTIES_LOCATION = "./src/test/resources/extensions";
	protected static final String FOO_PROPERTY_KEY = "foo";
	protected static final String FOO_PROPERTY_VALUE = "bar";
	protected static final String OVERRIDDEN_PROPERTY_KEY = "override.me";
	protected static final String OVERRIDDEN_PROPERTY_VALUE = "some-localized-value";
	protected static final String EXAMPLE_PROPERTIES_FILE_NAME = "example.properties";
	
	protected Krausening getKrausening(String baseLocation, String extensionLocation) {
		System.setProperty(Krausening.BASE_LOCATION, baseLocation);
		System.setProperty(Krausening.EXTENSIONS_LOCATION, extensionLocation);
		Krausening krausening = Krausening.getInstance();
		return krausening;
	}
}
