package org.technologybrewery.krausening;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestPropertiesEncryption extends AbstractKrauseningTest {

    protected static final String BASE_PROPERTIES_LOCATION_IN_OUTPUT_DIR = "./target/test-classes/apply-encryptions/base/";
    protected static final String EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR = "./target/test-classes/apply-encryptions/extensions/";
    protected static final String OVERRIDDEN_EXTENSIONS_LOCATION_IN_OUTPUT_DIR = "./target/test-classes/apply-encryptions/overrides/";
    protected static final String FILE_NAME = "test-apply-encryption.properties";

    @Test
    public void testLoadPropertyWithEncryptionMarkKey() throws Exception {
        try {
            Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION_IN_OUTPUT_DIR, EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR);
            krausening.loadProperties();
        } catch (KrauseningException krauseningException) {
            assertTrue(krauseningException.getMessage().contains(Krausening.ENCRYPTION_MARK + "key1"));
        }
    }

    @Test
    public void testPropertiesEncryptedAndEncryptionMarkRemoved() throws Exception {
        Krausening krausening = getKrausening(BASE_PROPERTIES_LOCATION_IN_OUTPUT_DIR, EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR, OVERRIDDEN_EXTENSIONS_LOCATION_IN_OUTPUT_DIR, "");
        krausening.applyEncryption();
        assertEncryptedPropertyKeyAndValue(BASE_PROPERTIES_LOCATION_IN_OUTPUT_DIR + FILE_NAME, "key1");
        assertEncryptedPropertyKeyAndValue(EXTENSIONS_PROPERTIES_LOCATION_IN_OUTPUT_DIR + FILE_NAME, "key2");
        assertEncryptedPropertyKeyAndValue(OVERRIDDEN_EXTENSIONS_LOCATION_IN_OUTPUT_DIR + FILE_NAME, "key3");
    }
    
    private void assertEncryptedPropertyKeyAndValue(String fileName, String key) throws IOException {
        File file = new File( fileName);
        String value = getProperty(file, key);
        assertTrue(value.startsWith("ENC("));
        value = getProperty(file, Krausening.ENCRYPTION_MARK + key);
        assertTrue(value == null);
    }

    private String getProperty(File file, String key) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        for (String line: lines) {
            if (line.startsWith(key)) {
                key = key + "=";
                return line.substring(key.length());
            }
        }
        return null;
    }
}
