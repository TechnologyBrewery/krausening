@logging
Feature: Log Level Specification

    Scenario Outline: Set the logging level by specifying the log level
        Given a name for the logger
        And a log level of <log_level>
        When the get logger method is called
        Then a logger is returned with a log level of <log_level>

        Examples: Filepaths with or without the container prefix only
            | log_level |               
            | CRITICAL  |
            | ERROR     |
            | WARNING   |
            | INFO      |
            | DEBUG     |
            | NOTSET    |

    Scenario: The logging level is set to INFO when the log level is not specified
        Given a name for the logger
        And no log level specification
        When the get logger method is called
        Then a logger is returned with a log level of INFO

    Scenario: The logging level is set to INFO when the log level is not a valid level
        Given a name for the logger
        And an invalid log level specification
        When the get logger method is called
        Then a logger is returned with a log level of INFO
