import os

from behave import given, when, then
from krausening.properties import PropertyManager


# Scenario: Ensuring property exists in base file
@given('a property, "{key}", exists in a base properties file')
def step_impl(context, key):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"
    context.base_properties = PropertyManager.get_instance().\
            get_properties(context.file)
    assert key in context.base_properties
    
    
@when('the base-specific property of "{key}" is requested')
def step_impl(context, key):
    assert context.base_properties.get(key) is not None


@then('the base-specific property value of "{key}" is "{value}"')
def step_impl(context, key, value):
    assert value == context.base_properties[key]


# Scenario: Ensuring property exists in both base and extension files.
@given('a property does not exist in a base or extension, but does exist as an environment variable')
def step_impl(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    os.environ["KRAUSENING_EXTENSIONS"] = "tests/resources/config_extension/"
    context.filename = "test.properties"


@when('a property we know that is not present, like "{key}", is requested')
def step_impl(context, key):
    os.environ["KRAUSENING_EXTENSIONS"] = "tests/resources/config_extension/"
    context.ext_properties = PropertyManager.get_instance().get_properties(context.filename)
    try:
        context.ext_properties[key]
        assert False
    except (TypeError, KeyError):
        assert True


@then('the property value for "{key}" is returned')
def step_impl(context, key):
    if key not in os.environ:
        assert True
    else:
        assert os.environ[key]
