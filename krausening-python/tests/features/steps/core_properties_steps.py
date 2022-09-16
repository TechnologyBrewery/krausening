import os

from behave import *
from krausening.properties import PropertyManager
from nose.tools import assert_equal


@given('a base properties file with property "foo"')
def step_impl(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"


@given('an extensions properties file with property "foo"')
def step_impl(context):
    os.environ["KRAUSENING_EXTENSIONS"] = "tests/resources/config_extension/"


@given('the properties file contains encrypted value for the "foo" property')
def step_impl(context):
    os.environ["KRAUSENING_PASSWORD"] = "P455w0rd"
    context.file = "test-encrypted.properties"


@when("the properties file is loaded")
def step_impl(context):
    context.properties = PropertyManager.get_instance().get_properties(context.file)


@then('the retrieved value of "foo" is "bar"')
def step_impl(context):
    foo_property_value = context.properties["foo"]
    assert_equal(
        foo_property_value,
        "bar",
        f"Retrieved 'foo' property, which is {foo_property_value}, didn't match expected value",
    )


@then('the retrieved value of "foo" is "bar2"')
def step_impl(context):
    foo_property_value = context.properties["foo"]
    assert_equal(
        foo_property_value,
        "bar2",
        f"Retrieved 'foo' property, which is {foo_property_value}, didn't match expected value",
    )


@given('encrypt the "foo" property value')
def step_impl(context):
    os.environ["KRAUSENING_PASSWORD"] = "P455w0rd"
    context.properties = PropertyManager.get_instance().get_properties(context.file)
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
