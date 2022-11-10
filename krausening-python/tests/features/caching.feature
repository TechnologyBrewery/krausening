@properties
Feature: Property Caching

  Scenario: Properties are held in memory
    Given a properties file with property "foo" is loaded
    When the value of "foo" is changed
    Then subsequent retrievals of the property file will reflect the changed value

  Scenario: A properties file is updated on disk
    Given a properties file containing property "bar" exists
    And the created properties file has been loaded
    When the value of "bar" is changed in the properties file
    Then the value of "bar" will automatically be updated in memory