Feature: Apply Encryption

  Scenario Outline: a key marked with encryption sign its value should be encrypted after calling the encryption function
    Given the "<krausening_configuration>" "<location>"
    And the KRAUSENING_PASSWORD
    And the property "<key_name>" is marked with encryption sign
    When user calls the encryption function
    Then the key value is encrypted in the property file
    And the encryption sign is removed

    Examples:
      | krausening_configuration       | location                                                   | key_name |
      | KRAUSENING_BASE                | target/test-classes/resources/apply-encryption/base/       | key1     |
      | KRAUSENING_EXTENSIONS          | target/test-classes/resources/apply-encryption/extensions/ | key2     |
      | KRAUSENING_OVERRIDE_EXTENSIONS | target/test-classes/resources/apply-encryption/overrides/  | key3     |

  Scenario: a property with unencrypted value should be warned
    Given the property with the marked encryption key
    When the property is loaded
    Then an exception should be thrown
    And the exception contains the key that should be encrypted