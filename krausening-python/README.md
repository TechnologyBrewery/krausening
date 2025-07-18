# Krausening Python - Externalized Property Management and Access for Python Projects #
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)
[![PyPI](https://img.shields.io/pypi/v/krausening?logo=python&logoColor=gold)](https://pypi.org/project/krausening/)
![PyPI - Python Version](https://img.shields.io/pypi/pyversions/krausening?logo=python&logoColor=gold)
![PyPI - Wheel](https://img.shields.io/pypi/wheel/krausening?logo=python&logoColor=gold)
[![PyPI - Downloads](https://img.shields.io/pypi/dm/krausening?color=blue&label=Installs&logo=pypi&logoColor=gold)](https://pypi.org/project/krausening/)

Krausening property management and encryption for Python is packaged using the open-source Python Maven plugin [Habushu](https://github.com/TechnologyBrewery/habushu) and made available as a [PyPI package](https://pypi.org/project/krausening/).  

## Distribution Channel

Krausening Python is published to PyPI under the [krausening](https://pypi.org/project/krausening/) project and may be installed using any package installer/manager that leverages PyPI.  For example:

* [Poetry](https://python-poetry.org/) - `poetry add krausening`
* [pip](https://pip.pypa.io/) - `pip install krausening`

## Managing Properties with Krausening and Python

Managing properties with Krausening's Python library utilizes a similar approach to that required by Krausening Java. Krausening Python expects that developers prime their target environment by configuring the following environment variables (which are named and leveraged in the same manner as the Java System Properties expected by Krausening Java):

* `KRAUSENING_BASE`
* `KRAUSENING_EXTENSIONS`
* `KRAUSENING_OVERRIDE_EXTENSIONS`
* `KRAUSENING_PASSWORD`

In order to use the Krausening Python, developers may directly use `PropertyManager` or extend `PropertyManager` to provide a custom interface.  For example, developers may directly use the `PropertyManager` as such:

```python
from krausening.properties import PropertyManager

propertyManager = PropertyManager.get_instance()
properties = None
properties = propertyManager.get_properties('my-property-file.properties')
assert properties['foo'] == 'bar2'
```

This has the disadvantage that you must know the property keys in order to find the corresponding property values. To mitigate the need for all property file consumers to rely on specific property keys, consider wrapping the `PropertyManager` and writing your own custom methods to get the corresponding keys and values, abstracting away the exact key values:

```python
from krausening.properties import PropertyManager

class TestConfig():
    """
    Configurations utility class for being able to read in and reload properties
    """

    def __init__(self):
        self.properties = None
        self.reload()
 
    def integration_test_enabled(self):
        """
        Returns whether the integration tests are enabled or not
        """
        integration_test_enable = False
        integration_enable_str = self.properties['integration.test.enabled']
        if (integration_enable_str):
            integration_test_enable = (integration_enable_str == 'True')
        return integration_test_enable
    
    def reload(self):
        self.properties = PropertyManager.get_instance().get_properties('test.properties')
```
### Using Encrypted Properties

Frequently, it is useful to store encrypted information within properties files.  Krausening optionally leverages Jasypt
to allow stored properties to be encrypted at rest while also decrypting property values as they are read without manual
interaction.

### 1. Encrypting Properties

#### Krausening CLI

The simplest way to encrypt your secrets for Krausening is to use the [Krausening CLI](https://pypi.org/project/krausening-cli)
`encrypt files` command.

#### Interactive Python Environment

If you're already using Krausening within an interactive Python environment, like a Jupyter Notebook, you can directly
call the same encryption function that the CLI uses!

1. In your `.properties` files, mark any properties you want encrypted by prefixing the key with `**`.
   ```properties
    **my.secret=someStrongPassword
   ```
2. Set the environment variable KRAUSENING_PASSWORD to point to your encryption password.
   ```python
   import os
   os.environ['KRAUSENING_PASSWORD']='myEncryptPassword'
   ```
3. Call applyEncryption() function
   ```python
   from krausening import apply_encryption 
   apply_encryption()
   ```
4. Across all properties files in the base, extensions, and overrides directories; properties
   marked for encryption are updated with their encrypted value, and the marker is removed from the key.
   ```properties
   my.secret=ENC(QtHAKNIA+CNaqpdKAPopIg8n6UvEOVCRlTMrjF6ejugNKWJD2bqxEEFUbxmM/how)
   ```

### 2. Decrypting Properties

1. set KRAUSENING_PASSWORD to point to your encryption password.
   ```properties
   KRAUSENING_PASSWORD=myEncryptPassword
   ```
2. When you look for your property, you'll now get the decrypted value:
   ```python
   propertyManager = PropertyManager.get_instance()
   properties = propertyManager.get_properties('example.properties')
   assert "someStrongPassword" == properties.getProperty("my.secret")
   ```
