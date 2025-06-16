import os

from behave import given, when, then
from krausening.properties import PropertyManager
from test_config import TestConfig


def bar_num_assert(foo_property_value, bar_num_str):
    "bar" + bar_num_str


@given('a base properties file with property "foo"')
def a_base_properties_file_with_property(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"


@given('an extensions properties file with property "foo"')
def an_extensions_properties_file_with_property(context):
    os.environ["KRAUSENING_EXTENSIONS"] = "tests/resources/config_extension/"


@given('a context-specific override properties file with property "foo"')
def a_context_specific_override_properties_file_with_property(context):
    os.environ["KRAUSENING_OVERRIDE_EXTENSIONS"] = "tests/resources/config_override/"


@given('the properties file contains encrypted value for the "foo" property')
def the_properties_file_contains_encrypted_value_for_the_property(context):
    os.environ["KRAUSENING_PASSWORD"] = "P455w0rd"
    context.file = "test-encrypted.properties"


@given('an environment variable "{env_var}" is set')
def an_environment_variable_is_set(context, env_var):
    os.environ[env_var] = "test value!"


@given('an environment variable "{env_var}" is not set')
def an_environment_variable_i_not_set(context, env_var):
    os.environ[env_var] = ""


@given(
    'a properties file contains a value "test" referencing the TEST_VAR environment variable'
)
def a_properties_file_contains_a_value_referencing_the_test_var_environment_variable(
    context,
):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.file = "test.properties"


@when("the properties file is loaded")
def the_properties_file_is_loaded(context):
    context.properties = PropertyManager.get_instance().get_properties(
        context.file, force_reload=True
    )


@when("the test config is loaded")
def the_test_config_is_loaded(context):
    os.environ["KRAUSENING_BASE"] = "tests/resources/config/"
    context.properties = TestConfig()


@then('the test config retrieved value of "{key}" is "{value}"')
def the_test_config_retrieved_value_of_is(context, key, value):
    retrieved_value = getattr(context.properties, key)
    assert retrieved_value == value


@then('the retrieved value of "{foo}" is "{bar_val}"')
def the_retrieved_value_of_is(context, foo, bar_val):
    foo_property_value = context.properties[foo]
    assert foo_property_value == bar_val


@given('encrypt the "foo" property value')
def encrypt_the_property_value(context):
    os.environ["KRAUSENING_PASSWORD"] = "P455w0rd"
    context.properties = PropertyManager.get_instance().get_properties(
        context.file, force_reload=True
    )
    context.encrypted_value = context.properties._encrypt("foo")


@when('decrypt the encrypted "foo" property value')
def decrypt_the_encrypted_property_value(context):
    context.decrypted_value = context.properties._decrypt(context.encrypted_value)


@when('the decrypted value matches original value "bar"')
def the_decrypted_value_matches_original_value(context):
    assert context.decrypted_value == "bar"


@then("the value of TEST_VAR will be substituted into the value of test")
def the_value_of_test_var_will_be_substituted_into_the_value_of_test(context):
    test_value = context.properties["test"]
    substitution_exists = os.environ["TEST_VAR"] in test_value
    raw_var_does_not_exist = "${TEST_VAR}" not in test_value
    assert substitution_exists and raw_var_does_not_exist
