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