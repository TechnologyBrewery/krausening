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

  Scenario: Properties can be overridden in a specific context
      Given a base properties file with property "foo"
      And an extensions properties file with property "foo"
      And a context-specific override properties file with property "foo"
      When the properties file is loaded
      Then the retrieved value of "foo" is "bar3"

  Scenario: Default properties can be retrieved from unencrypted property manager
    Given an environment variable "KRAUSENING_PASSWORD" is set
    When the test config is loaded
    Then the test config retrieved value of "baz" is "default"

  Scenario: Default properties can be retrieved from encrypted property manager
    Given an environment variable "KRAUSENING_PASSWORD" is not set
    When the test config is loaded
    Then the test config retrieved value of "baz" is "default"

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

  Scenario: Property value contains an environment variable
    Given an environment variable "TEST_VAR" is set
    And a properties file contains a value "test" referencing the TEST_VAR environment variable
    When the properties file is loaded
    Then the value of TEST_VAR will be substituted into the value of test
