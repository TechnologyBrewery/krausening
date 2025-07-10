import os

from behave import given, when, then
from krausening.properties import PropertyManager, apply_encryption


@given('the "{krausening_configuration}" "{location}"')
def the_krausening_configuration_location(context, krausening_configuration, location):
    os.environ[krausening_configuration] = location
    context.config_location = location
    context.file = "test-apply-encryption.properties"


@given("the KRAUSENING_PASSWORD")
def the_krausening_password(context):
    os.environ["KRAUSENING_PASSWORD"] = "master password"


@given('the property "{key_name}" is marked with encryption sign')
def the_property_key_name_is_marked_with_encryption_sign(context, key_name):
    context.key_name = key_name


@given("the property with the marked encryption key")
def the_property_with_marked_with_encryption_key(context):
    os.environ["KRAUSENING_BASE"] = (
        "target/test-classes/resources/apply-encryption/unencrypted/"
    )


@when("user calls the encryption function")
def user_calls_the_encryption_function(context):
    apply_encryption()
    # read the file content after apply encryption
    with open(f"{context.config_location}{context.file}", "r") as f:
        context.file_content = f.readlines()


@when("the property is loaded")
def the_property_is_loaded(context):
    try:
        PropertyManager.get_instance().get_properties(
            "test-apply-encryption.properties"
        )
    except Exception as error:
        context.error = f"{error}"


@then("the key value is encrypted in the property file")
def the_key_value_is_encrypted_in_the_property_file(context):
    for line in context.file_content:
        if line and not line.startswith("#"):
            key = line.split("=", 1)[0]
            value = line.split("=", 1)[1]
            if key == context.key_name:
                assert value.startswith("ENC(")


@then("the encryption sign is removed")
def the_retrieved_value_of_is(context):
    for line in context.file_content:
        if line and not line.startswith("#"):
            key = line.split("=", 1)[0]
            if key == context.key_name:
                assert not key.startswith("**")


@then("an exception should be thrown")
def an_exception_should_be_thrown(context):
    assert context.error


@then("the exception contains the key that should be encrypted")
def the_exception_contains_key_that_should_be_encrypted(context):
    assert "**key4" in context.error
