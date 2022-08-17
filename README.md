# Krausening - Externalized Property Management and Access #
[![Maven Central](https://img.shields.io/maven-central/v/org.bitbucket.askllc.krausening/krausening.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.bitbucket.askllc.krausening%22%20AND%20a%3A%22krausening%22)
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)

In brewing, krausening (KROI-zen-ing) refers to adding a small amount of existing beer to fresh wort to prime the beer for carbonation.  In Java, Krausening is a project to populate finished archives for deployment.  This approach allows Properties files to be externalized from deployment units, enabling the same deployment unit to be leveraged repeatedly without the need to rebuild or hack the archive.

# Krausening in One Pint (Learn Krausening in 2 Minutes)#
Krausening is very simple.  Follow these steps to prime your project:

1. Add a Java System Property called KRAUSENING_BASE pointing to the folder with your .properties files
```properties
KRAUSENING_BASE=./src/test/resources/base
```
2. In your source code, get a handle to the Krausening singleton, then request the property file you'd like to access:
```java
Krausening krausening = Krausening.getInstance();
Properties properties = krausening.getProperties("example.properties");

```
3. You're done - order your next pint!

# Krausening in Two Pints (Leveraging Property Extension)#
Often, some properties need to change as your deployment unit travels between environments.  We want to do this without having to copy and paste all the properties, lowering our maintenance burden to just those properties that have changed.  To accomplish this, build on the prior example by:

1. Add a Java System Property called KRAUSENING_EXTENSIONS pointing to the folder with your extension .properties files
```properties
KRAUSENING_EXTENSIONS=./src/test/resources/prod-env
```
2. Create a properties file of the same name as the one in base, only added the properties you want to extend:

```properties
# in $KRAUSENING_BASE/example.properties:
propertyA=org.bitbucket.some.reflect.Class
propertyB=https://localhost/

# in $KRAUSENING_EXTENSIONS/example.properties:
propertyB=https://prodUrl/
```
3. When you look for your properties, you'll now get a collapsed version, containing propertyA from the base version, and propertyB from the extensions version:
```java
Krausening krausening = Krausening.getInstance();
Properties properties = krausening.getProperties("example.properties");
assertEquals(properties.get("propertyA"), "org.bitbucket.some.reflect.Class");
assertEquals(properties.get("propertyB"), "https://prodUrl/");
```
4. You're done - try a mystery beer with Krausening's encryption integration to further quench your thirst.

# Krausening in Three Pints (Leveraging context specific properties)#
Sometimes different contexts/applications/classloads/wars want to have their own properties even when deployed in the same environments. 
For example, foo and bar are deployed together with the same krausening base and extensions set, but _foo wants to have my.property=X and bar wants to have my.property=Y_.
In this case you can leverage override extensions to apply different properties per context.

1. Add a Java System Property called KRAUSENING_OVERRIDE_EXTENSIONS pointing to the folder with your override extension .properties files
```properties
KRAUSENING_OVERRIDE_EXTENSIONS=./src/test/resources/prod-env-overrides
```

2. Create subfolders for the different contexts you need to override extensions

```properties
# in $KRAUSENING_OVERRIDE_EXTENSIONS/foo/example.properties:
my.property=X

# in $KRAUSENING_OVERRIDE_EXTENSIONS/bar/example.properties:
my.property=Y
```

3. A) Update the web.xml files for each context to point to a different subfolder within the override extensions location

`web.xml` for foo
```xml
    <listener>
        <listener-class>org.bitbucket.krausening.KrauseningWarSpecificBootstrapContextListener</listener-class>
    </listener>
    <context-param>
        <param-name>override.extensions.subfolder</param-name>
        <param-value>foo</param-value>
    </context-param>
```

`web.xml` for bar (**NOTE the difference in the subfolder parameter**)
```xml
    <listener>
        <listener-class>org.bitbucket.krausening.KrauseningWarSpecificBootstrapContextListener</listener-class>
    </listener>
    <context-param>
        <param-name>override.extensions.subfolder</param-name>
        <param-value>bar</param-value>
    </context-param>
```
```java
Krausening krausening = Krausening.getInstance("foo");
Properties properties = krausening.getProperties("example.properties");
assertEquals(properties.get("my.property"), "X");
```


3. B) Alternatively, you can use an override subfolder when getting the krausening instance.

```java
Krausening krausening = Krausening.getInstance("foo");
Properties properties = krausening.getProperties("example.properties");
assertEquals(properties.get("my.property"), "X");
```

# Krausening in Four Pints (Leveraging Jasypt for Encrypting/Decrypting Properties)#
Frequently, it is useful to store encrypted information within properties files.  Krausening optionally leverages Jasypt to allow stored properties to be encrypted at rest while also decrypting property values as they are read without manual interaction.

1.  Add a Java System Property called KRAUSENING_PASSWORD pointing to your Jasypt master encryption password.
```properties
KRAUSENING_PASSWORD=myMasterPassword
```

