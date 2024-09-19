from behave import given, when, then  # pylint: disable=no-name-in-module
from krausening.logging import LogManager

LOG_LEVEL_MAPPING = {
    50: 'CRITICAL',
    40: 'ERROR',
    30: 'WARNING',
    20: 'INFO',
    10: 'DEBUG',
    0: 'NOTSET',
} 

@given('a name for the logger')
def step_impl(context):
    context.name = 'test_logger'

@given('a log level of {log_level}')
def step_impl(context, log_level):
    context.log_level = log_level

@given('no log level specification')
def step_impl(context):
    context.log_level = None

@given('an invalid log level specification')
def step_impl(context):
    context.log_level = 'WARNIG'

@when('the get logger method is called')
def step_impl(context):
    if context.log_level:
        context.logger = LogManager.get_instance().get_logger(context.name, context.log_level)
    else:
        context.logger = LogManager.get_instance().get_logger(context.name)

@then('a logger is returned with a log level of {log_level}')
def step_impl(context, log_level):
    assigned_log_level = LOG_LEVEL_MAPPING[context.logger.level]
    assert assigned_log_level == log_level, f'The specified log level ({log_level}) does not match the assigned log level ({assigned_log_level}).'