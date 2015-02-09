package com.ask.krausening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.bitbucket.krausening.Krausening;
import org.junit.Test;

public class TestEncryptedKrausening extends TestKrausening {
	
	protected static final String ENCRYPTED_PROPERTIES = "encrypted.properties";
	protected static final String PASSWORD_KEY = "password";

	/**
	 * {@inheritDoc}
	 * 
	 * Added in a master password to the basic Krausening setup to regression test existing scenarios
	 * as well as to enable encrypted property settings.
	 */
	protected Krausening getKrausening(String baseLocation,	String extensionLocation) {
		System.setProperty(Krausening.KRAUSENING_PASSWORD, "myMasterPassword");
		return super.getKrausening(baseLocation, extensionLocation);
	}
	
	@Test
	public void testEncryptedPropertyReadProvidesDecryptedValue() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(ENCRYPTED_PROPERTIES);
		assertNotNull(properties);
		assertEquals(properties.getProperty(PASSWORD_KEY), "someStrongPassword");

	}

}
