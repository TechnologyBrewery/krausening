# Krausening Python - Externalized Property Management and Access for Python Projects #
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)
[![PyPI](https://img.shields.io/pypi/v/krausening?logo=python&logoColor=gold)](https://pypi.org/project/krausening/)
![PyPI - Python Version](https://img.shields.io/pypi/pyversions/krausening?logo=python&logoColor=gold)
![PyPI - Wheel](https://img.shields.io/pypi/wheel/krausening?logo=python&logoColor=gold)
[![PyPI - Downloads](https://img.shields.io/pypi/dm/krausening?color=blue&label=Installs&logo=pypi&logoColor=gold)](https://pypi.org/project/krausening/)

Krausening property management and encryption for Python is packaged using the open-source Python Maven plugin [Habushu](https://bitbucket.org/cpointe/habushu) and made available as a [PyPI package](https://pypi.org/project/krausening/).  

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
### Properties Encryption

Frequently, it is useful to store encrypted information within properties files.  Krausening optionally leverages Jasypt to allow stored properties to be encrypted at rest while also decrypting property values as they are read without manual interaction.

1.  set KRAUSENING_PASSWORD to point to your Jasypt master encryption password.
```properties
KRAUSENING_PASSWORD=myMasterPassword
```
2. A) Use `apply_encryption()` function to encrypt the secrets in your properties files
    * 1- In the `.properties` file, use the `**` to mark the key for its value to be encrypted (**Note**: Krausening will not load the properties if there is any key left with the encryption mark `**`)
   ```properties
    # in $KRAUSENING_BASE/foo/example.properties:
    **my.secret=X
   
    # in $KRAUSENING_EXTENSIONS/example.properties:
    **my.secret=Y
   ```
    * 2- Call applyEncryption() function
   ```python
   #import apply_encryption function
   from krausening.properties import apply_encryption 
   
   # call apply_encryption function
   apply_encryption()
   ```
    * 3- Secrets are encrypted and encryption mark (`**`) is removed from the key
   ```properties
    # in $KRAUSENING_BASE/example.properties:
    my.secret=ENC(QtHAKNIA+CNaqpdKAPopIg8n6UvEOVCRlTMrjF6ejugNKWJD2bqxEEFUbxmM/how)
   
    # in $KRAUSENING_EXTENSIONS/example.properties:
    my.secret=ENC(MsXDFMLQEFLnPyDWrex25QToJA4khyXUEV0iYaWghwqP7aDp3YRGHxYTzSdEBu5R)
   ```
2. B) Alternatively, you can manually encrypt the secret. Ref: [Krausening in Four Pints (Leveraging Jasypt for Encrypting/Decrypting Properties) - 2. B)](../krausening/README.md#krausening-in-four-pints-leveraging-jasypt-for-encryptingdecrypting-properties)
3. When you look for your property, you'll now get the decrypted value:
   ```python
   propertyManager = PropertyManager.get_instance()
   properties = propertyManager.get_properties('example.properties')
   assert "Y" == properties.getProperty("my.secret")
   ```

## Releasing to PyPI

Releasing Krausening Python integrates into the project's larger utilization of the `maven-release-plugin`, specifically publishing the package to PyPI during the `deploy` phase.  A [PyPI account](https://pypi.org/account/register/) with access to the [krausening](https://pypi.org/project/krausening/) project is required. PyPI account credentials should be specified in your `settings.xml` under the `<id>pypi</id>` `<server>` entry:

```xml
<settings>
  <servers>
    <server>
      <id>pypi</id>
      <username>pypi-username</username>
      <password>pypi-password</password>
    </server>
  </servers>
</settings>
```
