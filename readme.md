How to develop
==

Eclipse: Import pom.xml as maven project to Eclipse
IntelliJ: Open pom.xml as project

How to deploy
==

Open pom.xml and enter the correct location of your ImageJ/Fiji installation in line 92:

```xml
<imagej.app.directory>C:/programs/fiji-win64/Fiji.app/</imagej.app.directory>
```

Afterwards, you can install the plugins in this project to your ImageJ/Fiji installation by running this command line:

```bash
mvn install
```