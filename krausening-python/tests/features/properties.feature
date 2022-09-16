@properties
Feature: Property Management

  Scenario: Property can be loaded from file
    Given a base properties file with property "foo"
    When the properties file is loaded
    Then the retrieved value of "foo" is "bar"

  Scenario: Properties can be overridden
    Given a base properties file with property "foo"
    And an extensions properties file with property "foo"
    When the properties file is loaded
    Then the retrieved value of "foo" is "bar2"

  Scenario: Encrypted properties can be decrypted
    Given a base properties file with property "foo"
    And the properties file contains encrypted value for the "foo" property
    When the properties file is loaded
    Then the retrieved value of "foo" is "bar"

  Scenario: Property value can be encrypted and decrypted back to the same value
    Given a base properties file with property "foo"
    And encrypt the "foo" property value
    When decrypt the encrypted "foo" property value
    And the decrypted value matches original value "bar"
