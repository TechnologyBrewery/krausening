import os

from behave import given, when, then  # pylint: disable=no-name-in-module
from krausening.properties import PropertyManager
from nose.tools import assert_equal, assert_not_equal
from time import sleep


@given('a properties file with property "foo" is loaded')
def a_properties_file_with_property_is_loaded(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"
    context.properties = PropertyManager.get_instance().get_properties(
        context.file, force_reload=True
    )


@when('the value of "foo" is changed')
def the_value_of_is_changed(context):
    context.new_foo_value = "new_value"
    context.properties["foo"] = context.new_foo_value


@then("subsequent retrievals of the property file will reflect the changed value")
def subsequent_retrievals_of_the_property_file_will_reflect_the_changed_value(context):
    new_props = PropertyManager.get_instance().get_properties(context.file)
    foo_property_value = new_props["foo"]
    assert_equal(
        foo_property_value,
        context.new_foo_value,
        f"Retrieved 'foo' property, which is {foo_property_value}, didn't match expected value {context.new_foo_value}",
    )


@given('a properties file containing property "bar" exists')
def a_properties_file_containing_property_exists(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file_name = "hot_reload.properties"
    context.initial_bar_value = "some_value"
    context.abs_file_path = os.path.join(
        os.environ.get("KRAUSENING_BASE"), context.file_name
    )
    with open(context.abs_file_path, "w") as prop_file:
        prop_file.write(f"bar: {context.initial_bar_value}\n")


@given("the created properties file has been loaded")
def the_created_properties_file_has_been_loaded(context):
    context.properties = PropertyManager.get_instance().get_properties(
        context.file_name, force_reload=True
    )


@when('the value of "bar" is changed in the properties file')
def the_value_of_is_changed_in_the_properties_file(context):
    with open(context.abs_file_path, "w") as prop_file:
        prop_file.write(f"bar: Now {context.initial_bar_value} WITH CHANGES!\n")
    # Sleep to give the filewatcher a chance to process the update
    sleep(1)


@then('the value of "bar" will automatically be updated in memory')
def the_value_of_will_automatically_be_updated_in_memory(context):
    assert_not_equal(
        context.properties["bar"],
        context.initial_bar_value,
        f"Updated value was not reflected in memory!  Initial value: {context.initial_bar_value}.  Current value: {context.properties['bar']}",
    )

    with open(context.abs_file_path, "w") as prop_file:
        prop_file.write(f"bar: {context.initial_bar_value}\n")
