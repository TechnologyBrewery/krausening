package com.ask.krausening;

import org.aeonbits.owner.KrauseningConfig;
import org.aeonbits.owner.KrauseningConfig.KrauseningSources;
import org.aeonbits.owner.KrauseningConfigFactory;
import org.bitbucket.krausening.Krausening;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestEncryptedKrausening extends TestKrausening {
	
	protected static final String ENCRYPTED_PROPERTIES = "encrypted.properties";
	protected static final String PASSWORD_KEY = "password";
	protected static final String DECRYPTED_PASSWORD_VALUE = "someStrongPassword";
	
	@BeforeEach
	public void beforeTestExecution() {
	    System.setProperty(Krausening.KRAUSENING_PASSWORD, "myMasterPassword");
	}
	
	@Test
	public void testEncryptedPropertyReadProvidesDecryptedValue() {
		Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION, NO_LOCATION);
		krausening.loadProperties();
		Properties properties = krausening.getProperties(ENCRYPTED_PROPERTIES);
		assertNotNull(properties);
		assertEquals(DECRYPTED_PASSWORD_VALUE, properties.getProperty(PASSWORD_KEY));
	}

	@Test
	public void testDecryptValueThroughOwnerInterface() {
	    EncryptedPropertyFileConfig config = KrauseningConfigFactory.create(EncryptedPropertyFileConfig.class);
	    assertEquals(DECRYPTED_PASSWORD_VALUE, config.getPassword());
	}
	
    @KrauseningSources(ENCRYPTED_PROPERTIES)
    protected interface EncryptedPropertyFileConfig extends KrauseningConfig {

        @Key(PASSWORD_KEY)
        String getPassword();
    }
}
