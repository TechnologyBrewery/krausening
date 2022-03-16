import os

from behave import *
from krausening.properties import PropertyManager

use_step_matcher("re")

propertyManager = PropertyManager.get_instance()
properties = None

@given('a base file with property "foo" and value "bar"')
def step_impl(context):
    os.environ['KRAUSENING_BASE'] = 'config/'
    context.file = 'test.properties'

@given(u'a base file with property "foo" and an encrypted value for "bar"')
def step_impl(context):
    os.environ['KRAUSENING_BASE'] = 'config/'
    os.environ['KRAUSENING_PASSWORD'] = 'P455w0rd'
    context.file = 'test-encrypted.properties'

@when("the property file is loaded")
def step_impl(context):
    global properties
    properties = propertyManager.get_properties(context.file)

@then('the value of "foo" is set to "bar"')
def step_impl(context):
    assert properties['foo'] == 'bar'

@when('a new property file is read with property "foo" and value "bar2"')
def step_impl(context):
    os.environ['KRAUSENING_EXTENSIONS'] = 'config_extension/'
    global properties
    properties = propertyManager.get_properties('test.properties')

@then('the property value is set to "bar2"')
def step_impl(context):
    assert properties['foo'] == 'bar2'
