import os

from behave import given, when, then
from assertpy import assert_that
from click.testing import CliRunner, Result

from krausening_cli.cli import krausening


@given("a set of properties files in a {}")
def the_krausening_configuration_location(context, key):
    if not hasattr(context, "command_args"):
        context.command_args = {}
    context.config_location = "target/test-classes/resources/apply-encryption/"
    context.file = "test-apply-encryption.properties"
    context.command_args[key] = f"{context.config_location}"


@given("the environment variable KRAUSENING_PASSWORD is set")
def the_krausening_password(context):
    os.environ["KRAUSENING_PASSWORD"] = "master password"


@given("the properties are marked with encryption sign")
def the_property_key_name_is_marked_with_encryption_sign(context):
    # properties marked in the test properties file
    pass


@when("the user runs krausening {command}")
def user_calls_the_encryption_function(context, command):
    runner = CliRunner()
    cmd_with_args = command.format(**context.command_args)
    context.result = runner.invoke(krausening, cmd_with_args.split())


@then("the command runs successfully")
def the_command_exits_with_a_success_status(context):
    result: Result = context.result
    assert_that(result.stderr, description="StdErr empty").is_empty()
    assert_that(result.exit_code, description="Exit Code").is_equal_to(0)


@then("all properties file in the directory are updated to the encrypt values")
def the_retrieved_value_of_is(context):
    with open(f"{context.config_location}{context.file}", "r") as f:
        context.file_content = f.readlines()
    for line in context.file_content:
        line = line.strip()
        if line and not line.startswith("#"):
            key, value = line.split("=", 1)
            (
                assert_that(value, f"Property '{key}' value")
                .starts_with("ENC(")
                .ends_with(")")
            )
