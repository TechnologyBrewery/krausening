# Contributing to Krausening

## Purpose
This document serves to provide necessary steps and information to contribute to the Krausening codebase. Documentation on the purpose and functionality of relevant source files is also covered to serve as priming context, prior to making contributions.

## Releasing to Maven Central Repository

Krausening uses both the `maven-release-plugin` and the `nexus-staging-maven-plugin` to facilitate the release and deployment of new Krausening builds. In order to perform a release, you must:

1. Obtain a [JIRA](https://issues.sonatype.org/secure/Dashboard.jspa) account with Sonatype OSSRH and access to the `org.bitbucket.askllc` project group

2. Ensure that your Sonatype OSSRH JIRA account credentials are specified in your `settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>ossrh-jira-id</username>
      <password>{encrypted-ossrh-jira-pwd}</password>
    </server>
  </servers>
</settings>
```

3. Krausening Python requires a [PyPI account](https://pypi.org/account/register/) with access to the [krausening](https://pypi.org/project/krausening/) project and integrates into the `maven-release-plugin`'s `deploy` phase to appropriately publish the package to PyPI. PyPI account credentials should be specified in your `settings.xml` under the `<id>pypi</id>` `<server>` entry:

```xml
<settings>
  <servers>
    <server>
      <id>pypi</id>
      <username>pypi-username</username>
      <password>{encrypted-pypi-password}</password>
    </server>
  </servers>
</settings>
```


4. Install `gpg` and distribute your key pair - see [here](https://central.sonatype.org/publish/requirements/gpg/).  OS X users may need to execute:

```shell
export GPG_TTY=`tty`;
```

5. Execute `mvn release:clean release:prepare`, answer the prompts for the versions and tags, and perform `mvn release:perform`

## Krausening Architecture (Java)

It is worth first noting that many of the classes and interfaces defined in the source code rely on the [org.aeonbits.owner package](http://owner.aeonbits.org/docs/usage/). Below is a summary of the Krausening source code's files.

```
org
├── aeonbit/owner
│   └── ExtendedKrauseningSource.java
│   └── KrauseningAwarePropertiesManager.java
│   └── KrauseningConfig.java
│   └── KrauseningConfigFactory.java
│   └── KrauseningFactory.java
|  
├── bitbucket/krausening
|   └── Krausening.java
|   └── KrauseningException.java
|   └── KrauseningWarSpecificBootstrapContextListener.java
```

#### org.aeonbit.owner
* ```ExtendedKrauseningSource.java```
    * Provides a consistent mechanism to extend a KrauseningSources annotated config class by overwriting the properties file name to something new at runtime.
    * The primary use case for this functionality is when you want to build some generic code and need to have multiple different property sets for the same concept deployed at the same time.
* ```KrauseningAwarePropertiesManager.java```
    * KrauseningAwarePropertiesManager replaces the default URL-based property file specification strategy that is implemented in PropertiesManager and delegates to Krausening for loading property files.
    * All of the features present in OWNER, such as property variable expansion, default property values, hot reloading property files, etc. are still supported.
    * In addition, developers may still use the Config.Sources annotation in conjunction with the KrauseningConfig.KrauseningSources annotation on the same interface in order to load *.properties with Krausening and *.xml properties using OWNER.
* ```KrauseningConfig.java```
    * KrauseningConfig serves as the interface to extend in order to define a property mapping interface.
    * Annotations that are used to specify the desired Krausening property files to load and merge policy are also encapsulated within this interface.
* ```KrauseningConfigFactory.java```
    * KrauseningConfigFactory is largely modeled after ConfigFactory and provides a simple, straightforward adapter for creating KrauseningConfig proxies and largely delegates to an underlying KrauseningFactory implementation.
    * This class is the intended and expected entry point for creating KrauseningConfig proxies.
* ```KrauseningFactory.java```
    * KrauseningFactory extends DefaultFactory in order to delegate to KrauseningAwarePropertiesManager for property mapper proxy generation.

#### org.bitbucket.krausening
* ```Krausening.java```
    * Krausening serves as the entry singleton, which reads the specified properties files (through Java System Properties)
    * The first base properties will be loaded, with anything in the extensions location being added on to the top. This allows value to be added or overridden, which is especially useful when you have a standard configuration defined in your base files, but need to specialize some values for different deployments.
    * Only .properties files will be loaded. Any other file encountered will be skipped.
* ```KrauseningException.java```
    * KrauseningException extends RuntimeException
    * Krausening library's custom exception
* ```KrauseningWarSpecificBootstrapContextListener.java```
    * KrauseningWarSpecificBootstrapContextListener serves to initilize and destroy ServletContextListeners when OVERRIDE_EXTENSIONS_SUBFOLDER_PARM is defined
