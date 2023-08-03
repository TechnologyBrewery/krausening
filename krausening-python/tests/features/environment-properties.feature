@environmentVariables
Feature: Support exposing krausening properties as environment variables for easier python integration

  Scenario: All krausening properties are available as python environment variables within the python interpreter scope
    Given multiple krausening properties are available:
      | propertyName | propertyValue |
      | foo          | bar           |
      | key1         | value1        |
    When the krausening-python is loaded
    Then the user can access all properties as environment variables
      | environmentVariableName | environmentVariableValue |
      | FOO                     | bar                      |
      | KEY1                    | value1                   |

  Scenario: A python library that leverages environment variables can have those variables populated by krausening-python

  Scenario: Selected krausening properties are available as python environment variables

  Scenario: No available krausening properties are available as python environment variables