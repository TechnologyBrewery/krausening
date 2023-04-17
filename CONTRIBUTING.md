# Contributing to Krausening

## Purpose
This document serves to provide necessary steps and information to contribute to the Krausening codebase. Documentation on the purpose and functionality of relevant source files is also covered to serve as priming context, prior to making contributions.

## Releasing to Maven Central Repository

Krausening uses both the `maven-release-plugin` and the `nexus-staging-maven-plugin` to facilitate the release and deployment of new Krausening builds. In order to perform a release, you must:

1. Obtain a [JIRA](https://issues.sonatype.org/secure/Dashboard.jspa) account with Sonatype OSSRH and access to the `org.technologybrewery.askllc` project group

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