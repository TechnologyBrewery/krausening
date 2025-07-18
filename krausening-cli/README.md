# Krausening Command-Line Interface #
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)
[![PyPI](https://img.shields.io/pypi/v/krausening?logo=python&logoColor=gold)](https://pypi.org/project/krausening-cli/)
![PyPI - Python Version](https://img.shields.io/pypi/pyversions/krausening-cli?logo=python&logoColor=gold)
![PyPI - Wheel](https://img.shields.io/pypi/wheel/krausening-cli?logo=python&logoColor=gold)
[![PyPI - Downloads](https://img.shields.io/pypi/dm/krausening-cli?color=blue&label=Installs&logo=pypi&logoColor=gold)](https://pypi.org/project/krausening-cli/)

The Krausening CLI is a standalone tool to help encrypt properties and values in a manner that
[Krausening](https://github.com/TechnologyBrewery/krausening/) can decrypt.

## Installation

We recommend using [pipx](https://pipx.pypa.io/stable/installation/) to install the Krausening CLI. However, it can
also be installed using any tool that can install a package from PyPI.

```shell
pipx install krausening-cli
```

## Commands

### Encrypt Files

Reads a set of properties files from a directory and applies encryption to any properties prefixed with `**`.

```shell
KRAUSENING_PASSWORD=changeme krausening encrypt files ./configs/base-configs
```

#### Input Properties:
```properties input
aws.access.key=ABC123
**aws.secret.key=MYSECRETKEY
```

#### Output Properties
```properties output
aws.access.key=ABC123
aws.secret.key=ENC(NR02luiQpKIMY2O1miKx/Gv9gwWsOUyNZ87OGemjtuxSIlJMJEkaB2Ue85VqItUa)
```
