Feature: Krausening command line interface

  Scenario: Krausening properties files can be updated to be encrypted
    Given a set of properties files in a directory
    And the environment variable KRAUSENING_PASSWORD is set
    And the properties are marked with encryption sign
    When the user runs krausening encrypt files {directory}
    Then the command runs successfully
    And all properties file in the directory are updated to the encrypt values
