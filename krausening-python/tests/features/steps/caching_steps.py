import os

from behave import *
from krausening.properties import PropertyManager
from nose.tools import assert_equal, assert_not_equal
from time import sleep


@given('a properties file with property "foo" is loaded')
def step_impl(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"
    context.properties = PropertyManager.get_instance().get_properties(
        context.file, force_reload=True
    )


@when('the value of "foo" is changed')
def step_impl(context):
    context.new_foo_value = "new_value"
    context.properties["foo"] = context.new_foo_value


@then("subsequent retrievals of the property file will reflect the changed value")
def step_impl(context):
    new_props = PropertyManager.get_instance().get_properties(context.file)
    foo_property_value = new_props["foo"]
    assert_equal(
        foo_property_value,
        context.new_foo_value,
        f"Retrieved 'foo' property, which is {foo_property_value}, didn't match expected value {context.new_foo_value}",
    )


@given('a properties file containing property "bar" exists')
def step_impl(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file_name = "hot_reload.properties"
    context.initial_bar_value = "some_value"
    context.abs_file_path = os.path.join(
        os.environ.get("KRAUSENING_BASE"), context.file_name
    )
    with open(context.abs_file_path, "w") as prop_file:
        prop_file.write(f"bar: {context.initial_bar_value}\n")


@given("the created properties file has been loaded")
def step_impl(context):
    context.properties = PropertyManager.get_instance().get_properties(
        context.file_name, force_reload=True
    )


@when('the value of "bar" is changed in the properties file')
def step_impl(context):
    with open(context.abs_file_path, "w") as prop_file:
        prop_file.write(f"bar: Now {context.initial_bar_value} WITH CHANGES!\n")
    # Sleep to give the filewatcher a chance to process the update
    sleep(2)


@then('the value of "bar" will automatically be updated in memory')
def step_impl(context):
    assert_not_equal(
        context.properties["bar"],
        context.initial_bar_value,
        f"Updated value was not reflected in memory!  Initial value: {context.initial_bar_value}.  Current value: {context.properties['bar']}",
    )

    with open(context.abs_file_path, "w") as prop_file:
        prop_file.write(f"bar: {context.initial_bar_value}\n")
