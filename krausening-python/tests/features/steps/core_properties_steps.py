import os

from behave import *
from krausening.properties import PropertyManager
from nose.tools import assert_equal
from test_config import TestConfig


def bar_num_assert(foo_property_value, bar_num_str):
    bar_val = "bar" + bar_num_str


@given('a base properties file with property "foo"')
def step_impl(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"


@given('an extensions properties file with property "foo"')
def step_impl(context):
    os.environ["KRAUSENING_EXTENSIONS"] = "tests/resources/config_extension/"


@given('a context-specific override properties file with property "foo"')
def step_impl(context):
    os.environ["KRAUSENING_OVERRIDE_EXTENSIONS"] = "tests/resources/config_override/"


@given('the properties file contains encrypted value for the "foo" property')
def step_impl(context):
    os.environ["KRAUSENING_PASSWORD"] = "P455w0rd"
    context.file = "test-encrypted.properties"


@given('an environment variable "{env_var}" is set')
def step_impl(context, env_var):
    os.environ[env_var] = "test value!"


@given('an environment variable "{env_var}" is not set')
def step_impl(context, env_var):
    os.environ[env_var] = ""


@given(
    'a properties file contains a value "test" referencing the TEST_VAR environment variable'
)
def step_impl(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"


@when("the properties file is loaded")
def step_impl(context):
    context.properties = PropertyManager.get_instance().get_properties(
        context.file, force_reload=True
    )


@when("the test config is loaded")
def step_impl(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.properties = TestConfig()


@then('the test config retrieved value of "{key}" is "{value}"')
def step_impl(context, key, value):
    retrieved_value = getattr(context.properties, key)
    assert_equal(
        retrieved_value,
        value,
        f"Retrieved {key} property, which is {retrieved_value}, didn't match expected value",
    )


@then('the retrieved value of "{foo}" is "{bar_val}"')
def step_impl(context, foo, bar_val):
    foo_property_value = context.properties[foo]
    assert_equal(
        foo_property_value,
        bar_val,
        f"Retrieved 'foo' property, which is {foo_property_value}, didn't match expected value",
    )


@given('encrypt the "foo" property value')
def step_impl(context):
    os.environ["KRAUSENING_PASSWORD"] = "P455w0rd"
    context.properties = PropertyManager.get_instance().get_properties(
        context.file, force_reload=True
    )
    context.encrypted_value = context.properties._encrypt("foo")


@when('decrypt the encrypted "foo" property value')
def step_impl(context):
    context.decrypted_value = context.properties._decrypt(context.encrypted_value)


@when('the decrypted value matches original value "bar"')
def step_impl(context):
    assert_equal(
        context.decrypted_value,
        "bar",
        f"Decrypted the encrypted property value , which is {context.decrypted_value}, didn't match expected value",
    )


@then("the value of TEST_VAR will be substituted into the value of test")
def step_impl(context):
    test_value = context.properties["test"]
    substitution_exists = os.environ["TEST_VAR"] in test_value
    raw_var_does_not_exist = "${TEST_VAR}" not in test_value
    assert_equal(
        substitution_exists and raw_var_does_not_exist,
        True,
        f"Retrieved 'test' property, which is {test_value}, didn't match expected value",
    )
