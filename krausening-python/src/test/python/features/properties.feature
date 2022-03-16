@properties
Feature: Property Management

  Scenario: Property can be loaded from file
    Given a base file with property "foo" and value "bar"
    When the property file is loaded
    Then the value of "foo" is set to "bar"

  Scenario: Properties can be overridden
    Given a base file with property "foo" and value "bar"
    When a new property file is read with property "foo" and value "bar2"
    Then the property value is set to "bar2"

  Scenario: Encrypted properties can be decrypted
    Given a base file with property "foo" and an encrypted value for "bar"
    When the property file is loaded
    Then the value of "foo" is set to "bar"