2.  [Use Jasypt to encrypt your property information](http://www.jasypt.org/cli.html), then add the [encrypted value in your properties file via the Jasypt format](http://www.jasypt.org/encrypting-configuration.html):
```properties
password=ENC(2/O4g9ktLtJLWYyP8RlEhBfhVxuSL0fnBlOVmXI+qRw=)
```

3. When you look for your property, you'll now get the decrypted value:
```java
Krausening krausening = Krausening.getInstance();
krausening.loadProperties();
Properties properties = krausening.getProperties("encrypted.properties");
assertEquals(properties.get("password"), "someStrongPassword");
```

4. You're done - go for the whole sampler with Krausening's Owner integration if you're still thirsty.

# Krausening in Five Pints (Leveraging Owner Integration to Access Properties via Interfaces (and more))#
While accessing properties via `java.util.Properties` as `String` values works, wouldn't it be great if we could get compile-safe, strongly typed references to our Krausening property values that reflect their actual type? By integrating tightly with [Owner](http://owner.aeonbits.org/), Krausening can offer annotation-based type conversion, variable expansion, hot reloading, XML property file support, and all of the other great features that are built into Owner. Assuming that we have the following properties files created: 
```properties
# in $KRAUSENING_BASE/example.properties:
fibonacci=1, 1, 2, 3, 5, 8
url=https://localhost
serviceSubPath=foo/baz

# in $KRAUSENING_EXTENSIONS/example.properties:
url=https://prodUrl
fullServiceUrl=${url}/${serviceSubPath}/endpoint
pi=3.1415
```

1. Create an interface that describes and maps to the contents of the collapsed version of `example.properties`.  Code that relies on these property values will be able to directly use this interface, instead of interacting with a `java.util.Properties` object. The interface must contain a `@KrauseningSources` definition, along with any supported Owner annotation:
```java
@KrauseningSources("example.properties")
public interface ExampleConfig extends KrauseningConfig {
  @Key("fibonacci")
  List<Integer> getFibonacciSeq();

  @Key("fullUrl")
  URL getFullUrl();

  @Key("pi")
  double getPi();

  @Key("not-defined-in-prop-file")
  @DefaultValue("1234")
  int getInt();
}
```

2. Access properties via the newly created interface:
```java
ExampleConfig config = KrauseningConfigFactory.create(ExampleConfig.class);
assertEquals(3, config.getFibonacciSeq().get(3));
assertEquals(new URL("https://prodUrl/foo/baz/endpoint"), config.getFullUrl());
assertEquals(3.1415d, config.getPi());
assertEquals(1234, config.getInt());
```
3. Optionally, get a list of all the properties in the newly created interface by calling the configuration fill() method:
```java
ExampleConfig config = KrauseningConfigFactory.create(ExampleConfig.class);
Properties properties = new Properties();
config.fill(properties);
assertTrue(properties.keySet().contains("pi"));
assertEquals("3.1415",properties.getProperty("pi"));
```
4. Check out `KrauseningConfigTest` in `src/test/java` and/or the Owner documentation for additional information on how to best utilize the Krausening-Owner integration.

# Last Call

You're now 5 pints in and ready for how ever many more property files you need without having to worry about stumbling through deployment!

# Krausening and Python

See the [krausening-python README](https://bitbucket.org/cpointe/krausening/src/dev/krausening-python/) for more details.

# Distribution Channel

Want Krausening in your project? The following Maven dependency will add the Java implementation of Krausening to your Maven project from the Maven Central Repository:

```xml
<dependency>
    <groupId>org.bitbucket.askllc.krausening</groupId>
    <artifactId>krausening</artifactId>
    <version>11</version>
</dependency>
```

## Releasing to Maven Central Repository

Krausening uses both the `maven-release-plugin` and the `nexus-staging-maven-plugin` to facilitate the release and deployment of new Krausening builds. In order to perform a release, you must:

1. Obtain a [JIRA](https://issues.sonatype.org/secure/Dashboard.jspa) account with Sonatype OSSRH and access to the `org.bitbucket.askllc` project group

2. Ensure that your Sonatype OSSRH JIRA account credentials are specified in your `settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>your-jira-id</username>
      <password>your-jira-pwd</password>
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
      <password>pypi-password</password>
    </server>
  </servers>
</settings>
```


4. Install `gpg` and distribute your key pair - see [here](http://central.sonatype.org/pages/working-with-pgp-signatures.html).  OS X users may need to execute:

```shell
export GPG_TTY=`tty`;
```

5. Execute `mvn release:clean release:prepare`, answer the prompts for the versions and tags, and perform `mvn release:perform`

## Licensing
Krausening is available under the [MIT License](http://opensource.org/licenses/mit-license.php).

## Session Beer
Krausening would like to thank [Counterpointe Solutions](http://cpointe-inc.com/) for providing continuous integration and static code analysis services for Krausening.
