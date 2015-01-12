# Krausening - Externalized Property Management and Access #
In brewing, krausening (KROI-zen-ing) refers to adding a small amount of fresh wort to prime finished beer for carbonation.  In Java, Krausening is a project to populate finished archives for deployment.  This approach allows Properties files to be externalized from deployment units, enabling the same deployment unit to be leveraged repeatedly without the need to rebuild or hack the archive.

# Krausening in One Pint (Learn Krausening in 2 Minutes)#
Krausening is very simple.  Follow these steps to prime your project:

1.)  Add a Java System Property called KRAUSENING_BASE pointing to the folder with your .properties files
```
#!bash
KRAUSENING_BASE=./src/test/resources/base
```
2.) In your source code, get a handle to the Krausening singleton, then request the property file you'd like to access:
```
#!java
Krausening krausening = Krausening.getInstance();
krausening.loadProperties();
Properties properties = krausening.getProperties("example.properties");

```
3.)  You're done - order your next pint!

# Krausening in Two Pints (Leveraging Property Extension)#
Often, some properties need to change as your deployment unit travels between environments.  We want to do this without having to copy and paste all the properties, lowering our maintenance burden to just those properties that have changed.  To due this, build on the prior example by:

1.)  Add a Java System Property called KRAUSENING_EXTENSION pointing to the folder with your extension .properties files
```
#!bash
KRAUSENING_BASE=./src/test/resources/prod-env
```
2.) Create a properties file of the same name as the one in base, only added the properties you want to extend:

```
#!bash
# in $KRAUSENING_BASE/example.properties:
propertyA=org.bitbucket.some.reflect.Class
propertyB=https://localhost/

# in $KRAUSENING_EXTENSIONS/example.properties:
propertyB=https://prodUrl/

```
3.) When you look for your properties, you'll now get a collapsed version, containing propertyA from the base version, and propertyB from the extensions version:
```
#!java
Krausening krausening = Krausening.getInstance();
krausening.loadProperties();
Properties properties = krausening.getProperties("example.properties");
assertEquals(properties.get("propertyA"), "org.bitbucket.some.reflect.Class");
assertEquals(properties.get("propertyB"), "https://prodUrl/");
```
4.) You're done - go for the whole sampler with Krausening's Owner integration if you're still thirsty.

# Krausening in Three Pints (Leveraging Owner Integration to Access Properties via Interfaces (and more))#
To be completed