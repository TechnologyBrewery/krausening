Feature: Property Externalization

  Scenario: Ensuring property exists in base file
    Given a property, "foo", exists in a base properties file
    When the base-specific property of "foo" is requested
    Then the base-specific property value of "foo" is "bar"

  Scenario: Ensuring property exists in both base and extension files.
    Given a property does not exist in a base or extension, but does exist as an environment variable
    When a property we know that is not present, like "abc", is requested
    Then the property value for "abc" is returned